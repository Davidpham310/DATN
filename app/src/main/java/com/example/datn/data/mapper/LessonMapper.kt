package com.example.datn.data.mapper

import com.example.datn.data.local.entities.LessonEntity
import com.example.datn.domain.models.Lesson

fun LessonEntity.toDomain(): Lesson = Lesson(
    id = id,
    teacherId = teacherId,
    classId = classId,
    title = title,
    description = description,
    contentLink = contentLink,
    order = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Lesson.toEntity(): LessonEntity = LessonEntity(
    id = id,
    teacherId = teacherId,
    classId = classId,
    title = title,
    description = description,
    contentLink = contentLink,
    order = order,
    createdAt = createdAt,
    updatedAt = updatedAt
)