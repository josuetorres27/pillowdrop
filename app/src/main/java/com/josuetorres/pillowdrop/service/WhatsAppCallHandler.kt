package com.josuetorres.pillowdrop.service

import android.app.ActivityOptions
import android.content.Context
import android.os.Build
import com.josuetorres.pillowdrop.data.diagnostics.DiagnosticLogStore
import com.josuetorres.pillowdrop.data.diagnostics.LogLevel
import com.josuetorres.pillowdrop.domain.action.ActiveCallSnapshot
import com.josuetorres.pillowdrop.domain.action.CallController
import com.josuetorres.pillowdrop.domain.action.CallEndResult
import com.josuetorres.pillowdrop.domain.action.ShutdownAction
import com.josuetorres.pillowdrop.domain.action.ShutdownStepReport
import com.josuetorres.pillowdrop.domain.model.ShutdownSettings
import com.josuetorres.pillowdrop.domain.util.ContactNormalizer

class WhatsAppCallHandler(
    private val context: Context
) : CallController, ShutdownAction {

    override val id: String = "whatsapp_hangup"

    override fun getActiveCall(): ActiveCallSnapshot? =
        WhatsAppCallRegistry.activeCallFlow.value

    override fun endActiveCall(reason: String): CallEndResult {
        val sbn = WhatsAppCallRegistry.getCurrentStatusBarNotification()
            ?: return CallEndResult.NoActiveCall.also {
                DiagnosticLogStore.log(
                    tag = "WhatsAppHangUp",
                    message = "Sin llamada activa de WhatsApp al intentar colgar ($reason).",
                    level = LogLevel.WARN
                )
            }

        val notification = sbn.notification
        val snapshot = WhatsAppCallRegistry.activeCallFlow.value
        val caller = snapshot?.callerTitle ?: "WhatsApp"
        val hangUpTrigger = WhatsAppCallRegistry.findHangUpTrigger(notification)
            ?: return CallEndResult.MissingHangUpAction(
                availableActions = snapshot?.actionLabels.orEmpty()
            ).also {
                DiagnosticLogStore.log(
                    tag = "WhatsAppHangUp",
                    message = "No se encontró acción de colgar para '$caller'. Acciones vistas: ${snapshot?.actionLabels}",
                    level = LogLevel.ERROR
                )
            }

        val labelUsed = hangUpTrigger.label
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                    .toBundle()
                hangUpTrigger.pendingIntent.send(
                    context,
                    0,
                    null,
                    null,
                    null,
                    null,
                    options
                )
            } else {
                hangUpTrigger.pendingIntent.send()
            }

            DiagnosticLogStore.log(
                tag = "WhatsAppHangUp",
                message = "Acción '$labelUsed' enviada con éxito para '$caller' ($reason).",
                level = LogLevel.SUCCESS
            )
            CallEndResult.Ended(callerTitle = caller, actionLabelUsed = labelUsed)
        } catch (t: Throwable) {
            val msg = t.message ?: t::class.java.simpleName
            DiagnosticLogStore.log(
                tag = "WhatsAppHangUp",
                message = "Error al enviar PendingIntent '$labelUsed': $msg",
                level = LogLevel.ERROR
            )
            CallEndResult.Failed(msg)
        }
    }

    override suspend fun execute(
        settings: ShutdownSettings,
        triggerReason: String
    ): ShutdownStepReport {
        val active = getActiveCall()
            ?: return ShutdownStepReport(
                actionId = id,
                success = true,
                summary = "No había llamada activa de WhatsApp."
            )

        val whitelisted = ContactNormalizer.matchesWhitelist(
            notificationTitle = active.callerTitle,
            keyword = settings.contactKeyword,
            matchAnyWhatsAppCall = settings.matchAnyWhatsAppCall
        )
        if (!whitelisted) {
            val skipMsg = "Llamada con '${active.callerTitle}' omitida (no coincide con whitelist '${settings.contactKeyword}')."
            DiagnosticLogStore.log("WhatsAppHangUp", skipMsg, LogLevel.INFO)
            return ShutdownStepReport(
                actionId = id,
                success = true,
                summary = skipMsg
            )
        }

        return when (val result = endActiveCall(triggerReason)) {
            is CallEndResult.Ended -> ShutdownStepReport(
                actionId = id,
                success = true,
                summary = "Llamada con '${result.callerTitle}' finalizada ('${result.actionLabelUsed}')."
            )
            CallEndResult.NoActiveCall -> ShutdownStepReport(
                actionId = id,
                success = true,
                summary = "No había llamada activa."
            )
            is CallEndResult.MissingHangUpAction -> ShutdownStepReport(
                actionId = id,
                success = false,
                summary = "No se halló botón de colgar (botones: ${result.availableActions})."
            )
            is CallEndResult.Failed -> ShutdownStepReport(
                actionId = id,
                success = false,
                summary = "Fallo al colgar: ${result.errorMessage}"
            )
        }
    }
}
