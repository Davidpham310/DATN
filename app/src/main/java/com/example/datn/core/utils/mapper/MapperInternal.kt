package com.example.datn.core.utils.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

// ==========================================================
// 1️⃣ JSON ENGINES (Kotlinx + Jackson)
// ==========================================================

@PublishedApi
internal val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

@PublishedApi
internal val JacksonMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
}

// ==========================================================
// 2️⃣ FIRESTORE CONVERSION HELPERS
// ==========================================================

@PublishedApi
internal fun <T : Any> DocumentSnapshot.internalToDomain(clazz: Class<T>): T {
    val dataMap = data ?: throw IllegalStateException(
        "DocumentSnapshot data is null for ${clazz.simpleName}"
    )
    val jsonString = JacksonMapper.writeValueAsString(dataMap)

    return try {
        JacksonMapper.readValue(jsonString, clazz)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to parse document to ${clazz.simpleName}", e)
    }
}

fun <T : Any> internalToFirestoreMap(data: T, clazz: Class<T>): Map<String, Any?> {
    val jsonString = try {
        AppJson.encodeToString(AppJson.serializersModule.serializer(clazz), data)
    } catch (_: Exception) {
        JacksonMapper.writeValueAsString(data)
    }

    @Suppress("UNCHECKED_CAST")
    return JacksonMapper.readValue(jsonString, Map::class.java) as Map<String, Any?>
}
