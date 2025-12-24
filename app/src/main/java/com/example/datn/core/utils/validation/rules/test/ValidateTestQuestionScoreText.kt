package com.example.datn.core.utils.validation.rules.test

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateTestQuestionScoreText : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val score = value.trim().toDoubleOrNull()
        return when {
            score == null -> ValidationResult(false, "Điểm số phải là số")
            score.isNaN() || score.isInfinite() -> ValidationResult(false, "Điểm số không hợp lệ")
            score <= 0.0 -> ValidationResult(false, "Điểm số phải lớn hơn 0.0")
            score > 100.0 -> ValidationResult(false, "Điểm số không được vượt quá 100")
            else -> ValidationResult(true)
        }
    }
}
