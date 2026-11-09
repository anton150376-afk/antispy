# Инструкция для ИИ-ассистента по проекту AntiSpy

> **Кому:** локальная модель (qwen3.6:35b-a3b), которая продолжает разработку.
> **Как использовать:** прочитай этот файл ПОЛНОСТЬЮ в начале каждой сессии, прежде чем писать код.
> **Язык ответов:** всегда по-русски.

---

## 1. Что это за проект

**AntiSpy** — Android-приложение для обнаружения шпионских программ, которые слушают микрофон или следят за камерой в фоновом режиме.

Приложение:
- непрерывно сканирует, кто пишет в микрофон прямо сейчас (ForegroundService);
- анализирует UsageStats за последние 24 часа (какие приложения были активны в фоне);
- оценивает уровень риска каждого приложения (SAFE → CRITICAL);
- показывает всё это на экране (Jetpack Compose).

## 2. Путь к проекту

```
C:\Users\user\Desktop\boilerplate-public\notes\antispy
```

> ⚠️ Если проект перенесён в другое место — обнови путь здесь и в остальных инструкциях.

## 3. Стек и версии (НЕ менять без явной команды)

| Что | Значение |
|---|---|
| Язык | Kotlin 2.0.21 |
| UI | Jetpack Compose (Material 3), BOM 2024.11.00 |
| DI | Hilt 2.51.1 |
| MinSdk / TargetSdk | 26 / 35 |
| Java | 17 |
| AGP | 8.7.3 |
| KSP | 2.0.21-1.0.28 |
| Coroutines | 1.8.1 |
| Navigation Compose | 2.8.4 |

## 4. Структура файлов (точные пути)

Все Kotlin-файлы лежат в:
`app\src\main\java\com\anton150376afk\antispy\`

```
AntSpyApp.kt                          — Application + Hilt
MainActivity.kt                       — точка входа, тема, автозапуск сервиса
core\
  MicScanEngine.kt                    — ядро обнаружения (3 метода скана)
  MicListenerInfo.kt                  — data class «кто слушает микрофон»
  RiskLevel.kt                        — enum уровней риска
  UsageSummary.kt                     — data class агрегата UsageStats
service\
  AppRiskInfo.kt                      — data class оценки риска приложения
  MicMonitoringService.kt             — ForegroundService мониторинга
  PackageAudit.kt                     — data class данных пакета из UsageStats
  UsageAuditResult.kt                 — data class результата аудита
  UsageStatEvent.kt                   — data class одного события UsageStats
  UsageStatsAuditor.kt                — обёртка над UsageStatsManager
ui\dashboard\
  DashboardViewModel.kt               — ViewModel + UI-состояние
  DashboardScreen.kt                  — Compose-интерфейс дашборда
```

Ресурсы:
```
app\src\main\res\
  values\strings.xml                  — ВСЕ текстовые строки (готовый набор)
  values\themes.xml                   — тема Theme.AntiSpy
  values\colors.xml                   — цвета
  drawable\ic_launcher_foreground.xml — вектор иконки
  mipmap-anydpi-v26\                  — адаптивные иконки
  xml\backup_rules.xml                — правила бэкапа
  xml\data_extraction_rules.xml       — правила бэкапа (Android 12+)
```

## 5. ПРАВИЛА (обязательные, нарушать нельзя)

1. **Один data class = один файл.** Не клади несколько data class в один файл. Имя файла = имя класса.
2. **Нет inner-классов в сервисах.** Все data class выноси в отдельные файлы.
3. **Нет unused-импортов.** Если импорт не используется — удаляй его.
4. **Тексты — только в `strings.xml`.** Не хардкодь строки в коде. В `strings.xml` уже есть готовый набор ключей (см. раздел 8).
5. **Отвечай по-русски.**
6. **Windows PowerShell 5.1:** нет `&&` (используй `;` или отдельные команды), пути через `\`, не `/c/...`.
7. **`rm -rf` запрещён.** Для удаления — `Remove-Item -Recurse -Force`.
8. **Не делай `git push`.** Коммитить можно, но push выполняет только владелец проекта.
9. **Не меняй версии библиотек** без явной команды.
10. **Микрошаги.** Решай одну задачу за раз, потом докладывай. Не пытайся переписать весь проект сразу.

## 6. Как устроен код (связи между классами)

```
MicScanEngine.scanActiveListeners()
    → возвращает List<MicListenerInfo> (кто пишет в mic сейчас + аудит 24ч)

MicMonitoringService
    → каждые 3 сек вызывает MicScanEngine
    → конвертирует MicListenerInfo → AppRiskInfo
    → обновляет foreground-уведомление

DashboardViewModel
    → вызывает MicScanEngine + UsageStatsAuditor
    → конвертирует результат в AppScanResult / AuditEntry
    → держит состояние в StateFlow<DashboardUiState>

DashboardScreen
    → Compose-UI, читает state из DashboardViewModel
