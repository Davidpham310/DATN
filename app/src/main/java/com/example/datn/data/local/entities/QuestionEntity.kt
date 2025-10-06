package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.datn.data.local.converters.StringListConverter

@Entity(tableName = "questions")
@TypeConverters(StringListConverter::class)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val questionText: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String? = null,
    val type: String
)
