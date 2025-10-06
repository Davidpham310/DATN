package com.example.datn.data.local.converters

import androidx.room.TypeConverter

class StringListConverter {
    @TypeConverter
    fun fromList(list: List<String>?): String? = list?.joinToString(separator = "||")

    @TypeConverter
    fun toList(data: String?): List<String> =
        data?.split("||")?.map { it } ?: emptyList()
}
