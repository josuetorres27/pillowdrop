package com.josuetorres.pillowdrop.data.apps

object ProtectedAppsPolicy {

    val WHATSAPP_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b"
    )

    /**
     * Critical packages that must never be killed by "Cerrar todas" so morning alarms,
     * emergency/carrier calls, Samsung One UI launcher, and keyboards keep working safely.
     */
    private val EXACT_PROTECTED_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage", // Samsung Clock & Alarms
        "com.samsung.android.calendar",
        "com.google.android.calendar",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.google.android.dialer",
        "com.android.server.telecom",
        "com.sec.android.app.launcher",     // Samsung One UI Home
        "com.google.android.apps.nexuslauncher",
        "com.samsung.android.honeyboard",   // Samsung Keyboard
        "com.google.android.inputmethod.latin", // Gboard
        "com.google.android.gms"
    )

    private val PROTECTED_KEYWORDS = listOf(
        "clock",
        "alarm",
        "launcher",
        "inputmethod",
        "honeyboard",
        "incallui",
        "telecom",
        "systemui"
    )

    fun isProtected(packageName: String, ownPackageName: String): Boolean {
        val pkg = packageName.lowercase()
        if (pkg == ownPackageName.lowercase()) return true
        if (pkg in EXACT_PROTECTED_PACKAGES) return true
        return PROTECTED_KEYWORDS.any { pkg.contains(it) }
    }
}
