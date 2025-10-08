package com.example.datn.data.mapper

import com.example.datn.data.local.entities.TestOptionEntity
import com.example.datn.domain.models.TestOption

fun TestOptionEntity.toDomain(): TestOption = TestOption(
    id = id,
    testQuestionId = testQuestionId,
    content = content,
    isCorrect = isCorrect,
    order = order,
    mediaUrl = mediaUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TestOption.toEntity(): TestOptionEntity = TestOptionEntity(
    id = id,
    testQuestionId = testQuestionId,
    content = content,
    isCorrect = isCorrect,
    order = order,
    mediaUrl = mediaUrl,
    createdAt = createdAt,
    updatedAt = updatedAt
)