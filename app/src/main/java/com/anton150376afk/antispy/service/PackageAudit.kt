package com.anton150376afk.antispy.service

/** Информация о пакете из UsageStats */
data class PackageAudit(
    val packageName: String,
    val totalForegroundMs: Long,  // мс работы в foreground
    val lastLaunchMillis: Long,   // последнее время запуска
    val launchCount: Int          // количество запусков (event count)
)
