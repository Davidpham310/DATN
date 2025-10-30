package com.example.datn.data.mapper

import com.example.datn.data.local.entities.MiniGameOptionEntity
import com.example.datn.domain.models.MiniGameOption

fun MiniGameOptionEntity.toDomain(): MiniGameOption = MiniGameOption(
    id = id,
    miniGameQuestionId = miniGameQuestionId,
    content = content,
    isCorrect = isCorrect,
    order = order,
    mediaUrl = mediaUrl,
    hint = hint,
    pairId = pairId,
    pairContent = pairContent,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MiniGameOption.toEntity(): MiniGameOptionEntity = MiniGameOptionEntity(
    id = id,
    miniGameQuestionId = miniGameQuestionId,
    content = content,
    isCorrect = isCorrect,
    order = order,
    mediaUrl = mediaUrl,
    hint = hint,
    pairId = pairId,
    pairContent = pairContent,
    createdAt = createdAt,
    updatedAt = updatedAt
)