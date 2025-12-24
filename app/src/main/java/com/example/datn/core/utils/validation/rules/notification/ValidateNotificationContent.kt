package com.example.datn.core.utils.validation.rules.notification

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateNotificationContent : Validator<String> {
    override fun validate(input: String): ValidationResult {
        val content = input.trim()

        if (content.isBlank()) {
            return ValidationResult(false, "Nội dung thông báo không được để trống")
        }

        if (content.length > 500) {
            return ValidationResult(false, "Nội dung văn bản không được vượt quá 500 ký tự")
        }

        return ValidationResult(true)
    }
}
