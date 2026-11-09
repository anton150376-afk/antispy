# AntiSpy ProGuard Rules
# Минимальная обфускация для production сборки

# Hilt — не обfuscated классы с аннотациями dagger.hilt
-keepnames class * extends android.app.Application
-keepnames class * extends android.app.Service
-keepnames class * extends android.content.ContentProvider
-keepnames class * extends android.database.Observable
-keepsclassmembers class ** {
    @dagger.hilt.InstallIn <init>(...);
}

# Room — entity classes must be preserved for reflection
-keep class com.anton150376afk.antispy.core.model.** { *; }

# Data classes (MicListenerInfo, AuditEntry and others) — used in Jetpack Compose
-keepclassmembers class ** {
    @androidx.room.Entity <fields>;
}
-keep class com.anton150376afk.antispy.core.** { *; }

# kotlinx.coroutines — Keep coroutine-related classes
-dontwarn kotlinx.coroutines internals are not obfuscated
