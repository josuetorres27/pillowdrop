package com.josuetorres.pillowdrop.domain.util

import java.text.Normalizer
import java.util.Locale

object ContactNormalizer {

    private val diacriticsRegex = Regex("\\p{Mn}+")
    private val nonAlphaNumericRegex = Regex("[^\\p{L}\\p{N}\\s]+")
    private val whitespaceRegex = Regex("\\s+")

    /**
     * Exact normalized phrases and keywords that identify a Hang-Up / End-Call
     * notification action across English and Spanish WhatsApp layouts.
     */
    private val hangUpPhrases = listOf(
        "hang up",
        "end call",
        "finalizar llamada",
        "terminar llamada"
    )

    private val hangUpWords = setOf(
        "hangup",
        "end",
        "decline",
        "leave",
        "disconnect",
        "colgar",
        "finalizar",
        "terminar",
        "rechazar",
        "salir"
    )

    /**
     * Normalizes contact names or button labels by decomposing accents (ñ -> n, á -> a),
     * stripping emojis/punctuation, collapsing whitespace, and lowercasing.
     * Example: "Mi Muñequita ❤️" -> "mi munequita"
     */
    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFD)
        val withoutAccents = diacriticsRegex.replace(decomposed, "")
        val alphaNumOnly = nonAlphaNumericRegex.replace(withoutAccents, " ")
        return whitespaceRegex.replace(alphaNumOnly, " ")
            .trim()
            .lowercase(Locale.ROOT)
    }

    /**
     * Checks whether a WhatsApp notification title matches the user's whitelist configuration.
     */
    fun matchesWhitelist(
        notificationTitle: String?,
        keyword: String,
        matchAnyWhatsAppCall: Boolean
    ): Boolean {
        val normalizedTitle = normalize(notificationTitle)
        if (normalizedTitle.isEmpty()) return false
        if (matchAnyWhatsAppCall) return true

        val normalizedKeyword = normalize(keyword)
        if (normalizedKeyword.isEmpty()) return false

        return normalizedTitle.contains(normalizedKeyword)
    }

    /**
     * Checks whether a notification action button label represents hanging up / ending a call.
     * Uses whole-word and multi-word phrase matching so words like "send" never match "end".
     */
    fun isHangUpActionLabel(label: String?): Boolean {
        val normalized = normalize(label)
        if (normalized.isEmpty()) return false
        if (hangUpPhrases.any { phrase -> normalized.contains(phrase) }) {
            return true
        }
        val tokens = normalized.split(" ")
        return tokens.any { token -> token in hangUpWords }
    }
}
