# Project: PillowDrop (Nighttime Shutdown Routine & Call Sleep Timer)

## 1. Context & Problem Statement
- **Core Purpose:** Native Android nighttime utility that automatically hangs up ongoing WhatsApp calls and optionally closes background apps when the user falls asleep—triggered by a countdown timer, a target clock time, or low battery.
- **Primary Pain Points Solved:** Prevents waking up to a dead battery, an overheating phone, missed morning alarms, and dozens of lingering apps left open overnight.
- **Target Device & Environment:** Built and tested primarily on **Samsung Galaxy S25 (Android 15/16 · One UI 7/8)** via Android Studio USB/Wireless debugging, architected cleanly for GitHub portfolio quality and future Google Play Store compliance.

## 2. Tech Stack & Architecture
- **Language:** 100% Kotlin (JVM 17, Coroutines & `StateFlow`).
- **UI Framework:** Jetpack Compose + Material 3 (OLED dark theme optimized for dark bedrooms).
- **Architecture:** Clean MVVM (`ui` -> `domain` <- `data` / `service`).
- **Persistence:** Jetpack DataStore (`Preferences`)—lightweight, reactive, zero boilerplate for user preferences and active timer state.
- **Package ID:** `com.josuetorres.pillowdrop`

## 3. Core Functional Architecture

### A. Unified `ShutdownAction` & `CallController` Abstractions
1. **`HangUpCallAction` (`WhatsAppCallHandler` implementing `CallController`):**
   - Uses `PillowDropNotificationService` (`NotificationListenerService`) to inspect ongoing notifications from `com.whatsapp` and `com.whatsapp.w4b`.
   - **Accent- & Emoji-Resilient Contact Whitelist:** Normalizes notification titles via Unicode NFD decomposition (stripping diacritics, emojis, and punctuation) so a keyword like `muñequita` matches `"Mi Muñequita ❤️"`.
   - **Bilingual Hang-Up Matching:** Detects semantic call-dismiss actions and case/accent-insensitive labels across English (`hang up`, `end call`, `decline`, `leave`) and Spanish (`colgar`, `finalizar`, `terminar`, `rechazar`), invoking `Notification.Action.actionIntent.send()`.
2. **`CloseBackgroundAppsAction` ("Nuke" / App Selector):**
   - Runs *after* call termination (with a short settle delay so WhatsApp exits its foreground VoIP state first) and invokes `ActivityManager.killBackgroundProcesses()` on user-selected apps or all non-protected launchable apps.
   - **Protected Apps Policy:** Hard-protects alarm/clock apps (`com.sec.android.app.clockpackage`, `com.google.android.deskclock`), dialer/telecom, system UI, launchers, keyboards, and PillowDrop itself so morning alarms are never disrupted.
   - Uses `<queries>` for `ACTION_MAIN` + `CATEGORY_LAUNCHER` rather than Play-restricted `QUERY_ALL_PACKAGES`.

### B. Triggers & One UI Resilience
- **Countdown Timer & Target Hour:** Supports both *"En X minutos"* (including a **60s lock-screen test preset**) and *"A una hora fija"* (e.g., `03:00`).
- **Auto-Start Night Window:** Automatically arms the sleep timer when a whitelisted WhatsApp call begins during the configured bedtime window (e.g., `22:00 – 07:00`, or anytime when *All-Day Test Mode* is active) and cancels cleanly if the call ends manually beforehand.
- **AlarmManager Scheduling:** Schedules exact wakeups via `AlarmManager.setExactAndAllowWhileIdle` + `ShutdownAlarmReceiver` (`BroadcastReceiver` + short `PARTIAL_WAKE_LOCK`) instead of fragile multi-hour coroutine delays or Android 12–16 restricted background Foreground Service launches.
- **Battery Guard:** Dynamic `ACTION_BATTERY_CHANGED` monitor inside the bound `NotificationListenerService` triggers the shutdown routine automatically if battery drops to or below `N%` (default `15%`) while unplugged.
- **2-Minute Vibration Warning + Snooze:** Vibrates gently 2 minutes before execution (never plays an audio chime that would leak over the call mic) and posts `+30 min` Snooze and `Cancelar` actions on the notification.

### C. Built-In Spike Lab (Diagnostics)
- Rather than maintaining a separate throwaway project, the app includes a **Spike Lab** tab with a live ring-buffer log of `StatusBarNotification` metadata, 1-tap **Hang up now**, **Hang up in 60s (lock-screen test)**, and **Spike 2 (background app closer)** actions.

## 4. Roadmap
1. **Phase 1 (Current — Preliminary MVP + Spike Lab):** Complete Android project with WhatsApp `NotificationListenerService`, alarm-based countdown & target hour, Battery Guard, Protected App Closer, S25 Permissions Dashboard, Spike Lab, and unit tests.
2. **Phase 2 (Hardware Validation on S25):** Run the 6 lock-screen/call scenarios in Spike Lab and compare overnight battery telemetry with and without background app cleanup.
3. **Phase 3 (Optional Power-User & Play Expansion):** Optional Shizuku `am force-stop` adapter for advanced sideload users, `CarrierCallHandler` if cellular calls are ever needed, KMP shared timer logic, and Google Play Console declarations.
