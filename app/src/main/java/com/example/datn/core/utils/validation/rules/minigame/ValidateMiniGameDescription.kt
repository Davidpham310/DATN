package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateMiniGameDescription : Validator<String> {
    override fun validate(input: String): ValidationResult {
        val value = input.trim()
        if (value.isBlank()) return ValidationResult(true)

        return when {
            value.length < 5 -> ValidationResult(false, "Mô tả phải có ít nhất 5 ký tự")
            value.length > 500 -> ValidationResult(false, "Mô tả không được vượt quá 500 ký tự")
            else -> ValidationResult(true)
        }
    }
}
