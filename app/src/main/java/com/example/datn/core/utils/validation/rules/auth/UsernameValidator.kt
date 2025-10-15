package com.example.datn.core.utils.validation.rules.auth

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class UsernameValidator : Validator<String> {
    override fun validate(input: String): ValidationResult {
        return if (input.isBlank()) {
            ValidationResult(false, "Tên người dùng không được để trống")
        } else {
            ValidationResult(true)
        }
    }
}