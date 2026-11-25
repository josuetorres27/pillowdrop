package com.josuetorres.pillowdrop.data.apps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedAppsPolicyTest {

    private val ownPkg = "com.josuetorres.pillowdrop"

    @Test
    fun `protects alarms clocks system ui and own package`() {
        assertTrue(ProtectedAppsPolicy.isProtected("com.sec.android.app.clockpackage", ownPkg))
        assertTrue(ProtectedAppsPolicy.isProtected("com.google.android.deskclock", ownPkg))
        assertTrue(ProtectedAppsPolicy.isProtected("com.sec.android.app.launcher", ownPkg))
        assertTrue(ProtectedAppsPolicy.isProtected("com.samsung.android.honeyboard", ownPkg))
        assertTrue(ProtectedAppsPolicy.isProtected(ownPkg, ownPkg))
    }

    @Test
    fun `allows closing regular entertainment and social apps`() {
        assertFalse(ProtectedAppsPolicy.isProtected("com.instagram.android", ownPkg))
        assertFalse(ProtectedAppsPolicy.isProtected("com.spotify.music", ownPkg))
        assertFalse(ProtectedAppsPolicy.isProtected("com.zhiliaoapp.musically", ownPkg))
    }
}
