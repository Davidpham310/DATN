package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateClassName : Validator<String> {
    override fun validate(value: String): ValidationResult {
        if (value.isBlank()) {
            return ValidationResult(false, "Tên lớp không được để trống")
        }
        val trimmed = value.trim()
        if (trimmed.length < 3 || trimmed.length > 50) {
            return ValidationResult(false, "Tên lớp phải từ 3–50 ký tự")
        }
        if (trimmed.any { c -> !(c.isLetterOrDigit() || c.isWhitespace()) }) {
            return ValidationResult(false, "Tên lớp không được chứa ký tự đặc biệt")
        }
        if (!trimmed.any { it.isLetterOrDigit() }) {
            return ValidationResult(false, "Tên lớp không hợp lệ")
        }
        return ValidationResult(true)
    }
}