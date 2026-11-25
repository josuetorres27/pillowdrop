package com.josuetorres.pillowdrop.data.diagnostics

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

data class DiagnosticEntry(
    val timestampMillis: Long,
    val formattedTime: String,
    val tag: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

/**
 * In-memory ring buffer mirroring events to Android Logcat ("PillowSpike") and to the
 * in-app Spike Lab screen so locked-screen tests can be inspected right on the phone.
 */
object DiagnosticLogStore {

    private const val LOGCAT_TAG = "PillowSpike"
    private const val MAX_ENTRIES = 200
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val _entries = MutableStateFlow<List<DiagnosticEntry>>(emptyList())
    val entries: StateFlow<List<DiagnosticEntry>> = _entries.asStateFlow()

    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val now = System.currentTimeMillis()
        val timeText = runCatching {
            Instant.ofEpochMilli(now)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        }.getOrDefault(now.toString())

        val line = "[$tag] $message"
        runCatching {
            when (level) {
                LogLevel.INFO, LogLevel.SUCCESS -> Log.d(LOGCAT_TAG, line)
                LogLevel.WARN -> Log.w(LOGCAT_TAG, line)
                LogLevel.ERROR -> Log.e(LOGCAT_TAG, line)
            }
        }

        val entry = DiagnosticEntry(
            timestampMillis = now,
            formattedTime = timeText,
            tag = tag,
            message = message,
            level = level
        )
        _entries.update { current ->
            (listOf(entry) + current).take(MAX_ENTRIES)
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    fun exportAsPlainText(): String =
        _entries.value.reversed().joinToString(separator = "\n") { entry ->
            "${entry.formattedTime} [${entry.level.name}/${entry.tag}] ${entry.message}"
        }
}
