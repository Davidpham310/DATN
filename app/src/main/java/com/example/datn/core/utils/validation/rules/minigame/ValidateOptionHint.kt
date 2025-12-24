package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateOptionHint : Validator<String?> {
    override fun validate(input: String?): ValidationResult {
        val value = input?.trim().orEmpty()
        if (value.isBlank()) return ValidationResult(true)

        return when {
            value.length < 3 -> ValidationResult(false, "Gợi ý phải có ít nhất 3 ký tự")
            !value.contains('_') -> ValidationResult(false, "Gợi ý nên chứa ký tự '_' để ẩn ký tự")
            value.length > 200 -> ValidationResult(false, "Gợi ý không được vượt quá 200 ký tự")
            else -> ValidationResult(true)
        }
    }
}
