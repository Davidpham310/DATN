package com.example.datn.data.local.converters

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class DateTimeConverter {
    // Chuyển Instant (Thời điểm chính xác) sang Long (Timestamp)
    @TypeConverter
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    // Chuyển LocalDate (Chỉ ngày tháng) sang Long (Timestamp)
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? {
        return value?.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }
}