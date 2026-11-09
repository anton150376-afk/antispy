package com.anton150376afk.antispy.service

/** Событие UsageStats для одного события */
data class UsageStatEvent(
    val eventType: String,
    val timestamp: Long,
    val durationMs: Long  // мс в этом состоянии. Если >0 — значит было запущено на это время
)
