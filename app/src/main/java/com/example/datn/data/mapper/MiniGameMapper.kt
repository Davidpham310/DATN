package com.example.datn.data.mapper

import com.example.datn.data.local.entities.MiniGameEntity
import com.example.datn.domain.models.MiniGame

fun MiniGameEntity.toDomain(): MiniGame = MiniGame(
    id = id,
    teacherId = teacherId,
    lessonId = lessonId,
    title = title,
    description = description,
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
    contentUrl = null,
    level = level,
    createdAt = createdAt,
    updatedAt = updatedAt
)