package com.anton150376afk.antispy.service

import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Build

/**
 * NotificationListenerService для перехвата уведомлений, связанных с записью аудио/видео.
 * Работает параллельно с MicMonitoringService:
 *   — MMS сканирует процессы mic (real-time);
 *   — здесь ловим системные уведомления о действиях записи.
 */
class AppNlsListener : NotificationListenerService() {

    companion object {
        private const val EXTRA_TITLE_KEY = "android.title"
        private const val EXTRA_TEXT_KEY = "android.text"
        private const val EXTRA_SUBTITLE_KEY = "android.subtext"
    }

    data class RecordingNotification(
        val packageName: String,
        val appName: String,
        val timestamp: Long,
        val content: String
    )

    // Кэш последних 5 минут — только уведомления о записи / камере
    private val _events = mutableListOf<RecordingNotification>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        NlsEventLog.setEnabled(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NlsEventLog.setEnabled(false)
        // Очищаем кэш — данные больше не актуальны
        _events.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Игнорируем события до Android 5.0 (minSdk = 26)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        val pkgName = sbn.packageName.toString()
        val notification = sbn.notification ?: return

        // Извлекаем заголовок, текст и подзаголовок уведомления через стандартные ключи extras-бандла
        val title = notification.extras?.getString(EXTRA_TITLE_KEY)?.takeIf { it.isNotBlank() }
        val text = notification.extras?.getString(EXTRA_TEXT_KEY)?.takeIf { it.isNotBlank() }
        val subtitle = notification.extras?.getString(EXTRA_SUBTITLE_KEY)?.takeIf { it.isNotBlank() }

        val contentText = listOfNotNull(title, subtitle, text).firstOrNull() ?: ""
        if (contentText.length < 3) return // слишком мало данных — пропускаем

        // Пытаемся получить label приложения (если доступно)
        val appName = try {
            packageManager.getApplicationInfo(pkgName, 0)
                .loadLabel(packageManager)
                .toString()
        } catch (_: Exception) { pkgName }

        _events.add(RecordingNotification(pkgName, appName, sbn.postTime, contentText))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Игнорируем — события удаления не несут критичной информации
    }

    /** Сколько уникальных пакетов активны прямо сейчас */
    fun activePackagesCount(): Int {
        val events = recentEvents
        val now = System.currentTimeMillis()
        return events.filter { now - it.timestamp < 120_000L }
            .map { it.packageName }
            .toSet()
            .size
    }

    /** Последние перехваченные события (чтение) */
    val recentEvents: List<RecordingNotification>
        get() {
            val cutoff = System.currentTimeMillis() - 5 * 60_000L
            _events.removeAll { it.timestamp < cutoff }
            // Также пишем в общий лог для ViewModel
            _events.forEach { NlsEventLog.addEvent(it) }
            return _events.toList()
        }
}
