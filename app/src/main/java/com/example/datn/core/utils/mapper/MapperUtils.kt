package com.example.datn.core.utils.mapper

import com.google.firebase.firestore.DocumentSnapshot
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.InternalSerializationApi
import java.time.Instant

// ==========================================================
// 1. JSON CONFIGURATION
// ==========================================================

/**
 * Internal Json instance for Kotlinx Serialization.
 * Configured to be lenient and ignore unknown keys.
 */
internal val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/**
 * Internal Jackson ObjectMapper for bridging Map <-> JSON.
 */
internal val JacksonMapper = ObjectMapper().apply {
    registerKotlinModule()
}

// ==========================================================
// 2. INSTANT SERIALIZER (KSerializer cho java.time.Instant)
// ==========================================================
internal object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Instant {
        val milliseconds = decoder.decodeLong()
        return Instant.ofEpochMilli(milliseconds)
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilli())
    }
}

// ==========================================================
// 3. FIRESTORE EXTENSION FUNCTIONS (Map <-> Domain Model)
// ==========================================================

/**
 * ✅ [FROM DocumentSnapshot TO Domain Model]
 * Chuyển đổi Firestore DocumentSnapshot thành Domain Model (T).
 */
@OptIn(InternalSerializationApi::class)
internal inline fun <reified T : Any> DocumentSnapshot.toDomain(): T {
    val dataMap = this.data ?: throw IllegalStateException(
        "Document snapshot data is null or empty."
    )

    return try {
        // 1. Jackson: Map<String, Any?> -> JSON String
        val jsonString = JacksonMapper.writeValueAsString(dataMap)

        // 2. Kotlinx: JSON String -> Domain Model T
        AppJson.decodeFromString<T>(jsonString)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to deserialize snapshot to ${T::class.simpleName}.", e)
    }
}

/**
 * ✅ [FROM Domain Model TO Firestore Map]
 * Chuyển đổi Domain Model (T) thành Map<String, Any?> cho Firestore.
 */
@OptIn(InternalSerializationApi::class)
internal inline fun <reified T : Any> T.toFirestoreMap(): Map<String, Any?> {
    return try {
        // 1. Kotlinx: Domain Model T -> JSON String
        val jsonString = AppJson.encodeToString(this)

        // 2. Jackson: JSON String -> Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        JacksonMapper.readValue(jsonString, Map::class.java) as Map<String, Any?>
    } catch (e: Exception) {
        throw IllegalStateException("Failed to serialize ${T::class.simpleName} to Firestore map.", e)
    }
}

// ==========================================================
// 4. ROOM EXTENSION FUNCTION (Domain <-> Entity)
// ==========================================================

/**
 * ✅ [Domain <-> Room Entity]
 * Chuyển đổi giữa hai đối tượng Kotlin có cấu trúc trường giống nhau (dùng JSON bridge).
 */
@OptIn(InternalSerializationApi::class)
internal inline fun <reified T : Any> Any.toModel(): T {
    val jsonString = AppJson.encodeToString(this)
    return AppJson.decodeFromString(jsonString)
}
