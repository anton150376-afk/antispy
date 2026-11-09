package com.anton150376afk.antispy.service

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Общий лог событий между AppNlsListener и DashboardViewModel.
 * Работает в одном процессе: NLS service + app код живут вместе.
 */
object NlsEventLog {

    private val _events = CopyOnWriteArrayList<AppNlsListener.RecordingNotification>()

    /** Добавляет событие от NLS */
    fun addEvent(event: AppNlsListener.RecordingNotification) {
        _events.add(event)
        // Уменьшаем до последних 100 событий (за ~5 мин)
        if (_events.size > 100) {
            _events.removeAll { it.timestamp < System.currentTimeMillis() - 5 * 60_000L }
        }
    }

    /** Получает последние события (за все время работы сервиса) */
    fun getRecentEvents(): List<AppNlsListener.RecordingNotification> {
        return _events.toList()
    }

    /** Активен ли мониторинг уведомлений в системе */
    val isEnabled: Boolean
        get() = _isActive.get()

    /** Установить флаг активности (вызывается через bindService или check) */
    fun setEnabled(active: Boolean) {
        _isActive.set(active)
    }

    private val _isActive = AtomicBoolean(false)
}
