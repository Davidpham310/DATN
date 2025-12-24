package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.QuestionType

class ValidateMiniGameCorrectOptions : Validator<Pair<QuestionType?, List<MiniGameOption>>> {
    override fun validate(input: Pair<QuestionType?, List<MiniGameOption>>): ValidationResult {
        val (questionType, options) = input

        val correctCount = options.count { it.isCorrect }

        return when (questionType) {
            null -> ValidationResult(false, "Không xác định được loại câu hỏi")

            QuestionType.SINGLE_CHOICE -> {
                when {
                    correctCount == 0 -> ValidationResult(false, "Câu hỏi phải có ít nhất một đáp án đúng")
                    correctCount > 1 -> ValidationResult(false, "Câu hỏi một lựa chọn chỉ được có một đáp án đúng")
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
