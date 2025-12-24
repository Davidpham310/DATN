package com.example.datn.core.utils.validation.rules.lesson

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateLessonDescription : Validator<String?> {
    override fun validate(value: String?): ValidationResult {
        val trimmed = value?.trim().orEmpty()

        if (trimmed.isBlank()) {
            return ValidationResult(true)
        }

        return when {
            trimmed.length > 500 -> ValidationResult(false, "Mô tả tối đa 500 ký tự")
            !trimmed.any { it.isLetterOrDigit() } -> ValidationResult(false, "Mô tả phải có ít nhất 1 chữ hoặc số")
            else -> ValidationResult(true)
        }
    }
}
