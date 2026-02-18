package com.stream.iptvrevolut.data.remote.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.stream.iptvrevolut.data.remote.SeriesEpisodeDto
import java.lang.reflect.Type

class SeriesEpisodesDeserializer : JsonDeserializer<Map<String, List<SeriesEpisodeDto>>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Map<String, List<SeriesEpisodeDto>> {
        if (json.isJsonArray) {
            // Si el servidor manda [] en lugar de {}, retornamos mapa vacío
            return emptyMap()
        }
        
        val result = mutableMapOf<String, List<SeriesEpisodeDto>>()
        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            jsonObject.entrySet().forEach { entry ->
                val seasonNumber = entry.key
                val episodesArray = entry.value
                if (episodesArray.isJsonArray) {
                    val episodesList = context.deserialize<List<SeriesEpisodeDto>>(
                        episodesArray,
                        object : com.google.gson.reflect.TypeToken<List<SeriesEpisodeDto>>() {}.type
                    )
                    result[seasonNumber] = episodesList
                }
            }
        }
        return result
    }
}
