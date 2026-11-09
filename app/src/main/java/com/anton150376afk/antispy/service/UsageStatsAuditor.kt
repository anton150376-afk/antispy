package com.anton150376afk.antispy.service

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

/**
 * Wrapper для UsageStatsManager — аудит обращений пакетов к сенсору за период.
 * Возвращает информацию о том какие приложения работали в foreground, 
 * сколько раз каждый был запущен и сколько всего времени провёл в активном состоянии.
 */
class UsageStatsAuditor(private val context: Context) {

    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)!!

    /** 
     * Выполняет аудит UsageStats за период [startMs .. now].
     * Возвращает список пакетов отсортированных по времени в foreground.
     */
    fun auditLastHours(hours: Long): List<UsageAuditResult> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()

        val now = System.currentTimeMillis()
        val start = now - hours * 3600_000L

        // aggregate по пакетам — даёт totalTimeInForegroundMs и eventCount.
        // Это самый эффективный метод для аудита (не нужно перебирать все пакеты вручную)
        try {
            val aggStats = usageStatsManager.queryAndAggregateUsageStats(start, now)
            
            return aggStats.entries
                .filterNot { 
                    // Исключим системные процессы которые не представляют угрозы
                    it.key.startsWith("android.") || 
                    it.key.startsWith("com.google.android.") ||
                    it.key.startsWith("com.android.systemui")
                }
                .mapNotNull { (_, stats) ->
                    val pkgName = stats.packageName
                    if (pkgName.isNullOrEmpty()) return@mapNotNull null
                    
                    // Если приложение работало менее 1 секунды — не считаем
                    val totalMs = stats.totalTimeInForegroundMs ?: 0L
                    if (totalMs < 1_000) return@mapNotNull null
                    
                    PackageAudit(
                        packageName = pkgName,
                        totalForegroundMs = totalMs,
                        lastLaunchMillis = stats.lastTimeActive ?: now - totalMs,
                        launchCount = stats.eventCount ?: 0
                    )
                }
                .sortedByDescending { it.totalForegroundMs }
                .map(::UsageAuditResult)
        } catch (e: SecurityException) {
            // Обычно: пользователь не дал PACKAGE_USAGE_STATS permissions.
            // Нужно перенаправить его в Settings → Security → Usage Access > AntiSpy
            return emptyList()
        }
    }

    /** 
     * Запрашивает детальную информацию по отдельному пакету за период.
     * Возвращает список UsageEvents для анализа активности приложения по часам.
     */
    fun getAppUsageEvents(packageName: String, startMs: Long, endMs: Long): List<UsageStatEvent> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
        
        try {
            // queryEvents возвращает список UsageEvents — каждый event это одно включение/выключение приложения
            val events = usageStatsManager.queryEvents(startMs, endMs)
            
            // Фильтруем только события для нужного пакета
            val pkgEvents = mutableListOf<UsageStatEvent>()
            while (events.hasNextEvent()) {
                val event = android.app.usage.UsageEvent()
                events.getNextEvent(event)
                
                if (event.packageName == packageName) {
                    pkgEvents.add(
                        UsageStatEvent(
                            eventType = mapEventType(event.eventType),
                            timestamp = event.eventTime,
                            durationMs = event.duration
                        )
                    )
                }
            }
            events.close()
            return pkgEvents
        } catch (e: SecurityException) {
            return emptyList()
        }
    }

    /** Маппинг типа события UsageStats в человеко-читаемые строки */
    private fun mapEventType(type: Int): String {
        return when (type) {
            android.app.usage.UsageEvent.Event.TYPE_APPLICATION_RESUMED -> "Запущено"
            android.app.usage.UsageEvent.Event.TYPE_APPLICATION_OPENED -> "Открыто"
            android.app.usage.UsageEvent.Event.TYPE_APPLICATION_CLOSED -> "Закрыто"
            android.app.usage.UsageEvent.Event.TYPE_APPLICATION_BACKGROUNDED -> "Ушло в фон"
            android.app.usage.UsageEvent.Event.TYPE_APPLICATION_PRESTOPPED -> "Остановлено"
            else -> "Событие $type"
        }
    }
}
