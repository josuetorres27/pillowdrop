package com.josuetorres.pillowdrop.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.josuetorres.pillowdrop.data.diagnostics.DiagnosticLogStore
import com.josuetorres.pillowdrop.data.diagnostics.LogLevel
import com.josuetorres.pillowdrop.ui.PillowDropUiState
import com.josuetorres.pillowdrop.ui.theme.AmberWarning
import com.josuetorres.pillowdrop.ui.theme.CoralDanger
import com.josuetorres.pillowdrop.ui.theme.MintSuccess

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpikeLabScreen(
    state: PillowDropUiState,
    onHangUpNow: () -> Unit,
    onStartSixtySecLockTest: () -> Unit,
    onRunSpike2KillApps: () -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Spike Lab · Validación en Galaxy S25",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prueba aquí los escenarios críticos (especialmente pantalla bloqueada durante 60s) y copia el registro si algún botón de WhatsApp cambia de nombre.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onHangUpNow,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralDanger)
                        ) {
                            Icon(Icons.Outlined.PhoneDisabled, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Colgar ahora")
                        }

                        Button(onClick = onStartSixtySecLockTest) {
                            Icon(Icons.Outlined.Timer, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Colgar en 60s (bloqueo)")
                        }

                        OutlinedButton(onClick = onRunSpike2KillApps) {
                            Text("Spike 2: Cerrar apps 2do plano")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val text = DiagnosticLogStore.exportAsPlainText()
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("PillowDrop Logs", text))
                                Toast.makeText(context, "Logs copiados al portapapeles", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Copiar (${state.logs.size})")
                        }

                        OutlinedButton(onClick = onClearLogs) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Limpiar")
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Protocolo de prueba rápido:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("1. Haz una llamada de voz de WhatsApp y pulsa 'Colgar ahora'.", style = MaterialTheme.typography.bodySmall)
                    Text("2. Haz otra llamada, pulsa 'Colgar en 60s', bloquea el S25 de inmediato y espera.", style = MaterialTheme.typography.bodySmall)
                    Text("3. Prueba videollamada y verifica que también detecte la acción de colgar.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (state.logs.isEmpty()) {
            item {
                Text(
                    text = "Aún no hay eventos registrados. Inicia una llamada de WhatsApp o activa un temporizador para ver las notificaciones inspeccionadas en tiempo real.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(state.logs, key = { "${it.timestampMillis}_${it.tag}_${it.message.hashCode()}" }) { entry ->
                val badgeColor = when (entry.level) {
                    LogLevel.SUCCESS -> MintSuccess
                    LogLevel.WARN -> AmberWarning
                    LogLevel.ERROR -> CoralDanger
                    LogLevel.INFO -> MaterialTheme.colorScheme.primary
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${entry.formattedTime} · ${entry.tag}",
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = entry.message,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
