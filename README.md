# PillowDrop

[![Android CI](https://github.com/josuetorres27/pillowdrop/actions/workflows/android-ci.yml/badge.svg)](https://github.com/josuetorres27/pillowdrop/actions/workflows/android-ci.yml)

**Nighttime Shutdown Routine & WhatsApp Call Sleep Timer for Android (Samsung One UI ready).**

PillowDrop prevents waking up to a dead battery and an overheated phone after falling asleep on a late-night WhatsApp call or with dozens of apps left open. Set a countdown timer, pick a target bedtime hour (such as `03:00`), or let it arm automatically when a whitelisted call starts at night.

## Key Features

- **Automatic WhatsApp Call Hang-Up:** Uses Android's `NotificationListenerService` (no root, no fragile Accessibility UI clicking) to detect active calls from `com.whatsapp` and `com.whatsapp.w4b` and fire the notification's hang-up `PendingIntent` (supporting both `Notification.CallStyle` and localized action buttons).
- **Emoji & Accent-Insensitive Contact Whitelist:** Unicode NFD normalization lets a simple keyword like `muñequita` match `"Mi Muñequita ❤️"` while leaving daytime or work calls untouched.
- **Dual Timer Modes:** Hang up after **X minutes** (`15m`, `30m`, `45m`, `60m`, `90m`, `120m`, or `60s` lock-screen test) or at a **specific clock hour** (`03:00`).
- **Battery Guard:** Triggers the nighttime shutdown automatically when unplugged battery drops to or below a configurable threshold (default **15%**).
- **2-Minute Silent Vibration Warning + Snooze:** Gentle double-pulse vibration 2 minutes before hang-up (no audio chime that could leak into the call microphone) with `+30 min` and `Cancelar` actions on the notification.
- **Safe Background App Closer ("Nuke"):** Optionally closes selected or all non-protected background apps `0.7s` after ending the call, while protecting Clock/Alarm, System UI, Phone, Launcher and Keyboard packages.
- **Samsung S25 Permission Health Dashboard:** Live status checks and 1-tap deep links for Notification Access, Notifications, Exact Alarms and One UI Battery Optimization exemption.
- **Built-In Spike Lab:** Real-time `StatusBarNotification` log inspector and 1-tap lock-screen verification controls directly inside the app.

## Project Structure

```text
com.josuetorres.pillowdrop
├── domain/
│   ├── action/        # CallController & ShutdownAction interfaces
│   ├── model/         # ShutdownSettings, ActiveTimerState, TimerMode
│   └── util/          # ContactNormalizer (NFD/emoji strip) & TimeWindowCalculator
├── data/
│   ├── apps/          # InstalledAppsRepository & ProtectedAppsPolicy
│   ├── diagnostics/   # Real-time DiagnosticLogStore ring buffer
│   └── preferences/   # Jetpack DataStore (Preferences) repository
├── service/
│   ├── PillowDropNotificationService.kt  # WhatsApp call listener + Battery Guard
│   ├── WhatsAppCallHandler.kt            # CallStyle & bilingual PendingIntent terminator
│   ├── BackgroundAppCloser.kt            # Safe killBackgroundProcesses implementation
│   ├── ShutdownCoordinator.kt            # Ordered call-hangup -> settle -> app-close
│   ├── ShutdownScheduler.kt              # AlarmManager setExactAndAllowWhileIdle
│   └── ShutdownAlarmReceiver.kt          # WakeLock execution, 2-min warning & +30m snooze
└── ui/
    ├── screens/       # TimerScreen, AppsSelectorScreen, PermissionsScreen, SpikeLabScreen
    ├── theme/         # Bedroom-friendly Material 3 OLED dark theme
    ├── MainViewModel.kt
    └── PillowDropApp.kt
```

## Running on Samsung Galaxy S25

1. Open this repository in **Android Studio** (Ladybug / Meerkat or newer) and wait for Gradle sync.
2. Enable **Developer Options** and **USB Debugging** (or **Wireless Debugging**) on your Galaxy S25, then click **Run**.
3. Open the **Permisos** tab inside PillowDrop and grant:
   - **Acceso a Notificaciones** (*on Android 13–16 sideloaded APKs, if the toggle is disabled, open Settings → Apps → PillowDrop → ⋮ menu → "Allow restricted settings" first*).
   - **Mostrar Notificaciones**, **Alarmas exactas** and **Batería "No restringido"**.
4. Open the **Spike Lab** tab, place a test WhatsApp call, tap **Colgar en 60s (bloqueo)**, lock the screen, and verify automatic call termination.

## Tests & CI

Unit tests cover contact normalization, bilingual hang-up button matching, overnight schedule windows and protected package exclusions (`3` suites, `8` unit tests):

```bash
gradle testDebugUnitTest assembleDebug
```
