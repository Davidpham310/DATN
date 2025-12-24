package com.example.datn.core.utils.validation.rules.test

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateTotalScore : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val score = value.trim().toDoubleOrNull()
        return when {
            score == null -> ValidationResult(false, "Tổng điểm phải là số")
            score.isNaN() || score.isInfinite() -> ValidationResult(false, "Tổng điểm không hợp lệ")
            score <= 0 -> ValidationResult(false, "Tổng điểm phải lớn hơn 0")
            else -> ValidationResult(true)
        }
    }
}
