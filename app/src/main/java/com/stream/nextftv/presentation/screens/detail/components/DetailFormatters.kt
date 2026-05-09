package com.stream.nextftv.presentation.screens.detail.components

fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}

fun parseDurationToMs(raw: String?): Long {
    val input = raw?.trim().orEmpty()
    if (input.isBlank()) return 0L

    val hhMmSs = Regex("""^(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?$""").matchEntire(input)
    if (hhMmSs != null) {
        val a = hhMmSs.groupValues[1].toLongOrNull() ?: return 0L
        val b = hhMmSs.groupValues[2].toLongOrNull() ?: return 0L
        val c = hhMmSs.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L
        val totalSeconds = if (hhMmSs.groupValues[3].isNotBlank()) (a * 3600L) + (b * 60L) + c else (a * 60L) + b
        return totalSeconds * 1000L
    }

    val numeric = Regex("""(\d+(?:[.,]\d+)?)""").find(input)?.value
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?: return 0L

    return when {
        input.contains("h", ignoreCase = true) -> (numeric * 60.0 * 60.0 * 1000.0).toLong()
        input.contains("min", ignoreCase = true) || input.contains("m", ignoreCase = true) ->
            (numeric * 60.0 * 1000.0).toLong()
        else -> (numeric * 60.0 * 1000.0).toLong()
    }
}
