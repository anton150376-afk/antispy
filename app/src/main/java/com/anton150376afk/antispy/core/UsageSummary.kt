package com.anton150376afk.antispy.core

/** Данные UsageStats для одного пакета */
data class UsageSummary(
    val usageCount: Int,
    val foregroundMs: Long  // мс активности в foreground за период
)
