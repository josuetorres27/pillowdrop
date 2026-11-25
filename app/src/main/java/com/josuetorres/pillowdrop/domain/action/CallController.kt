package com.josuetorres.pillowdrop.domain.action

data class ActiveCallSnapshot(
    val packageName: String,
    val callerTitle: String,
    val notificationText: String,
    val category: String?,
    val isOngoing: Boolean,
    val actionLabels: List<String>,
    val hasHangUpAction: Boolean,
    val detectedAtMillis: Long
)

sealed interface CallEndResult {
    data class Ended(val callerTitle: String, val actionLabelUsed: String) : CallEndResult
    data object NoActiveCall : CallEndResult
    data class MissingHangUpAction(val availableActions: List<String>) : CallEndResult
    data class Failed(val errorMessage: String) : CallEndResult
}

interface CallController {
    val id: String
    fun getActiveCall(): ActiveCallSnapshot?
    fun endActiveCall(reason: String): CallEndResult
}
