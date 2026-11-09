package com.anton150376afk.antispy.ui.dashboard

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anton150376afk.antispy.core.MicScanEngine
import com.anton150376afk.antispy.core.RiskLevel
import com.anton150376afk.antispy.service.AppNlsListener
import com.anton150376afk.antispy.service.NlsEventLog
import com.anton150376afk.antispy.service.UsageStatsAuditor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Состояние дашборда */
data class DashboardUiState(
    val activeListeners: List<AppScanResult> = emptyList(),
    val auditResults: List<AuditEntry> = emptyList(),
    val nlsEvents: List<AppNlsListener.RecordingNotification> = emptyList(),
    val isScanning: Boolean = false,
    val errorMessage: String? = null
)

/** Результат сканирования одного приложения */
data class AppScanResult(
    val packageName: String,
    val appName: String,
    val riskLevel: RiskLevel,
    val sessionCount: Int,
    val lastAccessMs: Long
)

/** Результат аудита UsageStats за 24ч */
data class AuditEntry(
    val packageName: String,
    val appName: String,
    val totalForegroundMs: Long,
    val launchCount: Int
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context
) : ViewModel() {

    private val screenState = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = screenState.asStateFlow()

    // Сканер mic (MicScanEngine) — real-time обнаружение слушателей
    private val micScanner = MicScanEngine(ctx)

    // UsageStats auditor — аудит за 24ч (UsageStatsManager wrapper)
    private val usageAuditor = UsageStatsAuditor(ctx)

    /** Проверка: подключён ли NotificationListenerService через NlsEventLog (сервис сам ставит флаг при onListenerConnected/onListenerDisconnected) */
    fun isNlsEnabled(): Boolean = NlsEventLog.isEnabled

    /** Запустить полное сканирование всех источников */
    fun scanAll() {
        screenState.value = screenState.value.copy(isScanning = true, errorMessage = null)

        viewModelScope.launch {
            try {
                // Шаг 1: сканирование активных слушателей микрофона через MicScanEngine
                val micResults = micScanner.scanActiveListeners()

                // Шаг 2: аудит UsageStats за последние 24 ч
                val auditRawList = usageAuditor.auditLastHours(24)

                // Преобразуем AuditResult в AuditEntry с расчётом риска
                val auditEntries = auditRawList.mapNotNull { result ->
                    val pkg = result.packageInfo
                    if (pkg.totalForegroundMs < 1_000) return@mapNotNull null
                    AuditEntry(
                        packageName = pkg.packageName,
                        appName = getApplicationLabel(pkg.packageName),
                        totalForegroundMs = pkg.totalForegroundMs,
                        launchCount = pkg.launchCount
                    )
                }

                // Преобразуем MicListenerInfo в AppScanResult
                val scanResults = micResults.map { info ->
                    // durationMillis — общее время активности за период (24 ч),
                    // а не смещение от текущего момента. Для real-time слушателей lastAccess = сейчас.
                    AppScanResult(
                        packageName = info.packageName,
                        appName = info.appName,
                        riskLevel = info.riskLevel,
                        sessionCount = info.sessionCount,
                        lastAccessMs = System.currentTimeMillis()
                    )
                }

                screenState.value = DashboardUiState(
                    activeListeners = scanResults,
                    auditResults = auditEntries,
                    nlsEvents = NlsEventLog.getRecentEvents(),
                    isScanning = false
                )
            } catch (e: Throwable) {
                screenState.value = DashboardUiState(
                    isScanning = false,
                    errorMessage = "Ошибка сканирования: ${e.message}"
                )
            }
        }
    }

    /** Получить иконку пакета для отображения на дашборде */
    fun getPackageIcon(packageName: String): android.graphics.drawable.Drawable? {
        return try {
            ctx.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    /** Получить имя приложения (человеко-читаемое) по package name */
    fun getApplicationLabel(packageName: String): String {
        return try {
            ctx.packageManager.getApplicationInfo(packageName, 0).loadLabel(ctx.packageManager).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    /** Форматирование времени «5 мин назад» для отображения в дашборде */
    fun formatTimeSinceLastAccess(millisFromLastAccess: Long): String {
        if (millisFromLastAccess < 1000L) return "прямо сейчас"
        val seconds = millisFromLastAccess / 1000L
        if (seconds < 60L) return "$seconds сек назад"
        val minutes = seconds / 60L
        if (minutes < 60L) return "$minutes мин назад"
        return "${minutes / 60L} ч назад"
    }
}
