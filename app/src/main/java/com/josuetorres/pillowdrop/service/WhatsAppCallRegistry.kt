package com.josuetorres.pillowdrop.service

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.josuetorres.pillowdrop.data.apps.ProtectedAppsPolicy
import com.josuetorres.pillowdrop.domain.action.ActiveCallSnapshot
import com.josuetorres.pillowdrop.domain.util.ContactNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ResolvedHangUpTrigger(
    val label: String,
    val pendingIntent: PendingIntent
)

object WhatsAppCallRegistry {

    // Android 12+ Notification.CallStyle extras keys in AOSP
    private const val EXTRA_HANG_UP_INTENT = "android.hangUpIntent"
    private const val EXTRA_DECLINE_INTENT = "android.declineIntent"

    @Volatile
    private var latestCallSbn: StatusBarNotification? = null

    private val _activeCallFlow = MutableStateFlow<ActiveCallSnapshot?>(null)
    val activeCallFlow: StateFlow<ActiveCallSnapshot?> = _activeCallFlow.asStateFlow()

    private val _listenerConnected = MutableStateFlow(false)
    val listenerConnected: StateFlow<Boolean> = _listenerConnected.asStateFlow()

    fun setListenerConnected(connected: Boolean) {
        _listenerConnected.value = connected
        if (!connected) {
            clearActiveCall()
        }
    }

    fun isWhatsAppPackage(packageName: String?): Boolean =
        packageName != null && packageName in ProtectedAppsPolicy.WHATSAPP_PACKAGES

    fun findHangUpTrigger(notification: Notification): ResolvedHangUpTrigger? {
        // 1. Check Android 12+ CallStyle hang-up / decline PendingIntents in extras (used by One UI pills)
        val extras = notification.extras
        getPendingIntentExtra(extras, EXTRA_HANG_UP_INTENT)?.let { pi ->
            return ResolvedHangUpTrigger(label = "CallStyle.hangUpIntent", pendingIntent = pi)
        }

        // 2. Match English & Spanish hang-up / end-call Notification.Action buttons
        notification.actions?.firstOrNull { action ->
            action.actionIntent != null &&
                ContactNormalizer.isHangUpActionLabel(action.title?.toString())
        }?.let { action ->
            return ResolvedHangUpTrigger(
                label = action.title?.toString().orEmpty().ifBlank { "action_hangup" },
                pendingIntent = action.actionIntent
            )
        }

        // 3. Fallback to CallStyle declineIntent (e.g. incoming ringing call)
        getPendingIntentExtra(extras, EXTRA_DECLINE_INTENT)?.let { pi ->
            return ResolvedHangUpTrigger(label = "CallStyle.declineIntent", pendingIntent = pi)
        }

        return null
    }

    private fun getPendingIntentExtra(extras: Bundle?, key: String): PendingIntent? {
        if (extras == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(key, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(key) as? PendingIntent
        }
    }

    fun inspectCallCandidate(sbn: StatusBarNotification): ActiveCallSnapshot? {
        if (!isWhatsAppPackage(sbn.packageName)) return null
        val notification = sbn.notification ?: return null
        val extras = notification.extras

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val category = notification.category
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

        val actions = buildList {
            notification.actions?.forEach { act ->
                add(act.title?.toString()?.ifBlank { "<sin texto>" } ?: "<null>")
            }
            if (getPendingIntentExtra(extras, EXTRA_HANG_UP_INTENT) != null) {
                add("[CallStyle:hangUpIntent]")
            }
            if (getPendingIntentExtra(extras, EXTRA_DECLINE_INTENT) != null) {
                add("[CallStyle:declineIntent]")
            }
        }

        val hangUpTrigger = findHangUpTrigger(notification)
        val looksLikeCall = category == Notification.CATEGORY_CALL ||
            (isOngoing && hangUpTrigger != null)

        if (!looksLikeCall) return null

        return ActiveCallSnapshot(
            packageName = sbn.packageName,
            callerTitle = title.ifBlank { "Desconocido" },
            notificationText = text,
            category = category,
            isOngoing = isOngoing,
            actionLabels = actions,
            hasHangUpAction = hangUpTrigger != null,
            detectedAtMillis = System.currentTimeMillis()
        )
    }

    fun updateActiveCall(sbn: StatusBarNotification, snapshot: ActiveCallSnapshot) {
        latestCallSbn = sbn
        _activeCallFlow.value = snapshot
    }

    fun clearIfMatchesKey(key: String): Boolean {
        val current = latestCallSbn ?: return false
        if (current.key == key) {
            clearActiveCall()
            return true
        }
        return false
    }

    fun clearActiveCall() {
        latestCallSbn = null
        _activeCallFlow.value = null
    }

    fun getCurrentStatusBarNotification(): StatusBarNotification? = latestCallSbn
}
