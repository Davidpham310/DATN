package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateGradeLevelText : Validator<String> {
    override fun validate(value: String): ValidationResult {
        if (value.isBlank()) {
            return ValidationResult(false, "Khối lớp không được để trống")
        }

        if (value.toInt() !in 1..12) {
            return ValidationResult(false, "Khối lớp phải từ 1 đến 12")
        }

        return ValidationResult(true)
    }
}
