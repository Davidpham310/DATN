package com.example.datn.core.utils.validation.rules

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class PasswordValidator : Validator<String> {
    override fun validate(input: String): ValidationResult {
        return if (input.length < 6) {
            ValidationResult(false, "Mật khẩu phải có ít nhất 6 ký tự")
        } else {
            ValidationResult(true)
        }
    }
}
