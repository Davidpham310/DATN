package com.example.datn.data.mapper

import com.example.datn.data.local.entities.QuestionEntity
import com.example.datn.domain.models.Question

fun QuestionEntity.toDomain(): Question {
    return Question(
        id = id,
        testId = testId,
        questionText = questionText,
        options = options,
        correctAnswer = correctAnswer,
        type = type
    )
}

fun Question.toEntity(): QuestionEntity {
    return QuestionEntity(
        id = id,
        testId = testId,
        questionText = questionText,
        options = options,
        correctAnswer = correctAnswer,
        type = type
    )
}
