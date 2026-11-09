package com.anton150376afk.antispy.core

/** Запись о том что приложение пишет в микрофон */
data class MicListenerInfo(
    val packageName: String,
    val appName: String,
    val sessionCount: Int,
    val durationMillis: Long, // мс активности за период; 0 = активен прямо сейчас
    val riskLevel: RiskLevel
)
