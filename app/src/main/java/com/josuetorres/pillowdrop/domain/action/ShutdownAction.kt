package com.josuetorres.pillowdrop.domain.action

import com.josuetorres.pillowdrop.domain.model.ShutdownSettings

data class ShutdownStepReport(
    val actionId: String,
    val success: Boolean,
    val summary: String
)

interface ShutdownAction {
    val id: String
    suspend fun execute(settings: ShutdownSettings, triggerReason: String): ShutdownStepReport
}
