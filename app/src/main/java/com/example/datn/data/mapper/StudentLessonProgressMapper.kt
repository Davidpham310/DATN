package com.example.datn.data.mapper

import com.example.datn.data.local.entities.StudentLessonProgressEntity
import com.example.datn.domain.models.StudentLessonProgress

fun StudentLessonProgressEntity.toDomain(): StudentLessonProgress = StudentLessonProgress(
    id = id,
    studentId = studentId,
    lessonId = lessonId,
    progressPercentage = progressPercentage,
    lastAccessedContentId = lastAccessedContentId,
    lastAccessedAt = lastAccessedAt,
    isCompleted = isCompleted,
    timeSpentSeconds = timeSpentSeconds,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun StudentLessonProgress.toEntity(): StudentLessonProgressEntity = StudentLessonProgressEntity(
    id = id,
    studentId = studentId,
    lessonId = lessonId,
    progressPercentage = progressPercentage,
    lastAccessedContentId = lastAccessedContentId,
    lastAccessedAt = lastAccessedAt,
    isCompleted = isCompleted,
    timeSpentSeconds = timeSpentSeconds,
    createdAt = createdAt,
    updatedAt = updatedAt
)