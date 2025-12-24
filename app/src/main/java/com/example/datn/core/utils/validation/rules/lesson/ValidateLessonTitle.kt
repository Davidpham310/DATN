package com.example.datn.core.utils.validation.rules.lesson

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateLessonTitle : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val trimmed = value.trim()

        return when {
            trimmed.isBlank() -> ValidationResult(false, "Tiêu đề không được để trống")
            trimmed.length < 3 -> ValidationResult(false, "Tiêu đề phải có ít nhất 3 ký tự")
            trimmed.length > 100 -> ValidationResult(false, "Tiêu đề tối đa 100 ký tự")
            trimmed.any { !it.isLetterOrDigit() && !it.isWhitespace() } -> ValidationResult(false, "Tiêu đề không được chứa ký tự đặc biệt")
            !trimmed.any { it.isLetterOrDigit() } -> ValidationResult(false, "Tiêu đề phải có ít nhất 1 chữ hoặc số")
            else -> ValidationResult(true)
        }
    }
}
