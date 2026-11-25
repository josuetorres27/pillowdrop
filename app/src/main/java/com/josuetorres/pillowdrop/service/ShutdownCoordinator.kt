package com.josuetorres.pillowdrop.service

import android.content.Context
import com.josuetorres.pillowdrop.data.diagnostics.DiagnosticLogStore
import com.josuetorres.pillowdrop.data.diagnostics.LogLevel
import com.josuetorres.pillowdrop.data.preferences.SettingsRepository
import com.josuetorres.pillowdrop.domain.action.ShutdownStepReport
import kotlinx.coroutines.delay

class ShutdownCoordinator(
    private val context: Context,
    private val settingsRepository: SettingsRepository = SettingsRepository(context),
    private val whatsAppCallHandler: WhatsAppCallHandler = WhatsAppCallHandler(context),
    private val backgroundAppCloser: BackgroundAppCloser = BackgroundAppCloser(context)
) {

    suspend fun executeRoutine(triggerReason: String): List<ShutdownStepReport> {
        val settings = settingsRepository.getSettingsSnapshot()
        val reports = mutableListOf<ShutdownStepReport>()

        DiagnosticLogStore.log(
            tag = "Coordinator",
            message = "Iniciando rutina nocturna (motivo: $triggerReason).",
            level = LogLevel.INFO
        )

        var callWasPresent = false
        if (settings.hangUpWhatsAppEnabled) {
            callWasPresent = whatsAppCallHandler.getActiveCall() != null
            val callReport = whatsAppCallHandler.execute(settings, triggerReason)
            reports.add(callReport)
        }

        if (settings.nukeAppsEnabled) {
            // Let WhatsApp exit its foreground call service before cleaning background processes
            if (callWasPresent) {
                delay(700L)
            }
            val appsReport = backgroundAppCloser.execute(settings, triggerReason)
            reports.add(appsReport)
        }

        settingsRepository.clearActiveTimer()
        NotificationHelper.dismissActiveTimerNotification(context)

        val summary = if (reports.isEmpty()) {
            "Rutina finalizada sin acciones activas."
        } else {
            reports.joinToString(" · ") { it.summary }
        }
        NotificationHelper.showSummaryNotification(context, summary)
        DiagnosticLogStore.log("Coordinator", "Resumen: $summary", LogLevel.SUCCESS)

        return reports
    }
}
