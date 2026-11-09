package com.anton150376afk.antispy.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anton150376afk.antispy.MainActivity
import com.anton150376afk.antispy.R
import com.anton150376afk.antispy.core.MicScanEngine

/**
 * ForegroundService для непрерывного мониторинга микрофона.
 * Работает постоянно в фоне, показывает статус-бар уведомление "AntiSpy - защита активна".
 * Каждые 3 секунды сканирует кто использует mic и обновляет уведомления.
 */
class MicMonitoringService : Service() {

    private val scanEngine = MicScanEngine(this)

    // Последние обнаруженные слушатели
    private var lastScanResults = listOf<AppRiskInfo>()
        private set

    // Интервал сканирования в мс (3 секунды)
    private var scanIntervalMs: Long = 3000L

    // Handler для периодических сканов
    private val handler = Handler(Looper.getMainLooper())
    private val scanRunnable = object : Runnable {
        override fun run() {
            performScan()
            handler.postDelayed(this, scanIntervalMs)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Создаём notification channel (Android 8+ required)
        createNotificationChannel(this)

        // Запускаем foreground service с постоянным уведомлением
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Запустим периодический сканер
        handler.post(scanRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(scanRunnable)
    }

    private fun buildForegroundNotification(): Notification {
        val startIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.antispy_notification_title))
            .setContentText(getString(R.string.antispy_notification_content_idle))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Невозможно swipe-отменить
            .build()
    }

    /** Выполняет один скан и обновляет уведомление */
    private fun performScan() {
        val results = scanEngine.scanActiveListeners().map { info ->
            // durationMillis == 0 означает "активен прямо сейчас" - ставим текущее время, иначе getTimeSinceLastAccess вернёт корректное значение
            val lastActiveMs = if (info.durationMillis > 0) {
                System.currentTimeMillis() - info.durationMillis
            } else {
                System.currentTimeMillis() // Активен прямо сейчас
            }

            AppRiskInfo(
                packageName = info.packageName,
                appName = info.appName,
                score = info.riskLevel.ordinal.toLong(),
                lastActiveMs = lastActiveMs
            )
        }

        lastScanResults = results.distinctBy { it.packageName }
        updateNotification(lastScanResults)
    }

    /** Обновляет foreground notification с результатами сканирования */
    private fun updateNotification(results: List<AppRiskInfo>) {
        val highRiskCount = results.count { it.score >= 3L }

        val summaryText = if (highRiskCount > 0) {
            getString(R.string.antispy_notification_content_threats, highRiskCount)
        } else {
            getString(R.string.antispy_notification_content_clean)
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.antispy_notification_title))
            .setContentText(summaryText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /** Создаёт notification channel для ForegroundService (Android 8.0+). */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val name = context.getString(R.string.antispy_notification_channel_name)
        val descriptionText = context.getString(R.string.antispy_notification_channel_desc)
        val importance = NotificationManagerCompat.IMPORTANCE_LOW
        val channel = android.app.NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(false)
        }

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "antispy_mic_monitor"

        /** Запуск мониторинга микрофона из Activity */
        fun startMonitoring(context: Context) {
            context.startService(Intent(context, MicMonitoringService::class.java))
        }
    }
}
