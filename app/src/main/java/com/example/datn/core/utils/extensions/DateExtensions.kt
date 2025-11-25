package com.example.datn.core.utils.extensions

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Instant.formatAsDateTime(pattern: String = "dd/MM/yyyy HH:mm"): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern(pattern)
            .withZone(ZoneId.systemDefault())
        formatter.format(this)
    } catch (e: Exception) {
        "N/A"
    }
}

fun Instant.formatAsDate(pattern: String = "dd/MM/yyyy"): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern(pattern)
            .withZone(ZoneId.systemDefault())
        formatter.format(this)
    } catch (e: Exception) {
        "N/A"
    }
}

fun LocalDate.formatAsDate(pattern: String = "dd/MM/yyyy"): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        this.format(formatter)
    } catch (e: Exception) {
        "N/A"
    }
}