package com.stream.nextftv.data.utils

import java.text.Normalizer
import java.util.Locale

object StringUtils {
    private val DIACRITICAL_MARKS = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val DIGITS = Regex("(\\d+)")
    private val LEADING_ZEROS = Regex("^0+(?!$)")
    private val FILENAME_FORBIDDEN = Regex("[\\\\/:*?\"<>|]")
    private val UNDERSCORES = Regex("_+")

    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return DIACRITICAL_MARKS.replace(normalized, "")
            .lowercase(Locale.getDefault())
            .trim()
    }

    /**
     * Genera una clave de ordenamiento natural e insensible a espacios/ceros.
     */
    fun naturalSort(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        val normalized = normalize(input).replace(" ", "")
        
        return DIGITS.replace(normalized) { matchResult ->
            val number = LEADING_ZEROS.replaceFirst(matchResult.value, "")
            number.padStart(10, '0')
        }
    }

    fun sanitizeFileName(name: String): String {
        return FILENAME_FORBIDDEN.replace(name, "")
            .replace(" ", "_")
            .replace(UNDERSCORES, "_")
            .trim('_')
    }
}
