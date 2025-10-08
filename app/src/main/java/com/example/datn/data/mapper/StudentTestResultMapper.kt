package com.example.datn.data.mapper

import com.example.datn.data.local.entities.StudentTestResultEntity
import com.example.datn.domain.models.StudentTestResult

fun StudentTestResultEntity.toDomain(): StudentTestResult = StudentTestResult(
    id = id,
    studentId = studentId,
    testId = testId,
    score = score,
    completionStatus = completionStatus,
    submissionTime = submissionTime,
    durationSeconds = durationSeconds,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun StudentTestResult.toEntity(): StudentTestResultEntity = StudentTestResultEntity(
    id = id,
    studentId = studentId,
    testId = testId,
    score = score,
    completionStatus = completionStatus,
    submissionTime = submissionTime,
    durationSeconds = durationSeconds,
    createdAt = createdAt,
    updatedAt = updatedAt
)