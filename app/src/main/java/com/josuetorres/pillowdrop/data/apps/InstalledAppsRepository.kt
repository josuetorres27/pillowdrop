package com.josuetorres.pillowdrop.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isProtected: Boolean,
    val isWhatsApp: Boolean
)

class InstalledAppsRepository(private val context: Context) {

    suspend fun getLaunchableApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val ownPkg = context.packageName
        resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == ownPkg) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                InstalledAppItem(
                    packageName = pkg,
                    appName = label,
                    isProtected = ProtectedAppsPolicy.isProtected(pkg, ownPkg),
                    isWhatsApp = pkg in ProtectedAppsPolicy.WHATSAPP_PACKAGES
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareBy<InstalledAppItem> { it.isProtected }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName }
            )
    }
}
