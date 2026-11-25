package com.josuetorres.pillowdrop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.josuetorres.pillowdrop.domain.model.ShutdownSettings
import com.josuetorres.pillowdrop.ui.PillowDropUiState
import com.josuetorres.pillowdrop.ui.theme.AmberWarning
import com.josuetorres.pillowdrop.ui.theme.MintSuccess

@Composable
fun AppsSelectorScreen(
    state: PillowDropUiState,
    onUpdateSettings: ((ShutdownSettings) -> ShutdownSettings) -> Unit,
    onTogglePackage: (String) -> Unit,
    onReloadApps: () -> Unit
) {
    val settings = state.settings
    var query by remember { mutableStateOf("") }

    val filteredApps = remember(state.installedApps, query) {
        if (query.isBlank()) {
            state.installedApps
        } else {
            val q = query.trim().lowercase()
            state.installedApps.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Selector de cierre nocturno (Nuke)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onReloadApps) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Recargar apps")
                        }
                    }

                    Text(
                        text = "Primero cuelga la llamada de WhatsApp y 0.7s después cierra los procesos en segundo plano. Las apps de Reloj/Alarma, Teléfono, Teclado y One UI siempre quedan protegidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingToggleRow(
                        title = "Cerrar apps en segundo plano al ejecutar rutina",
                        subtitle = "Usa killBackgroundProcesses de forma segura.",
                        checked = settings.nukeAppsEnabled,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(nukeAppsEnabled = v) } }
                    )

                    HorizontalDivider()

                    SettingToggleRow(
                        title = "Cerrar todas las apps no protegidas",
                        subtitle = "Si lo desactivas, solo se cerrarán las apps que marques abajo (${settings.selectedPackagesToClose.size} elegidas).",
                        checked = settings.nukeAllNonProtected,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(nukeAllNonProtected = v) } }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                label = { Text("Buscar entre tus apps (${state.installedApps.size})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
        }

        items(filteredApps, key = { it.packageName }) { app ->
            val checked = when {
                app.isProtected -> false
                settings.nukeAllNonProtected -> true
                else -> app.packageName in settings.selectedPackagesToClose
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !app.isProtected && !settings.nukeAllNonProtected) {
                        onTogglePackage(app.packageName)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (app.isWhatsApp) {
                                Spacer(Modifier.width(6.dp))
                                BadgePill(text = "Cuelga llamada 1º", color = MintSuccess)
                            }
                            if (app.isProtected) {
                                Spacer(Modifier.width(6.dp))
                                BadgePill(text = "Protegida", color = AmberWarning)
                            }
                        }
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (app.isProtected) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Protegida",
                            tint = AmberWarning
                        )
                    } else {
                        Checkbox(
                            checked = checked,
                            enabled = !settings.nukeAllNonProtected,
                            onCheckedChange = { onTogglePackage(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
