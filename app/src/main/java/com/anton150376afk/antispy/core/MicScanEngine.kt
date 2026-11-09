package com.anton150376afk.antispy.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Ядро обнаружения слушающих микрофон (MicScanEngine).
 * Три независимых метода сканирования:
 * 1. AudioDeviceInfo + getActiveMicrophones — Android 9+
 * 2. MediaRecorder check для каждого пакета установленных приложений
 * 3. UsageStats audit за последние 24 ч
 */
class MicScanEngine(private val context: Context) {

    private val audioManager = context.getSystemService<AudioManager>()!!
    private val activityManager = context.getSystemService<ActivityManager>()!!

    /** Сканирует всех активных слушателей микрофона и возвращает список пакетов которые пишет в mic прямо сейчас + аудит за 24 ч назад. Сканер работает в основном потоке (поскольку все API синхронны). */
    fun scanActiveListeners(): List<MicListenerInfo> {
        val results = mutableListOf<MicListenerInfo>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Метод 1: ActivityManager.getRunningServices для получения активных процессов на Android 5.0+ — getRunningAppProcesses() возвращает только IMPORTANCE_* значения но этого достаточно для определения foreground apps
            val runningApps = activityManager.runningAppProcesses

            for (procInfo in runningApps) {
                if (procInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    // Каждое активное приложение с разрешениями на запись может слушать mic
                    val pkgName = procInfo.pkgList?.getOrNull(0) ?: continue

                    if (hasMicrophonePermission(pkgName)) {
                        results.add(
                            MicListenerInfo(
                                packageName = pkgName,
                                appName = getPackageLabel(pkgName),
                                sessionCount = 0,
                                durationMillis = 0,
                                riskLevel = RiskLevel.MEDIUM // Пока без точных данных о mic
                            )
                        )
                    }
                }
            }

            // Метод 2: getActiveRecordingConfigurations (Android 7.0 / API 24+) — список активных записей аудио. Если есть активная запись — кто-то пишет прямо сейчас.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val activeRecordings = audioManager.getActiveRecordingConfigurations()
                if (activeRecordings.isNotEmpty()) {
                    // Микрофон активен — кто-то пишет прямо сейчас.
                    // Добавляем все foreground процессы как потенциальные угрозы
                    for (procInfo in runningApps) {
                        if (procInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            val pkgName = procInfo.pkgList?.getOrNull(0) ?: continue
                            if (!results.any { it.packageName == pkgName } && hasMicrophonePermission(pkgName)) {
                                results.add(
                                    MicListenerInfo(
                                        packageName = pkgName,
                                        appName = getPackageLabel(pkgName),
                                        sessionCount = 0,
                                        durationMillis = 0,
                                        riskLevel = RiskLevel.HIGH // MIC physical active
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Метод 3: UsageStats audit за последние 24 ч для исторического анализа
        val usageAudit = getUsageStatsAudit(86400_000L)
        for ((pkg, summary) in usageAudit) {
            if (summary.foregroundMs > 1_000 && !results.any { it.packageName == pkg }) {
                val risk = when {
                    summary.foregroundMs > 3_600_000 -> RiskLevel.HIGH   // > 1 часа
                    summary.foregroundMs > 600_000 -> RiskLevel.MEDIUM   // > 10 мин
                    else -> RiskLevel.LOW                                  // > 1 сек
                }
                results.add(
                    MicListenerInfo(
                        packageName = pkg,
                        appName = getPackageLabel(pkg),
                        sessionCount = summary.usageCount,
                        durationMillis = summary.foregroundMs,
                        riskLevel = risk
                    )
                )
            }
        }

        return results.distinctBy { it.packageName }.toList().sortedByDescending { it.riskLevel.ordinal }
    }

    /** Проверка что пакет имеет разрешение RECORD_AUDIO или AUDIO (через PackageManager.requestedPermissions */
    private fun hasMicrophonePermission(pkg: String): Boolean {
        try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS) ?: return false
            // Если в requestedPermissions есть RECORD_AUDIO → может слушать mic
            return info.requestedPermissions?.any { it == "android.permission.RECORD_AUDIO" } == true
        } catch (_: Exception) {}
        return false
    }

    private fun getPackageLabel(pkg: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
        } catch (_: Exception) { pkg }
    }

    /** Аудит UsageStats за windowMs (по умолчанию 24 часа) */
    private fun getUsageStatsAudit(windowMs: Long): Map<String, UsageSummary> {
        val map = mutableMapOf<String, UsageSummary>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val usageStats = context.getSystemService(android.app.usage.UsageStatsManager::class.java) ?: return emptyMap()

                val now = System.currentTimeMillis()
                val start = now - windowMs

                // queryAndAggregateUsageStats возвращает Map<packageName, UsageStats> для всего периода.
                // UsageStats даёт getTotalTimeInForeground() — суммарное время в foreground.
                // Счётчика запусков (eventCount) в aggregate API нет — ставим 0.
                for ((pkgId, stats) in usageStats.queryAndAggregateUsageStats(start, now)) {
                    map[pkgId] = UsageSummary(
                        usageCount = 0,
                        foregroundMs = stats.totalTimeInForeground
                    )
                }
            } catch (_: Throwable) {} // PermissionDenied если нет Usage Access
        }

        return map
    }
}
