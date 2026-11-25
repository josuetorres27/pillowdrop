package com.josuetorres.pillowdrop.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.josuetorres.pillowdrop.service.PermissionChecker
import com.josuetorres.pillowdrop.ui.PillowDropUiState
import com.josuetorres.pillowdrop.ui.theme.AmberWarning
import com.josuetorres.pillowdrop.ui.theme.MintSuccess

@Composable
fun PermissionsScreen(
    state: PillowDropUiState,
    onRefreshPermissions: () -> Unit
) {
    val context = LocalContext.current
    val perms = state.permissions

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Salud en Samsung Galaxy S25: ${perms.grantedCount} / 4 listos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.listenerConnected) {
                        "Servicio detector conectado en vivo."
                    } else {
                        "Concede Acceso a Notificaciones para poder detectar y colgar WhatsApp."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        PermissionItemCard(
            title = "1. Acceso a Notificaciones (Esencial)",
            description = "Permite detectar la llamada en curso de WhatsApp y presionar su acción de colgar sin necesidad de root ni Accesibilidad.",
            granted = perms.notificationListenerGranted,
            actionText = "Abrir Acceso a Notificaciones",
            onClick = {
                launchSafely(context, PermissionChecker.buildNotificationListenerIntent())
            }
        )

        PermissionItemCard(
            title = "2. Mostrar Notificaciones",
            description = "Muestra el estado del temporizador nocturno y el botón '+30 min' 2 minutos antes de colgar.",
            granted = perms.postNotificationsGranted,
            actionText = "Conceder permiso",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !perms.postNotificationsGranted) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    launchSafely(context, PermissionChecker.buildAppDetailsIntent(context))
                }
            }
        )

        PermissionItemCard(
            title = "3. Alarmas y recordatorios exactos",
            description = "Garantiza que AlarmManager despierte el teléfono a la hora exacta aunque esté en modo Doze toda la noche.",
            granted = perms.exactAlarmsGranted,
            actionText = "Permitir alarmas exactas",
            onClick = {
                launchSafely(context, PermissionChecker.buildExactAlarmIntent(context))
            }
        )

        PermissionItemCard(
            title = "4. Batería 'No restringido' (One UI)",
            description = "Evita que el optimizador nocturno de Samsung One UI suspenda PillowDrop con la pantalla apagada.",
            granted = perms.batteryUnrestricted,
            actionText = "Excluir de optimización",
            onClick = {
                launchSafely(context, PermissionChecker.buildIgnoreBatteryOptimizationsIntent(context))
            }
        )

        // Samsung One UI Android 13-16 Restricted Settings Tip
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Tip para Samsung S25 (Ajustes restringidos)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Si instalas el APK manualmente y el interruptor de 'Acceso a notificaciones' aparece gris/bloqueado: toca 'Abrir info de la app' abajo, abre el menú ⋮ arriba a la derecha y elige 'Permitir ajustes restringidos'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { launchSafely(context, PermissionChecker.buildAppDetailsIntent(context)) }
                ) {
                    Text("Abrir info de la app en Ajustes")
                }
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    granted: Boolean,
    actionText: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = if (granted) MintSuccess else AmberWarning
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (granted) "Listo" else "Pendiente",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (granted) MintSuccess else AmberWarning
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!granted) {
                Button(onClick = onClick) {
                    Text(actionText)
                }
            } else {
                OutlinedButton(onClick = onClick) {
                    Text("Revisar en Ajustes")
                }
            }
        }
    }
}

private fun launchSafely(context: Context, intent: Intent) {
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        runCatching {
            context.startActivity(PermissionChecker.buildAppDetailsIntent(context))
        }
    }
}
