package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateClassName : Validator<String> {
    override fun validate(value: String): ValidationResult {
        if (value.isBlank()) {
            return ValidationResult(false, "Tên lớp không được để trống")
        }
        if (value.length < 3) {
            return ValidationResult(false, "Tên lớp phải có ít nhất 3 ký tự")
        }
        return ValidationResult(true)
    }
}