package com.example.datn.core.utils.validation.rules.test

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateTestTitle : Validator<String> {
    override fun validate(value: String): ValidationResult {
        return when {
            value.isBlank() -> ValidationResult(false, "Tiêu đề không được để trống")
            value.length < 3 -> ValidationResult(false, "Tiêu đề phải có ít nhất 3 ký tự")
            else -> ValidationResult(true)
        }
    }
}
