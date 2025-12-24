package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateOptionPairContent : Validator<String?> {
    override fun validate(input: String?): ValidationResult {
        val value = input?.trim().orEmpty()
        if (value.isBlank()) return ValidationResult(true)

        return when {
            value.length < 1 -> ValidationResult(false, "Nội dung cặp ghép không hợp lệ")
            value.length > 200 -> ValidationResult(false, "Nội dung cặp ghép không được vượt quá 200 ký tự")
            else -> ValidationResult(true)
        }
    }
}
