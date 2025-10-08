package com.example.datn.data.mapper

import com.example.datn.data.local.entities.TestEntity
import com.example.datn.domain.models.Test

fun TestEntity.toDomain(): Test = Test(
    id = id,
    classId = classId,
    lessonId = lessonId,
    title = title,
    description = description,
    totalScore = totalScore,
    startTime = startTime,
    endTime = endTime,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Test.toEntity(): TestEntity = TestEntity(
    id = id,
    classId = classId,
    lessonId = lessonId,
    title = title,
    description = description,
    totalScore = totalScore,
    startTime = startTime,
    endTime = endTime,
    createdAt = createdAt,
    updatedAt = updatedAt
)