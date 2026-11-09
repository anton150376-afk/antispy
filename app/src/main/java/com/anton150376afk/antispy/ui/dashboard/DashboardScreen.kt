package com.anton150376afk.antispy.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import com.anton150376afk.antispy.core.RiskLevel
import com.anton150376afk.antispy.R

/**
 * Главный экран дашборда AntiSpy.
 * Показывает активные прослушиватели микрофона и аудит за 24ч.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSettingsClick: () -> Unit = {},
    onNlsSettingsClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState = viewModel.state.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.scanAll() // Авто-сканер при входе на дашборд
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AntiSpy", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Карточка сканирования
            if (uiState.isScanning) {
                ScanningCard()
            }

            // Ошибка сканирования
            uiState.errorMessage?.let { errorMsg ->
                ErrorCard(errorMsg)
            }

            // Секция 1: кто слушает микрофон прямо сейчас
            if (uiState.activeListeners.isNotEmpty()) {
                ActiveListenersCard(viewModel = viewModel, listeners = uiState.activeListeners)
            }

            // Секция 2: аудит UsageStats за последние 24ч
            if (uiState.auditResults.isNotEmpty()) {
                AuditRecordCard(viewModel = viewModel, auditResults = uiState.auditResults)
            }

            // Карточка запроса разрешения NLS
            if (!uiState.isNlsGranted) {
                NlsPermissionCard(onOpenSettings = onNlsSettingsClick)
            }

            // Секция 3: события из NotificationListenerService (только когда NLS уже разрешён)
            if (uiState.isNlsGranted && uiState.nlsEvents.isNotEmpty()) {
                NlsEventsCard(events = uiState.nlsEvents)
            }

            // Пустое состояние — только если всё проверено и данных нет
            if (!uiState.isScanning && !uiState.isNlsGranted && uiState.activeListeners.isEmpty() && uiState.errorMessage == null) {
                EmptyStateCard(
                    onScanAgain = viewModel::scanAll
                )
            }

            // Кнопка перехода к настройкам
            SettingsCard(onClick = onSettingsClick)
        }
    }
}

// ============================================================
// Под-композиблы экрана
// ============================================================

@Composable
private fun ScanningCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.size(16.dp))
            Text(stringResource(R.string.dashboard_scanning_active))
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECB3))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.dashboard_warning_title), style = MaterialTheme.typography.titleMedium, color = Color(0xFFE65100))
            Spacer(Modifier.size(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ActiveListenersCard(
    viewModel: DashboardViewModel,
    listeners: List<AppScanResult>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Заголовок секции
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Mic, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.dashboard_active_listeners_title), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.size(8.dp))

            listeners.forEachIndexed { index, listener ->
                AppListItem(
                    name = listener.appName,
                    packageName = listener.packageName,
                    iconColor = riskColor(listener.riskLevel),
                    subtitle = viewModel.formatTimeSinceLastAccess(
                        System.currentTimeMillis() - listener.lastAccessMs
                    ) + ", сессий: ${listener.sessionCount}",
                    isLastItem = index == listeners.lastIndex
                )
            }
        }
    }
}

@Composable
private fun AuditRecordCard(
    viewModel: DashboardViewModel,
    auditResults: List<AuditEntry>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Заголовок секции
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.dashboard_audit_24h_title), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.size(8.dp))

            auditResults.forEachIndexed { index, audit ->
                val foregroundMins = viewModel.formatTimeSinceLastAccess(audit.totalForegroundMs)
                AppListItem(
                    name = audit.appName,
                    packageName = audit.packageName,
                    iconColor = auditRiskColor(audit),
                    subtitle = "Время в фоне: $foregroundMins, запуска: ${audit.launchCount}",
                    isLastItem = index == auditResults.lastIndex
                )
            }
        }
    }
}

// ============================================================
// Секция событий из NotificationListenerService
// ============================================================

@Composable
private fun NlsEventsCard(events: List<com.anton150376afk.antispy.service.AppNlsListener.RecordingNotification>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Заголовок секции
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Notifications, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.nls_events_24h_title), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.size(8.dp))

            events.forEachIndexed { index, event ->
                val timeStr = formatNlsTime(event.timestamp)
                AppListItem(
                    name = event.appName.ifEmpty { stringResource(R.string.nls_event_unknown_package) },
                    packageName = event.packageName,
                    iconColor = Color(0xFF2196F3),
                    subtitle = "$timeStr",
                    isLastItem = index == events.lastIndex
                )
            }
        }
    }
}

/** Форматирование времени для NLS событий */
private fun formatNlsTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "только что"
        diff < 3_600_000L -> "${diff / 60_000L} мин назад"
        else -> "${diff / 3_600_000L} ч назад"
    }
}

// ============================================================
// Запрос разрешения NLS
// ============================================================

@Composable
private fun NlsPermissionCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = Color(0xFFFFA000),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.nls_card_description),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.size(8.dp))
            androidx.compose.material3.TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.nls_card_open_settings), color = Color(0xFF2196F3))
            }
        }
    }
}

// ============================================================
// Пустое состояние
// ============================================================

@Composable
private fun EmptyStateCard(onScanAgain: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.size(16.dp))
            Text(stringResource(R.string.dashboard_empty_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(8.dp))
            Text("Активных слушателей не обнаружено", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
private fun SettingsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_link_text))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Перейти к настройкам", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Одна строка списка приложения */
@Composable
private fun AppListItem(
    name: String,
    packageName: String,
    iconColor: Color,
    subtitle: String,
    isLastItem: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кружок-индикатор риска
            Box(
                modifier = Modifier.size(12.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.3f))
            )
            Spacer(Modifier.size(12.dp))

            Column {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(packageName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(Modifier.weight(1f))

            // Подпись справа
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = iconColor)
        }

        if (!isLastItem) {
            Spacer(Modifier.size(8.dp))
            HorizontalDivider()
        }
    }
}

// ============================================================
// Утилиты цветов по уровню риска
// ============================================================

private fun riskColor(level: RiskLevel): Color {
    return when (level) {
        RiskLevel.HIGH,
        RiskLevel.CRITICAL -> Color(0xFFF44336)
        RiskLevel.MEDIUM -> Color(0xFFFFA000)
        else -> Color(0xFF4CAF50)
    }
}

private fun auditRiskColor(audit: AuditEntry): Color {
    val totalMin = audit.totalForegroundMs / 60_000L
    return when {
        totalMin > 60 -> Color(0xFFF44336)   // > 1 часа
        totalMin > 10 -> Color(0xFFFFA000)    // > 10 мин
        else -> Color(0xFF4CAF50)              // < 10 мин
    }
}
