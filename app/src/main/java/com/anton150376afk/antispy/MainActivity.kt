package com.anton150376afk.antispy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.anton150376afk.antispy.service.MicMonitoringService
import com.anton150376afk.antispy.ui.dashboard.DashboardScreen
import com.anton150376afk.antispy.ui.dashboard.DashboardViewModel

/**
 * Главная активность приложения AntiSpy.
 * Запускает сканирование при старте и показывает Dashboard.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Запрашиваем POST_NOTIFICATIONS (разрешение на показ уведомлений) для Android 13+
        val postNotifLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) startMicMonitoring()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startMicMonitoring()
            } else {
                postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startMicMonitoring()
        }

        setContent {
            AntiSpyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AntiSpyAppRoot()
                }
            }
        }
    }

    /** Запускает ForegroundService мониторинга микрофона один раз */
    private fun startMicMonitoring() {
        // Проверка что сервис ещё не работает по наличию активного notification
        if (!isMyServiceRunning()) {
            MicMonitoringService.startMonitoring(this)
        }
    }

    /** Проверка запущен ли наш foreground-сервис (по наличию уведомления) */
    @Suppress("DEPRECATION")
    private fun isMyServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (MicMonitoringService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

/** Корневой Composable-граф приложения */
@Composable
private fun AntiSpyAppRoot(viewModel: DashboardViewModel = hiltViewModel()) {
    AntiSpyAppScreen(viewModel = viewModel)
}

/** Основной экран приложения (один экран, без навигации) */
@Composable
private fun AntiSpyAppScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val context = LocalContext.current

    // Открываем настройки защиты при клике
    val onSettingsClick: () -> Unit = {
        Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }.also { context.startActivity(it) }
    }

    DashboardScreen(onSettingsClick = onSettingsClick, viewModel = viewModel)
}

// ============================================================
// Минимальная тема AntiSpy (Material 3)
// ============================================================

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFFFBFE),
    primaryContainer = Color(0xFFE8DEF8),
)

/** Тема приложения — обёртка над MaterialTheme с палитрой AntiSpy */
@Composable
fun AntiSpyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
