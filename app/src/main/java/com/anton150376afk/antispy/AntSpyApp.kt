package com.anton150376afk.antispy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Точка входа приложения AntiSpy.
 * Инициализирует Hilt для Dependency Injection.
 */
@HiltAndroidApp
class AntSpyApp : Application()
