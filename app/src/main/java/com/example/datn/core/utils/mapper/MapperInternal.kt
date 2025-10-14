package com.example.datn.core.utils.mapper

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
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

// ==========================================================
// 2️⃣ Jackson Mapper với Firestore Instant Support
// ==========================================================
@PublishedApi
internal val JacksonMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())

    // Module custom để parse Firestore Instant {epochSecond, nano}
    val instantModule = SimpleModule().apply {
        addDeserializer(Instant::class.java, object : JsonDeserializer<Instant>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant {
                val node = p.codec.readTree<JsonNode>(p)
                val epochSecondNode = node.get("epochSecond")
                val nanoNode = node.get("nano")
                return if (epochSecondNode != null && nanoNode != null) {
                    Instant.ofEpochSecond(epochSecondNode.asLong(), nanoNode.asLong())
                } else {
                    throw IllegalStateException("Invalid Firestore Instant format: $node")
                }
            }
        })
    }
    registerModule(instantModule)
}

// ==========================================================
// 3️⃣ FIRESTORE CONVERSION HELPERS
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
    // Chuyển dữ liệu sang Map trước
    val jsonString = try {
        AppJson.encodeToString(AppJson.serializersModule.serializer(clazz), data)
    } catch (_: Exception) {
        JacksonMapper.writeValueAsString(data)
    }

    val rawMap: Map<String, Any?> = JacksonMapper.readValue(jsonString, Map::class.java) as Map<String, Any?>

    // Chuyển các trường Instant sang {epochSecond, nano}
    return rawMap.mapValues { entry ->
        when (val value = entry.value) {
            is Instant -> mapOf(
                "epochSecond" to value.epochSecond,
                "nano" to value.nano
            )
            is Map<*, *> -> value // giữ nguyên map con
            is List<*> -> value.map { item ->
                if (item is Instant) mapOf(
                    "epochSecond" to item.epochSecond,
                    "nano" to item.nano
                ) else item
            }
            else -> value
        }
    }
}

