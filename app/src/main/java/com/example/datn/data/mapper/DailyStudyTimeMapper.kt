package com.example.datn.data.mapper

import com.example.datn.data.local.entities.DailyStudyTimeEntity
import com.example.datn.domain.models.DailyStudyTime

fun DailyStudyTimeEntity.toDomain(): DailyStudyTime = DailyStudyTime(
    id = id,
    studentId = studentId,
    date = date,
    durationSeconds = durationSeconds,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DailyStudyTime.toEntity(): DailyStudyTimeEntity = DailyStudyTimeEntity(
    id = id,
    studentId = studentId,
    date = date,
    durationSeconds = durationSeconds,
    createdAt = createdAt,
    updatedAt = updatedAt
)