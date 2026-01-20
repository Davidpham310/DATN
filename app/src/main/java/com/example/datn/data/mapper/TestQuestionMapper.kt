package com.example.datn.data.mapper

import com.example.datn.data.local.entities.TestQuestionEntity
import com.example.datn.domain.models.TestQuestion

fun TestQuestionEntity.toDomain(): TestQuestion = TestQuestion(
    id = id,
    testId = testId,
    content = content,
    score = score,
    questionType = questionType,
    mediaUrl = mediaUrl,
    timeLimit = timeLimit,
    order = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TestQuestion.toEntity(): TestQuestionEntity = TestQuestionEntity(
    id = id,
    testId = testId,
    content = content,
    score = score,
    questionType = questionType,
    mediaUrl = mediaUrl,
    timeLimit = timeLimit,
    order = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)