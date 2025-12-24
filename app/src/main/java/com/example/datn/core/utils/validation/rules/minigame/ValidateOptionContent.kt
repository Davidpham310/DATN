package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateOptionContent : Validator<String> {
    override fun validate(input: String): ValidationResult {
        val value = input.trim()
        return when {
            value.isBlank() -> ValidationResult(false, "Nội dung đáp án không được để trống")
            value.length > 200 -> ValidationResult(false, "Nội dung đáp án không được vượt quá 200 ký tự")
            else -> ValidationResult(true)
        }
    }
}
