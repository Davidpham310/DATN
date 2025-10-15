package com.example.datn.core.utils.validation.rules.auth

import android.util.Patterns
import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class EmailValidator : Validator<String> {
    override fun validate(input: String): ValidationResult {
        return if (input.isBlank()) {
            ValidationResult(false, "Email không được để trống")
        } else if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            ValidationResult(false, "Email không hợp lệ")
        } else {
            ValidationResult(true)
        }
    }
}