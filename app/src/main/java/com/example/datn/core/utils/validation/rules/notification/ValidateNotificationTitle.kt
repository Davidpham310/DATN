package com.example.datn.core.utils.validation.rules.notification

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateNotificationTitle : Validator<String> {
    override fun validate(input: String): ValidationResult {
        val title = input.trim()

        if (title.isBlank()) {
            return ValidationResult(false, "Tiêu đề không được để trống")
        }

        if (title.length < 3) {
            return ValidationResult(false, "Tiêu đề phải có ít nhất 3 ký tự")
        }

        if (title.length > 100) {
            return ValidationResult(false, "Tiêu đề tối đa 100 ký tự")
        }

        val isValidCharacters = title.matches(Regex("^[\\p{L}\\p{N}\\s]+$"))
        if (!isValidCharacters) {
            return ValidationResult(false, "Tiêu đề không được chứa ký tự đặc biệt")
        }

        return ValidationResult(true)
    }
}
