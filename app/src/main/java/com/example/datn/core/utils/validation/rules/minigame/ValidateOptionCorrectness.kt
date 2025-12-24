package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator
import com.example.datn.domain.models.QuestionType

class ValidateOptionCorrectness : Validator<Pair<QuestionType?, Boolean>> {
    override fun validate(input: Pair<QuestionType?, Boolean>): ValidationResult {
        val (questionType, isCorrect) = input
        if (!isCorrect) return ValidationResult(true)

        return when (questionType) {
            QuestionType.ESSAY -> ValidationResult(false, "Câu hỏi tự luận không cần đánh dấu đáp án đúng")
            else -> ValidationResult(true)
        }
    }
}
