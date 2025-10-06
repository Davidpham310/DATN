package com.example.datn.data.mapper

import com.example.datn.data.local.entities.TestResultEntity
import com.example.datn.domain.models.TestResult

fun TestResultEntity.toDomain(): TestResult {
    return TestResult(
        id = id,
        testId = testId,
        studentId = studentId,
        score = score,
        submittedAt = submittedAt,
        feedback = feedback
    )
}

fun TestResult.toEntity(): TestResultEntity {
    return TestResultEntity(
        id = id,
        testId = testId,
        studentId = studentId,
        score = score,
        submittedAt = submittedAt,
        feedback = feedback
    )
}
