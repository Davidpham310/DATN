package com.example.datn.core.utils.mapper

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.math.floor
import kotlin.math.roundToLong

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
    // Ignore unknown properties khi deserialize từ Firestore (ví dụ: field "phone" không có trong User model)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    // Module custom để parse Firestore Instant {epochSecond, nano} hoặc milliseconds (number)
    val instantModule = SimpleModule().apply {
        addDeserializer(Instant::class.java, object : JsonDeserializer<Instant>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Instant {
                val node = p.codec.readTree<JsonNode>(p)
                
                // Nếu là number:
                // - Một số màn hình/clients lưu epochSeconds (có thể có phần thập phân)
                // - Một số lưu epochMillis
                if (node.isNumber) {
                    val raw = node.asDouble()
                    // Heuristic: < 1e12 => epochSeconds, >= 1e12 => epochMillis
                    return if (raw < 1_000_000_000_000.0) {
                        val seconds = floor(raw).toLong()
                        val nanos = ((raw - seconds) * 1_000_000_000.0).roundToLong().coerceIn(0, 999_999_999)
                        Instant.ofEpochSecond(seconds, nanos)
                    } else {
                        Instant.ofEpochMilli(raw.roundToLong())
                    }
                }
                
                // Nếu là object với {epochSecond, nano} (Firestore Instant format)
                val epochSecondNode = node.get("epochSecond")
                val nanoNode = node.get("nano")
                if (epochSecondNode != null && nanoNode != null) {
                    return Instant.ofEpochSecond(epochSecondNode.asLong(), nanoNode.asLong())
                }
                
                // Fallback: thử parse như string hoặc number
                if (node.isTextual) {
                    return try {
                        Instant.ofEpochMilli(node.asText().toLong())
                    } catch (e: Exception) {
                        Instant.parse(node.asText())
                    }
                }
                
                throw IllegalStateException("Invalid Firestore Instant format: $node")
            }
        })

        addDeserializer(LocalDate::class.java, object : JsonDeserializer<LocalDate>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate {
                val node = p.codec.readTree<JsonNode>(p)

                // Firestore có thể lưu LocalDate dạng array [year, month, day]
                if (node.isArray && node.size() >= 3) {
                    val year = node.get(0).asInt()
                    val month = node.get(1).asInt()
                    val day = node.get(2).asInt()
                    return LocalDate.of(year, month, day)
                }

                if (node.isTextual) {
                    return LocalDate.parse(node.asText())
                }

                throw IllegalStateException("Invalid Firestore LocalDate format: $node")
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
    
    // Ensure 'id' field matches document ID (for backward compatibility and safety)
    val enrichedDataMap = dataMap.toMutableMap().apply {
        val existingId = this["id"] as? String
        if (existingId.isNullOrEmpty()) {
            this["id"] = this@internalToDomain.id
        }
    }
    
    // Log dữ liệu thô từ Firestore
    android.util.Log.d("MapperInternal", "Converting document ${id} to ${clazz.simpleName}")
    android.util.Log.d("MapperInternal", "Raw data from Firestore: $enrichedDataMap")
    
    val jsonString = JacksonMapper.writeValueAsString(enrichedDataMap)
    android.util.Log.d("MapperInternal", "JSON string: $jsonString")

    return try {
        val result = JacksonMapper.readValue(jsonString, clazz)
        android.util.Log.d("MapperInternal", "Successfully mapped to ${clazz.simpleName}: $result")
        result
    } catch (e: Exception) {
        android.util.Log.e("MapperInternal", "Failed to parse document to ${clazz.simpleName}", e)
        android.util.Log.e("MapperInternal", "JSON string was: $jsonString")
        android.util.Log.e("MapperInternal", "Data map was: $enrichedDataMap")
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

