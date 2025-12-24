package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateMiniGameTitle : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val trimmed = value.trim()
        val allowedTitleRegex = Regex("^[\\p{L}\\p{N} ]+$")
        return when {
            trimmed.isBlank() -> ValidationResult(false, "Tiêu đề không được để trống")
            trimmed.length < 3 -> ValidationResult(false, "Tiêu đề phải có ít nhất 3 ký tự")
            !allowedTitleRegex.matches(trimmed) -> ValidationResult(false, "Tiêu đề không được chứa ký tự đặc biệt")
            else -> ValidationResult(true)
        }
    }
}
