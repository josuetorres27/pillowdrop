package com.josuetorres.pillowdrop.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.josuetorres.pillowdrop.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationHelper {

    private const val CHANNEL_STATUS = "pillowdrop_timer_status"
    private const val CHANNEL_WARNING = "pillowdrop_snooze_warning"

    const val NOTIFICATION_ID_TIMER = 1001
    const val NOTIFICATION_ID_SUMMARY = 1002

    private val clockFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val statusChannel = NotificationChannel(
            CHANNEL_STATUS,
            "Estado del temporizador nocturno",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Muestra cuándo PillowDrop colgará la llamada o cerrará apps."
            setShowBadge(false)
        }

        val warningChannel = NotificationChannel(
            CHANNEL_WARNING,
            "Aviso de 2 minutos (+30 min)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Vibración suave 2 minutos antes de colgar por si sigues despierto."
            enableVibration(true)
            setSound(null, null) // Never play audio that could leak into the call microphone
        }

        nm.createNotificationChannel(statusChannel)
        nm.createNotificationChannel(warningChannel)
    }

    fun showActiveTimerNotification(
        context: Context,
        triggerEpochMillis: Long,
        isTwoMinuteWarning: Boolean
    ) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)

        val formattedTarget = Instant.ofEpochMilli(triggerEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(clockFormatter)

        val launchIntent = (context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setPackage(context.packageName)).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            10,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            11,
            Intent(context, ShutdownAlarmReceiver::class.java).apply {
                action = ShutdownAlarmReceiver.ACTION_SNOOZE_30M
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = PendingIntent.getBroadcast(
            context,
            12,
            Intent(context, ShutdownAlarmReceiver::class.java).apply {
                action = ShutdownAlarmReceiver.ACTION_CANCEL
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = if (isTwoMinuteWarning) CHANNEL_WARNING else CHANNEL_STATUS
        val title = if (isTwoMinuteWarning) {
            "¿Sigues despierto? Colgando en 2 min ($formattedTarget)"
        } else {
            "PillowDrop activo · Rutina a las $formattedTarget"
        }
        val body = if (isTwoMinuteWarning) {
            "Toca '+30 min' para extender la llamada o 'Cancelar' para desactivar."
        } else {
            "Colgará la llamada de WhatsApp automáticamente si te quedas dormido."
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_moon)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(!isTwoMinuteWarning)
            .setSilent(!isTwoMinuteWarning)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent)
            .addAction(0, "+30 min", snoozeIntent)
            .addAction(0, "Cancelar", cancelIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER, notification)
    }

    fun dismissActiveTimerNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_TIMER)
    }

    fun showSummaryNotification(context: Context, summaryText: String) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setPackage(context.packageName)

        val openAppIntent = PendingIntent.getActivity(
            context,
            13,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification_moon)
            .setContentTitle("PillowDrop ejecutó tu rutina nocturna")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setAutoCancel(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SUMMARY, notification)
    }

    fun triggerGentleWarningVibration(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() != true) return

        val pattern = longArrayOf(0L, 180L, 140L, 220L)
        val amplitudes = intArrayOf(0, 90, 0, 120)
        val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
