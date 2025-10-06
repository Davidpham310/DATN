package com.example.datn.data.mapper

import com.example.datn.data.local.entities.LessonEntity
import com.example.datn.domain.models.Lesson

fun LessonEntity.toDomain(): Lesson {
    return Lesson(
        id = id,
        classId = classId,
        title = title,
        description = description,
        videoUrl = videoUrl,
        documentUrl = documentUrl,
        isPublished = isPublished,
        orderIndex = orderIndex,
        createdAt = createdAt
    )
}

fun Lesson.toEntity(): LessonEntity {
    return LessonEntity(
        id = id,
        classId = classId,
        title = title,
        description = description,
        videoUrl = videoUrl,
        documentUrl = documentUrl,
        isPublished = isPublished,
        orderIndex = orderIndex,
        createdAt = createdAt
    )
}
