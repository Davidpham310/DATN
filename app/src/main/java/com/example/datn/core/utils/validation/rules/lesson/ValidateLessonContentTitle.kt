package com.example.datn.core.utils.validation.rules.lesson

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateLessonContentTitle : Validator<String> {
    override fun validate(value: String): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult(false, "Tiêu đề không được để trống")
        } else {
            ValidationResult(true)
        }
    }
}
