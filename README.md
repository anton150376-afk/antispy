# AntiSpy — обнаружение шпионских приложений на Android

**AntiSpy** — андроид-приложение для обнаружения приложений, которые слушают микрофон или следят за камерой в фоновом режиме.

## Возможности

- **Real-time мониторинг**: ForegroundService непрерывно сканирует активные audio-сессии
  и показывает кто пишет в микрофон прямо сейчас.
- **UsageStats аудит**: Анализирует обращение приложений к сенсору за последние 24 часа.
- **Уровни риска**: Каждое приложение получает оценку риска (SAFE → CRITICAL).
- **Foreground notification**: Всегда видно, что защита активна + количество угроз.

## Методы обнаружения

1. **AudioDeviceInfo** (Android 9+) — получение списка активных аудио-девайсов и определение процессов, использующих микрофон.
2. **UsageStatsManager** — аудит обращений всех установленных приложений в foreground за период 24 часа.
3. **ActiveMicrophones API** (Android 12+) — прямое получение информации об активных микрофонах.

## Ограничения доступа Android

> ⚠️ **Важное примечание**: Из-за ограничений безопасности Android некоторые методы имеют ограничения:
> - `PACKAGE_USAGE_STATS` нельзя запросить программно через `requestPermissions()` — пользователь должен включить вручную Settings → Безопасность → Доступ к данным использования.
> - Для точного обнаружения *кто именно* использует микрофон нужен либо root, либо права MediaProjection на Android 10+.

## Стек

- **Язык**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **DI**: Hilt
- **Архитектура**: MVVM
- **MinSdk**: 26 (Android 8.0)
- **TargetSdk**: 35 (Android 15)

## Структура проекта

```
app/
├── src/main/java/com/anton150376afk/antispy/
│   ├── AntSpyApp.kt          — Application + Hilt init
│   ├── MainActivity.kt       — Точка входа, Theme
│   ├── core/
│   │   ├── MicScanEngine.kt  — ядро обнаружения (3 метода)
│   │   ├── MicListenerInfo.kt — модель активного слушателя микрофона
│   │   ├── RiskLevel.kt      — уровни риска (SAFE → CRITICAL)
│   │   └── UsageSummary.kt   — агрегат UsageStats по пакету
│   ├── service/
│   │   ├── AppRiskInfo.kt           — класс оценок риска
│   │   ├── MicMonitoringService.kt  — ForegroundService мониторинга
│   │   ├── UsageStatsAuditor.kt     — wrapper для UsageStatsManager
│   │   ├── PackageAudit.kt          — данные пакета из UsageStats
│   │   ├── UsageStatEvent.kt        — одно событие UsageStats
│   │   └── UsageAuditResult.kt      — результат аудита UsageStats
│   └── ui/dashboard/
│       ├── DashboardViewModel.kt  — ViewModel + UI-состояние
│       └── DashboardScreen.kt     — Compose UI дашборда
├── src/main/res/values/strings.xml  — строки ресурсов
├── src/main/res/values/themes.xml   — тема приложения
├── src/main/res/values/colors.xml   — цвета (фон иконки)
├── src/main/res/drawable/           — векторная иконка лаунчера
├── src/main/res/mipmap-anydpi-v26/  — адаптивные иконки лаунчера
├── src/main/res/xml/                — правила бэкапа
├── proguard-rules.pro               — правила обфускации
└── AndroidManifest.xml              — манифест с разрешениями
```

## Сборка и установка

```bash
# Клонировать репозиторий
git clone https://github.com/anton150376-afk/antispy.git
cd antispy

# Запустить debug сборку
./gradlew assembleDebug

# Или установить на подключённое устройство
./gradlew installDebug
```

## Разрешения (AndroidManifest)

| Permission | Purpose |
|---|---|
| `FOREGROUND_SERVICE_MICROPHONE` | ForegroundService мониторинга mic (Android 14+) |
| `FOREGROUND_SERVICE_CAMERA` | Future: для camera monitoring |
| `FOREGROUND_SERVICE` | Базовый permission для foreground-сервисов (Android 13-) |
| `RECORD_AUDIO` | Проверка audio-девайсов на устройстве |
| `POST_NOTIFICATIONS` | Foreground notification в Android 13+ (runtime) |
| `PACKAGE_USAGE_STATS` | Аудит обращений к сенсорам (включается вручную в настройках) |
| `QUERY_ALL_PACKAGES` | Чтение списка установленных пакетов |
| `RECEIVE_BOOT_COMPLETED` | Автозапуск после перезагрузки |
| `MODIFY_AUDIO_SETTINGS` | Мониторинг аудио-сессий |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Чтобы сервис не убивался энергосбережением |

## Архитектура данных

```
MicScanEngine
   ├── активные слушатели → MicListenerInfo[] (кто пишет в mic прямо сейчас)
   └── UsageStats аудит    → UsageSummary (агрегат по пакету)
                                ↓
UsageStatsAuditor
   └── auditLastHours()    → UsageAuditResult[] (обёртка над PackageAudit)
                                ↓
DashboardViewModel
   └── преобразует в UI-типы → AppScanResult[], AuditEntry[]
                                ↓
DashboardScreen (Compose UI)
```

## Roadmap

- [ ] NotificationListenerService для перехвата уведомлений о записи от других приложений.
- [ ] Камера мониторинг через CameraManager.getCamera.characteristics().
- [ ] Блокировка процесса — root доступ для "убивания" шпионских процессов.
- [ ] Push-уведомления об обнаружении угроз в реальном времени.
- [ ] Export отчётов о найденных угрозах (CSV / PDF).

## Лицензия

MIT License — см. файл [LICENSE](../LICENSE).