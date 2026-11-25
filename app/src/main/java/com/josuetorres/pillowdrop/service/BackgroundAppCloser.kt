package com.josuetorres.pillowdrop.service

import android.app.ActivityManager
import android.content.Context
import com.josuetorres.pillowdrop.data.apps.InstalledAppsRepository
import com.josuetorres.pillowdrop.data.apps.ProtectedAppsPolicy
import com.josuetorres.pillowdrop.data.diagnostics.DiagnosticLogStore
import com.josuetorres.pillowdrop.data.diagnostics.LogLevel
import com.josuetorres.pillowdrop.domain.action.ShutdownAction
import com.josuetorres.pillowdrop.domain.action.ShutdownStepReport
import com.josuetorres.pillowdrop.domain.model.ShutdownSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackgroundAppCloser(
    private val context: Context,
    private val installedAppsRepository: InstalledAppsRepository = InstalledAppsRepository(context)
) : ShutdownAction {

    override val id: String = "close_background_apps"

    override suspend fun execute(
        settings: ShutdownSettings,
        triggerReason: String
    ): ShutdownStepReport {
        val packagesToClose = if (settings.nukeAllNonProtected) {
            installedAppsRepository.getLaunchableApps()
                .filterNot { it.isProtected }
                .map { it.packageName }
                .toSet()
        } else {
            settings.selectedPackagesToClose
        }

        if (packagesToClose.isEmpty()) {
            return ShutdownStepReport(
                actionId = id,
                success = true,
                summary = "Sin apps seleccionadas para cerrar."
            )
        }

        val closedCount = killPackagesSafely(packagesToClose, triggerReason)
        return ShutdownStepReport(
            actionId = id,
            success = true,
            summary = "Se solicitó cerrar $closedCount apps en segundo plano."
        )
    }

    /**
     * Used directly by Spike 2 button in Spike Lab to test overnight background cleanup
     * across all non-protected launchable apps (excluding WhatsApp if a call is active).
     */
    suspend fun runSpike2KillAllNonProtected(): Int {
        val activeCallActive = WhatsAppCallRegistry.activeCallFlow.value != null
        val targets = installedAppsRepository.getLaunchableApps()
            .filterNot { it.isProtected }
            .filterNot { activeCallActive && it.isWhatsApp }
            .map { it.packageName }
            .toSet()

        return killPackagesSafely(targets, "Spike 2 manual")
    }

    private suspend fun killPackagesSafely(
        packages: Set<String>,
        reason: String
    ): Int = withContext(Dispatchers.IO) {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return@withContext 0

        val ownPkg = context.packageName
        val closedPackages = mutableListOf<String>()

        for (pkg in packages) {
            if (ProtectedAppsPolicy.isProtected(pkg, ownPkg)) continue
            runCatching {
                activityManager.killBackgroundProcesses(pkg)
                closedPackages.add(pkg)
            }.onFailure { err ->
                DiagnosticLogStore.log(
                    tag = "AppCloser",
                    message = "No se pudo cerrar $pkg: ${err.message}",
                    level = LogLevel.WARN
                )
            }
        }

        val sample = closedPackages.take(8).joinToString(", ")
        val extra = if (closedPackages.size > 8) " (+${closedPackages.size - 8} más)" else ""
        DiagnosticLogStore.log(
            tag = "AppCloser",
            message = "[$reason] killBackgroundProcesses ejecutado en ${closedPackages.size} apps: $sample$extra",
            level = LogLevel.SUCCESS
        )
        closedPackages.size
    }
}
