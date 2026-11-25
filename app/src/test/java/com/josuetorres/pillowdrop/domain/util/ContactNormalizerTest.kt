package com.josuetorres.pillowdrop.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactNormalizerTest {

    @Test
    fun `normalizes accents emojis and casing`() {
        assertEquals("mi munequita", ContactNormalizer.normalize("Mi Muñequita ❤️"))
        assertEquals("amorcito lindo", ContactNormalizer.normalize("  ¡Amorcito   Lindo! ✨🌙 "))
    }

    @Test
    fun `matches girlfriend contact across accents and emojis`() {
        assertTrue(
            ContactNormalizer.matchesWhitelist(
                notificationTitle = "Mi Muñequita ❤️",
                keyword = "muñequita",
                matchAnyWhatsAppCall = false
            )
        )
        assertTrue(
            ContactNormalizer.matchesWhitelist(
                notificationTitle = "Mi Muñequita ❤️",
                keyword = "MUNEQUITA",
                matchAnyWhatsAppCall = false
            )
        )
        assertFalse(
            ContactNormalizer.matchesWhitelist(
                notificationTitle = "Grupo Trabajo UCR",
                keyword = "muñequita",
                matchAnyWhatsAppCall = false
            )
        )
    }

    @Test
    fun `detects bilingual hangup buttons without false positives`() {
        assertTrue(ContactNormalizer.isHangUpActionLabel("Colgar"))
        assertTrue(ContactNormalizer.isHangUpActionLabel("Finalizar llamada"))
        assertTrue(ContactNormalizer.isHangUpActionLabel("Hang up"))
        assertTrue(ContactNormalizer.isHangUpActionLabel("End call"))
        assertTrue(ContactNormalizer.isHangUpActionLabel("Decline"))
        assertTrue(ContactNormalizer.isHangUpActionLabel("Rechazar"))

        // Must never match unrelated words containing "end"
        assertFalse(ContactNormalizer.isHangUpActionLabel("Send message"))
        assertFalse(ContactNormalizer.isHangUpActionLabel("Silenciar"))
        assertFalse(ContactNormalizer.isHangUpActionLabel("Altavoz"))
        assertFalse(ContactNormalizer.isHangUpActionLabel("Mute"))
    }
}