```

Ключевые типы:
- `MicListenerInfo` (core) — packageName, appName, sessionCount, durationMillis, riskLevel.
- `RiskLevel` (core) — enum: SAFE, LOW, MEDIUM, HIGH, CRITICAL.
- `AppRiskInfo` (service) — packageName, appName, score (0-4), lastActiveMs.
- `PackageAudit` (service) — packageName, totalForegroundMs, lastLaunchMillis, launchCount.
- `UsageAuditResult` (service) — обёртка над PackageAudit.
- `AppScanResult`, `AuditEntry`, `DashboardUiState` — UI-типы в DashboardViewModel.kt.

## 7. Что уже реализовано (не ломать)

- ✅ MicScanEngine: 3 метода — runningAppProcesses, activeMicrophones (API 31+), UsageStats audit.
- ✅ MicMonitoringService: ForegroundService с уведомлением, скан каждые 3 сек.
- ✅ UsageStatsAuditor: auditLastHours + getAppUsageEvents.
- ✅ Dashboard: ViewModel + Compose-экран со списками.
- ✅ Hilt DI (AntSpyApp + @HiltViewModel).
- ✅ Ресурсы: тема, иконка, strings.xml, правила бэкапа.
- ✅ Каждый data class в отдельном файле (стандарт бойлера).

## 8. Известные проблемы и следующие задачи (делай по порядку)

> Выполняй задачи СТРОГО по одной. После каждой — отчитайся, что сделал и что проверил.

### Задача 1. Создать notification channel (КРИТИЧНО)
**Проблема:** в `MicMonitoringService.kt` используется `NOTIFICATION_CHANNEL_ID = "antispy_mic_monitor"`, но сам канал нигде не создаётся через `NotificationManager.createNotificationChannel()`. На Android 8+ (minSdk 26) без канала уведомление НЕ покажется.
**Что сделать:** в `MicMonitoringService.onCreate()` создать канал перед `startForeground()`. Имя и описание канала взять из `strings.xml` (ключи `antispy_notification_channel_name`, `antispy_notification_channel_desc` уже есть).
**Как проверить:** запустить приложение на эмуляторе/устройстве — должно показаться постоянное уведомление «Защита активна».

### Задача 2. Запросить POST_NOTIFICATIONS на Android 13+
**Проблема:** `MainActivity` запускает ForegroundService, но не запрашивает runtime-разрешение `POST_NOTIFICATIONS` (нужно на Android 13+). Без него уведомление не покажется.
**Что сделать:** в `MainActivity.onCreate()` перед запуском сервиса проверить и запросить `android.permission.POST_NOTIFICATIONS` (через `ActivityResultContracts.RequestPermission` или `requestPermissions`).
**Как проверить:** на Android 13+ при первом запуске появится системный диалог разрешения.

### Задача 3. Вынести хардкод-строки в strings.xml
**Проблема:** часть строк захардкожена в коде, хотя в `strings.xml` уже есть готовые ключи.
**Где именно:**
- `DashboardScreen.kt`: «Сканирование активных приложений...», «Микрофон чист», «Активных прослушателей не обнаружено», «Настройки защиты», заголовки секций.
- `MicMonitoringService.kt`: «AntiSpy - защита активна», «приложений требуют внимания», «Всё чисто».
- `AppRiskInfo.kt`: «Критическая угроза», «Высокий риск» и т.д. (ключи `risk_safe`...`risk_critical` уже есть).
**Что сделать:** заменить хардкод на `context.getString(R.string.xxx)` (в Composable — `stringResource(R.string.xxx)`).
**Как проверить:** собрать проект, убедиться что строки отображаются корректно.

### Задача 4. Настроить gradle wrapper
**Проблема:** в проекте нет `gradlew.bat` и `gradle\wrapper\`. Сборка из README (`./gradlew assembleDebug`) не сработает.
**Что сделать:** сгенерировать wrapper. Варианты:
- Открыть проект в Android Studio (File → Open) — студия предложит настроить Gradle; или
- Установить Gradle CLI и выполнить `gradle wrapper --gradle-version 8.9`.
**Как проверить:** после настройки `gradlew.bat assembleDebug` должен запуститься.

### Задача 5. (дальше по роадмапу)
Когда задачи 1–4 готовы, переходи к функциональным улучшениям из README, раздел «Roadmap»:
- NotificationListenerService для перехвата уведомлений о записи;
- мониторинг камеры через CameraManager;
- экспорт отчётов (CSV/PDF).

## 9. Как собирать и проверять проект

> ⚠️ Сейчас gradle wrapper НЕ настроен (см. Задачу 4). До этого сборка из командной строки не заработает.

```powershell
# перейти в каталог проекта
cd C:\Users\user\Desktop\boilerplate-public\notes\antispy

# сборка debug-версии (после настройки wrapper)
.\gradlew.bat assembleDebug

# установить на подключённое устройство
.\gradlew.bat installDebug
```

Для проверки синтаксиса без полной сборки (если wrapper не готов) — перечитывай файл глазами: проверь скобки, импорты, соответствие типов.

## 10. Git-правила

- Репозиторий: `https://github.com/anton150376-afk/antispy` (remote `origin`).
- **Коммитить можно, пушить — НЕТ** (это делает владелец).
- После каждой готовой задачи — коммит с понятным сообщением на русском.

```powershell
cd C:\Users\user\Desktop\boilerplate-public\notes\antispy
git add -A
git commit -m "Краткое описание изменения"
```

## 11. Формат ответа после каждой задачи

Отчитывайся так:
1. **Что сделал** (какие файлы изменил/создал).
2. **Почему** (какая проблема решена).
3. **Что проверил** (глазами / сборкой / тестом).
4. **Следующий шаг** (какая задача дальше).

Не пиши длинных простыней — коротко и по делу.
