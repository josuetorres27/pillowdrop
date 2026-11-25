package com.josuetorres.pillowdrop.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.josuetorres.pillowdrop.domain.model.ShutdownSettings
import com.josuetorres.pillowdrop.domain.model.TimerMode
import com.josuetorres.pillowdrop.domain.util.ContactNormalizer
import com.josuetorres.pillowdrop.domain.util.TimeWindowCalculator
import com.josuetorres.pillowdrop.ui.PillowDropUiState
import com.josuetorres.pillowdrop.ui.theme.AmberWarning
import com.josuetorres.pillowdrop.ui.theme.CoralDanger
import com.josuetorres.pillowdrop.ui.theme.MintSuccess
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(
    state: PillowDropUiState,
    onStartMinutes: (Int) -> Unit,
    onStartSixtySecTest: () -> Unit,
    onStartTargetClock: () -> Unit,
    onSnooze30m: () -> Unit,
    onExtend5m: () -> Unit,
    onCancelTimer: () -> Unit,
    onUpdateSettings: ((ShutdownSettings) -> ShutdownSettings) -> Unit
) {
    val settings = state.settings
    var sampleContact by remember { mutableStateOf("Mi Muñequita ❤️") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live WhatsApp Call Detection Banner
        state.activeCall?.let { call ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneInTalk,
                        contentDescription = null,
                        tint = MintSuccess
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Llamada activa: ${call.callerTitle}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (call.hasHangUpAction) {
                                "Botón de colgar listo (${call.actionLabels.joinToString()})"
                            } else {
                                "Sin botón reconocido (${call.actionLabels.joinToString()})"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Active Countdown Hero Card
        if (state.activeTimer.isActive) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.activeTimer.label.ifBlank { "Rutina nocturna activa" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = TimeWindowCalculator.formatCountdown(state.remainingMillis),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onExtend5m) {
                            Text("+5 min")
                        }
                        Button(onClick = onSnooze30m) {
                            Text("+30 min")
                        }
                        OutlinedButton(
                            onClick = onCancelTimer,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger)
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }

        // Quick Timer & Target Hour Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Programar apagado / colgado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.timerMode == TimerMode.DURATION,
                        onClick = { onUpdateSettings { it.copy(timerMode = TimerMode.DURATION) } },
                        label = { Text("Por duración") },
                        leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = null) }
                    )
                    FilterChip(
                        selected = settings.timerMode == TimerMode.TARGET_TIME,
                        onClick = { onUpdateSettings { it.copy(timerMode = TimerMode.TARGET_TIME) } },
                        label = { Text("A una hora fija") },
                        leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) }
                    )
                }

                if (settings.timerMode == TimerMode.DURATION) {
                    Text(
                        text = "Toca una duración para iniciar ahora (y dejarla como predeterminada):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = onStartSixtySecTest,
                            label = { Text("⚡ 60s Test") }
                        )
                        listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                            FilterChip(
                                selected = settings.defaultDurationMinutes == mins,
                                onClick = { onStartMinutes(mins) },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Hora objetivo: ${TimeWindowCalculator.formatHourMinute(settings.targetHour, settings.targetMinute)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to 30, 2 to 0, 2 to 30, 3 to 0, 3 to 30, 4 to 0).forEach { (h, m) ->
                            val label = TimeWindowCalculator.formatHourMinute(h, m)
                            FilterChip(
                                selected = settings.targetHour == h && settings.targetMinute == m,
                                onClick = {
                                    onUpdateSettings { it.copy(targetHour = h, targetMinute = m) }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val nextH = (settings.targetHour + 23) % 24
                                onUpdateSettings { it.copy(targetHour = nextH) }
                            }
                        ) { Text("-1h") }
                        OutlinedButton(
                            onClick = {
                                val nextH = (settings.targetHour + 1) % 24
                                onUpdateSettings { it.copy(targetHour = nextH) }
                            }
                        ) { Text("+1h") }
                        OutlinedButton(
                            onClick = {
                                val nextM = (settings.targetMinute + 15) % 60
                                onUpdateSettings { it.copy(targetMinute = nextM) }
                            }
                        ) { Text("+15m") }
                        Button(
                            onClick = onStartTargetClock,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Activar")
                        }
                    }
                }
            }
        }

        // Contact Whitelist Card (Accent & Emoji Resilient)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Contacto de WhatsApp (Whitelist)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Ignora tildes, mayúsculas y emojis automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = settings.contactKeyword,
                    onValueChange = { kw -> onUpdateSettings { it.copy(contactKeyword = kw) } },
                    enabled = !settings.matchAnyWhatsAppCall,
                    label = { Text("Palabra clave (ej. muñequita)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sampleContact,
                    onValueChange = { sampleContact = it },
                    label = { Text("Probar coincidencia con nombre en WhatsApp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val matchesSample = ContactNormalizer.matchesWhitelist(
                    notificationTitle = sampleContact,
                    keyword = settings.contactKeyword,
                    matchAnyWhatsAppCall = settings.matchAnyWhatsAppCall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (matchesSample) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (matchesSample) MintSuccess else AmberWarning
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (matchesSample) {
                            "Coincide con \"$sampleContact\" (normalizado: \"${ContactNormalizer.normalize(sampleContact)}\")"
                        } else {
                            "No coincide con \"$sampleContact\" — ajusta la palabra clave."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                HorizontalDivider()

                SettingToggleRow(
                    title = "Aplicar a cualquier llamada de WhatsApp",
                    subtitle = "Desactiva el filtro por nombre de contacto.",
                    checked = settings.matchAnyWhatsAppCall,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(matchAnyWhatsAppCall = v) } }
                )

                SettingToggleRow(
                    title = "Auto-iniciar al detectar la llamada",
                    subtitle = "Arma el temporizador solo cuando inicia la llamada dentro del horario nocturno.",
                    checked = settings.autoStartOnWhitelistedCall,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(autoStartOnWhitelistedCall = v) } }
                )
            }
        }

        // Night Schedule & Battery Guard Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Guardia de batería y horario nocturno",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                SettingToggleRow(
                    title = "Guardia de batería (${settings.batteryThresholdPercent}%)",
                    subtitle = "Ejecuta la rutina si el S25 baja de ${settings.batteryThresholdPercent}% sin estar cargando.",
                    checked = settings.batteryGuardEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(batteryGuardEnabled = v) } }
                )

                if (settings.batteryGuardEnabled) {
                    Slider(
                        value = settings.batteryThresholdPercent.toFloat(),
                        onValueChange = { raw ->
                            onUpdateSettings { it.copy(batteryThresholdPercent = raw.roundToInt()) }
                        },
                        valueRange = 5f..35f,
                        steps = 5
                    )
                }

                HorizontalDivider()

                SettingToggleRow(
                    title = "Ventana nocturna (${TimeWindowCalculator.formatHourMinute(settings.nightStartHour, settings.nightStartMinute)} – ${TimeWindowCalculator.formatHourMinute(settings.nightEndHour, settings.nightEndMinute)})",
                    subtitle = "Evita cortar llamadas largas durante la tarde.",
                    checked = settings.nightWindowEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(nightWindowEnabled = v) } }
                )

                SettingToggleRow(
                    title = "Modo prueba todo el día",
                    subtitle = "Ignora la ventana nocturna para probar de día en 1 minuto.",
                    checked = settings.allDayTestMode,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(allDayTestMode = v) } }
                )

                SettingToggleRow(
                    title = "Vibración suave 2 min antes (+30 min)",
                    subtitle = "Sin sonido para que no se escuche por el micrófono de la llamada.",
                    checked = settings.warningVibrationEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(warningVibrationEnabled = v) } }
                )
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
