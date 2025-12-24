package com.example.datn.core.utils.validation.rules.test

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateTestQuestionScore : Validator<Double> {
    override fun validate(input: Double): ValidationResult {
        return when {
            input.isNaN() || input.isInfinite() -> ValidationResult(false, "Điểm số không hợp lệ")
            input <= 0.0 -> ValidationResult(false, "Điểm số phải lớn hơn 0.0")
            input > 100.0 -> ValidationResult(false, "Điểm số không được vượt quá 100")
            else -> ValidationResult(true)
        }
    }
}
