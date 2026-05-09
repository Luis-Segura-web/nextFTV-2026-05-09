package com.stream.nextftv.data.remote.tmdb

val TMDB_LANGUAGE_PRIORITY: List<String> = listOf("es-MX", "es-ES", "en-US")

private suspend inline fun <T> firstLocalizedResult(
    crossinline request: suspend (String) -> T,
    crossinline isGoodResult: (T) -> Boolean
): T {
    var firstSuccess: T? = null
    var lastError: Throwable? = null

    TMDB_LANGUAGE_PRIORITY.forEach { language ->
        try {
            val result = request(language)
            if (firstSuccess == null) {
                firstSuccess = result
            }
            if (isGoodResult(result)) {
                return result
            }
        } catch (error: Throwable) {
            lastError = error
        }
    }

    return firstSuccess ?: throw (lastError ?: IllegalStateException("TMDB request failed"))
}

private fun TmdbMovieResponseDto.hasAnyTitle(): Boolean =
    results.orEmpty().any { !it.title.isNullOrBlank() || !it.tvName.isNullOrBlank() }

private fun TmdbMovieDetailsDto.hasLocalizedContent(): Boolean =
    !overview.isNullOrBlank() ||
        recommendations?.hasAnyTitle() == true ||
        similar?.hasAnyTitle() == true

private fun TmdbPersonDetailsDto.hasLocalizedContent(): Boolean =
    !biography.isNullOrBlank() ||
        !alsoKnownAs.isNullOrEmpty() ||
        combinedCredits?.cast?.isNotEmpty() == true

private fun TmdbSeasonDetailsDto.hasLocalizedContent(): Boolean =
    episodes.orEmpty().any { !it.name.isNullOrBlank() }

private fun TmdbCollectionDto.hasLocalizedContent(): Boolean =
    parts.orEmpty().any { !it.title.isNullOrBlank() || !it.tvName.isNullOrBlank() }

suspend fun TmdbApiService.searchMovieWithFallback(query: String): TmdbSearchResponseDto =
    firstLocalizedResult(
        request = { language -> searchMovie(query = query, language = language) },
        isGoodResult = { response -> response.results.isNotEmpty() }
    )

suspend fun TmdbApiService.searchPersonWithFallback(query: String): TmdbPersonSearchResponseDto =
    firstLocalizedResult(
        request = { language -> searchPerson(query = query, language = language) },
        isGoodResult = { response -> response.results.isNotEmpty() }
    )

suspend fun TmdbApiService.discoverMoviesByActorWithFallback(personId: Int): TmdbMovieResponseDto =
    firstLocalizedResult(
        request = { language -> discoverMoviesByActor(personId = personId, language = language) },
        isGoodResult = { response -> response.results.orEmpty().isNotEmpty() }
    )

suspend fun TmdbApiService.getPersonDetailsWithFallback(personId: Int): TmdbPersonDetailsDto =
    firstLocalizedResult(
        request = { language -> getPersonDetails(personId = personId, language = language) },
        isGoodResult = { details -> details.hasLocalizedContent() }
    )

suspend fun TmdbApiService.getMovieDetailsWithFallback(movieId: Int): TmdbMovieDetailsDto =
    firstLocalizedResult(
        request = { language -> getMovieDetails(movieId = movieId, language = language) },
        isGoodResult = { details -> details.hasLocalizedContent() }
    )

suspend fun TmdbApiService.searchTvWithFallback(query: String): TmdbSearchResponseDto =
    firstLocalizedResult(
        request = { language -> searchTv(query = query, language = language) },
        isGoodResult = { response -> response.results.isNotEmpty() }
    )

suspend fun TmdbApiService.getTvDetailsWithFallback(tvId: Int): TmdbMovieDetailsDto =
    firstLocalizedResult(
        request = { language -> getTvDetails(tvId = tvId, language = language) },
        isGoodResult = { details -> details.hasLocalizedContent() }
    )

suspend fun TmdbApiService.getTvSeasonDetailsWithFallback(tvId: Int, seasonNumber: Int): TmdbSeasonDetailsDto =
    firstLocalizedResult(
        request = { language -> getTvSeasonDetails(tvId = tvId, seasonNumber = seasonNumber, language = language) },
        isGoodResult = { details -> details.hasLocalizedContent() }
    )

suspend fun TmdbApiService.getCollectionWithFallback(collectionId: Int): TmdbCollectionDto =
    firstLocalizedResult(
        request = { language -> getCollection(collectionId = collectionId, language = language) },
        isGoodResult = { details -> details.hasLocalizedContent() }
    )
