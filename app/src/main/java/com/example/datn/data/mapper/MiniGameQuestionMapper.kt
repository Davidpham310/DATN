package com.example.datn.data.mapper

import com.example.datn.data.local.entities.MiniGameQuestionEntity
import com.example.datn.domain.models.MiniGameQuestion

fun MiniGameQuestionEntity.toDomain(): MiniGameQuestion = MiniGameQuestion(
    id = id,
    miniGameId = miniGameId,
    content = content,
    questionType = questionType,
    score = score,
    timeLimit = timeLimit,
    order = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MiniGameQuestion.toEntity(): MiniGameQuestionEntity = MiniGameQuestionEntity(
    id = id,
    miniGameId = miniGameId,
    content = content,
    questionType = questionType,
    score = score,
    timeLimit = timeLimit,
    order = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)
