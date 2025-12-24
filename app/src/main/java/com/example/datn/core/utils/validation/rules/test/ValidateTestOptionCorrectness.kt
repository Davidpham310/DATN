package com.example.datn.core.utils.validation.rules.test

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator
import com.example.datn.domain.models.QuestionType

class ValidateTestOptionCorrectness : Validator<Pair<QuestionType?, Boolean>> {
    override fun validate(input: Pair<QuestionType?, Boolean>): ValidationResult {
        val (questionType, isCorrect) = input

        return when (questionType) {
            null -> ValidationResult(false, "Không xác định được loại câu hỏi")
            QuestionType.ESSAY -> ValidationResult(false, "Câu hỏi tự luận không cần đáp án")
            QuestionType.FILL_BLANK -> if (!isCorrect) {
                ValidationResult(false, "Câu hỏi điền vào chỗ trống cần đáp án đúng")
            } else {
                ValidationResult(true)
            }
            else -> ValidationResult(true)
        }
    }
}
