package com.stream.nextftv.presentation.screens.seriesdetail

import com.stream.nextftv.data.remote.SeriesEpisodeDto
import java.util.Locale

private fun String.normalizedForComparison(): String {
    return lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
        .trim()
}

private fun escapeRegex(value: String): String = Regex.escape(value)

private fun buildEpisodePrefixRegexes(
    seasonNumber: Int?,
    episodeNumber: Int?
): List<Regex> {
    if (episodeNumber == null) return emptyList()

    val episode = episodeNumber.toString()
    val episodePadded = episodeNumber.toString().padStart(2, '0')
    val season = seasonNumber?.toString()
    val seasonPadded = seasonNumber?.toString()?.padStart(2, '0')
    val separator = "[\\s._:-]*"
    val trailing = "(?:\\s*[-:|.)]+\\s*|\\s+)+"
    val regexes = mutableListOf<Regex>()

    fun add(pattern: String) {
        regexes += Regex("^$pattern", RegexOption.IGNORE_CASE)
    }

    if (season != null && seasonPadded != null) {
        add("(?:s${separator}(?:${escapeRegex(seasonPadded)}|${escapeRegex(season)})${separator}e${separator}(?:${escapeRegex(episodePadded)}|${escapeRegex(episode)})$trailing)")
        add("(?:(?:${escapeRegex(seasonPadded)}|${escapeRegex(season)})${separator}x${separator}(?:${escapeRegex(episodePadded)}|${escapeRegex(episode)})$trailing)")
        add("(?:temporada${separator}${escapeRegex(season)}${separator}episodio${separator}(?:${escapeRegex(episodePadded)}|${escapeRegex(episode)})$trailing)")
    }

    add("(?:e${separator}(?:${escapeRegex(episodePadded)}|${escapeRegex(episode)})$trailing)")
    add("(?:episode${separator}(?:${escapeRegex(episodePadded)}|${escapeRegex(episode)})$trailing)")
    add("(?:episodio${separator}(?:${escapeRegex(episodePadded)}|${escapeRegex(episode)})$trailing)")
    add("(?:capitulo${separator}(?:${escapeRegex(episodePadded)}|${escapeRegex(episode)})$trailing)")
    add("(?:capítulo${separator}(?:${escapeRegex(episodePadded)}|${escapeRegex(episode)})$trailing)")

    return regexes
}

private fun stripSeriesNameFromEpisodeTitle(
    seriesName: String?,
    episodeTitle: String?
): String {
    val rawTitle = episodeTitle?.trim().orEmpty()
    if (rawTitle.isBlank()) return ""

    val cleanSeriesName = seriesName?.trim().orEmpty()
    if (cleanSeriesName.isBlank()) return rawTitle

    val normalizedSeries = cleanSeriesName.normalizedForComparison()
    val normalizedTitle = rawTitle.normalizedForComparison()
    if (!normalizedTitle.startsWith(normalizedSeries)) return rawTitle

    return rawTitle
        .replace(
            Regex("^\\Q$cleanSeriesName\\E\\s*[-:|.]?\\s*", RegexOption.IGNORE_CASE),
            ""
        )
        .trim()
        .ifBlank { rawTitle }
}

private fun stripEpisodeNumberPrefixFromTitle(
    episodeTitle: String,
    seasonNumber: Int?,
    episodeNumber: Int?
): String {
    var cleanedTitle = episodeTitle.trim()
    if (cleanedTitle.isBlank()) return ""

    buildEpisodePrefixRegexes(seasonNumber, episodeNumber).forEach { regex ->
        val updatedTitle = cleanedTitle.replaceFirst(regex, "").trim()
        if (updatedTitle != cleanedTitle) {
            cleanedTitle = updatedTitle
        }
    }

    return cleanedTitle
}

fun buildEpisodeDisplayTitle(
    episode: SeriesEpisodeDto,
    seriesName: String? = null,
    preferredTitle: String? = null
): String {
    val seasonNumber = episode.season
    val episodeNumber = episode.episodeNum
    val season = seasonNumber?.toString()?.padStart(2, '0')
    val number = episodeNumber?.toString()?.padStart(2, '0')
    val rawTitle = preferredTitle?.takeIf { it.isNotBlank() } ?: episode.title
    val titleWithoutSeries = stripSeriesNameFromEpisodeTitle(seriesName, rawTitle)
    val titleWithoutSeriesOrPrefix = stripEpisodeNumberPrefixFromTitle(
        episodeTitle = titleWithoutSeries,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber
    )
    val fallbackTitle = episode.episodeNum?.let { "Episodio $it" } ?: "Episodio"

    val code = when {
        season != null && number != null -> "S${season}E${number}"
        number != null -> "E${number}"
        else -> null
    }

    val resolvedTitle = titleWithoutSeriesOrPrefix.ifBlank { fallbackTitle }

    return if (code != null) "$code - $resolvedTitle" else resolvedTitle
}
