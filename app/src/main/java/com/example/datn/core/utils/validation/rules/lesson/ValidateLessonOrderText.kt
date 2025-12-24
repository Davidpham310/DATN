package com.example.datn.core.utils.validation.rules.lesson

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateLessonOrderText : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val trimmed = value.trim()

        if (trimmed.isBlank()) {
            return ValidationResult(false, "Thứ tự không được để trống")
        }

        val order = trimmed.toIntOrNull()
            ?: return ValidationResult(false, "Thứ tự phải là số")

        return when {
            order <= 0 -> ValidationResult(false, "Thứ tự phải lớn hơn 0")
            else -> ValidationResult(true)
        }
    }
}
