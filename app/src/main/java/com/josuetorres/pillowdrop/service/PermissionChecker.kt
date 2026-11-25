package com.josuetorres.pillowdrop.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class PermissionStatusSnapshot(
    val notificationListenerGranted: Boolean = false,
    val postNotificationsGranted: Boolean = false,
    val exactAlarmsGranted: Boolean = true,
    val batteryUnrestricted: Boolean = false
) {
    val grantedCount: Int
        get() = listOf(
            notificationListenerGranted,
            postNotificationsGranted,
            exactAlarmsGranted,
            batteryUnrestricted
        ).count { it }

    val allReady: Boolean
        get() = grantedCount == 4
}

object PermissionChecker {

    fun inspect(context: Context): PermissionStatusSnapshot {
        val listenerEnabled = NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)

        val postNotifications = NotificationHelper.canPostNotifications(context)
        val exactAlarms = ShutdownScheduler(context).canScheduleExactAlarms(context)
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryUnrestricted = pm.isIgnoringBatteryOptimizations(context.packageName)

        return PermissionStatusSnapshot(
            notificationListenerGranted = listenerEnabled,
            postNotificationsGranted = postNotifications,
            exactAlarmsGranted = exactAlarms,
            batteryUnrestricted = batteryUnrestricted
        )
    }

    fun buildNotificationListenerIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun buildAppDetailsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun buildExactAlarmIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            buildAppDetailsIntent(context)
        }

    @SuppressLint("BatteryLife")
    fun buildIgnoreBatteryOptimizationsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
