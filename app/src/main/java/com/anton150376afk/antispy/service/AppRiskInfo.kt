package com.anton150376afk.antispy.service

/** Информация о пакете с оценкой риска слушания / камеры */
data class AppRiskInfo(
    val packageName: String,
    val appName: String,
    val score: Long, // 0-4 = risk level (SAFE, LOW, MEDIUM, HIGH, CRITICAL)
    val lastActiveMs: Long
) {
    /** Время с последнего обращения к сенсору в читаемом виде */
    fun getTimeSinceLastAccess(): String {
        if (lastActiveMs <= 0L) return "прямо сейчас"

        val diffMs = System.currentTimeMillis() - lastActiveMs
        return when {
            diffMs < 1_000L -> "прямо сейчас"
            diffMs < 60_000L -> "${diffMs / 1_000L} сек назад"
            diffMs < 3_600_000L -> "${diffMs / 60_000L} мин назад"
            else -> "${diffMs / 3_600_000L} ч назад"
        }
    }

    /** Текстовая метка уровня риска */
    fun getRiskLabel(): String {
        return when (score.toInt()) {
            4 -> "Критическая угроза" // CRITICAL
            3 -> "Высокий риск"       // HIGH
            2 -> "Средний риск"       // MEDIUM
            1 -> "Низкий риск"        // LOW
            else -> "Безопасно"        // SAFE
        }
    }
}
