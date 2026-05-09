package com.stream.nextftv.data.remote.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

/**
 * Reglas globales de parseo obligatorias:
 * - Tipos mixtos (String/Number/Boolean)
 * - Nulos ("null", "N/A", "") -> null
 * - Booleanos (1/0, "true"/"false")
 */

class SafeIntAdapter : TypeAdapter<Int>() {
    override fun write(out: JsonWriter, value: Int?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): Int? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return try {
            when (reader.peek()) {
                JsonToken.NUMBER -> reader.nextInt()
                JsonToken.STRING -> {
                    val str = reader.nextString()
                    if (str.isEmpty() || str.equals("null", true) || str.equals("N/A", true)) null
                    else str.toDoubleOrNull()?.toInt() // Handle "123.0" as 123
                }
                JsonToken.BOOLEAN -> if (reader.nextBoolean()) 1 else 0
                else -> {
                    reader.skipValue()
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

class SafeLongAdapter : TypeAdapter<Long>() {
    override fun write(out: JsonWriter, value: Long?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): Long? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return try {
            when (reader.peek()) {
                JsonToken.NUMBER -> reader.nextLong()
                JsonToken.STRING -> {
                    val str = reader.nextString()
                    if (str.isEmpty() || str.equals("null", true) || str.equals("N/A", true)) null
                    else str.toDoubleOrNull()?.toLong()
                }
                JsonToken.BOOLEAN -> if (reader.nextBoolean()) 1L else 0L
                else -> {
                    reader.skipValue()
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

class SafeDoubleAdapter : TypeAdapter<Double>() {
    override fun write(out: JsonWriter, value: Double?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): Double? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return try {
            when (reader.peek()) {
                JsonToken.NUMBER -> reader.nextDouble()
                JsonToken.STRING -> {
                    val str = reader.nextString()
                    if (str.isEmpty() || str.equals("null", true) || str.equals("N/A", true)) null
                    else str.toDoubleOrNull()
                }
                else -> {
                    reader.skipValue()
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

class SafeBooleanAdapter : TypeAdapter<Boolean>() {
    override fun write(out: JsonWriter, value: Boolean?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): Boolean? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return try {
            when (reader.peek()) {
                JsonToken.BOOLEAN -> reader.nextBoolean()
                JsonToken.NUMBER -> reader.nextInt() != 0
                JsonToken.STRING -> {
                    val str = reader.nextString()
                    str.equals("1", true) || str.equals("true", true) || str.equals("yes", true)
                }
                else -> {
                    reader.skipValue()
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

class SafeStringAdapter : TypeAdapter<String>() {
    override fun write(out: JsonWriter, value: String?) {
        out.value(value)
    }

    override fun read(reader: JsonReader): String? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return when (reader.peek()) {
            JsonToken.STRING -> {
                val str = reader.nextString()
                if (str.equals("null", true) || str.equals("N/A", true)) null else str
            }
            JsonToken.NUMBER -> reader.nextString() // Convert number to string
            JsonToken.BOOLEAN -> reader.nextBoolean().toString()
            else -> {
                reader.skipValue()
                null
            }
        }
    }
}

class SafeListStringAdapter : TypeAdapter<List<String>>() {
    override fun write(out: JsonWriter, value: List<String>?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.beginArray()
            value.forEach { out.value(it) }
            out.endArray()
        }
    }

    override fun read(reader: JsonReader): List<String>? {
        return when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                val list = mutableListOf<String>()
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() == JsonToken.STRING) {
                        list.add(reader.nextString())
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endArray()
                list
            }
            JsonToken.STRING -> {
                val str = reader.nextString()
                if (str.isNullOrBlank() || str.equals("null", true)) emptyList()
                else listOf(str)
            }
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            else -> {
                reader.skipValue()
                emptyList()
            }
        }
    }
}
