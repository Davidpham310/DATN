package com.example.datn.data.mapper

import com.example.datn.data.local.entities.MiniGameEntity
import com.example.datn.domain.models.MiniGame

fun MiniGameEntity.toDomain(): MiniGame = MiniGame(
    id = id,
    teacherId = teacherId,
    lessonId = lessonId,
    title = title,
    description = description,
    gameType = gameType,
    contentUrl = contentUrl,
    level = level,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MiniGame.toEntity(): MiniGameEntity = MiniGameEntity(
    id = id,
    teacherId = teacherId,
    lessonId = lessonId,
    title = title,
    description = description,
    gameType = gameType,
    contentUrl = contentUrl,
    level = level,
    createdAt = createdAt,
    updatedAt = updatedAt
)