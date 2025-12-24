package com.example.datn.core.utils.validation.rules.test

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestOption

class ValidateTestCorrectOptions : Validator<Pair<QuestionType?, List<TestOption>>> {
    override fun validate(input: Pair<QuestionType?, List<TestOption>>): ValidationResult {
        val (questionType, options) = input

        val correctCount = options.count { it.isCorrect }

        return when (questionType) {
            null -> ValidationResult(false, "Không xác định được loại câu hỏi")

            QuestionType.SINGLE_CHOICE -> {
                when {
                    correctCount == 0 -> ValidationResult(false, "Câu hỏi phải có ít nhất một đáp án đúng")
                    correctCount > 1 -> ValidationResult(false, "Câu hỏi chỉ có một đáp án đúng")
                    else -> ValidationResult(true)
                }
            }

            QuestionType.MULTIPLE_CHOICE -> {
                if (correctCount == 0) {
                    ValidationResult(false, "Câu hỏi phải có ít nhất một đáp án đúng")
                } else {
                    ValidationResult(true)
                }
            }

            QuestionType.FILL_BLANK,
            QuestionType.ESSAY -> ValidationResult(true)
        }
    }
}
