package com.josuetorres.pillowdrop.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.josuetorres.pillowdrop.data.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(appContext)
                val activeTimer = repo.getActiveTimerSnapshot()
                if (activeTimer.isActive) {
                    val now = System.currentTimeMillis()
                    val scheduler = ShutdownScheduler(appContext, repo)
                    if (activeTimer.triggerEpochMillis > now + 5_000L) {
                        scheduler.scheduleExactTimestamp(
                            triggerEpochMillis = activeTimer.triggerEpochMillis,
                            autoStartedFromCall = activeTimer.autoStartedFromCall,
                            label = activeTimer.label.ifBlank { "Restaurado tras reinicio" }
                        )
                    } else {
                        repo.clearActiveTimer()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
