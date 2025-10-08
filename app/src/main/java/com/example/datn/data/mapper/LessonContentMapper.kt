package com.example.datn.data.mapper

import com.example.datn.data.local.entities.LessonContentEntity
import com.example.datn.domain.models.LessonContent

fun LessonContentEntity.toDomain(): LessonContent = LessonContent(
    id = id,
    lessonId = lessonId,
    title = title,
    contentType = contentType,
    content = content,
    order = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun LessonContent.toEntity(): LessonContentEntity = LessonContentEntity(
    id = id,
    lessonId = lessonId,
    title = title,
    contentType = contentType,
    content = content,
    order = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)