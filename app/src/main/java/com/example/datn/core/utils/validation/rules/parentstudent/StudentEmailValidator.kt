package com.example.datn.core.utils.validation.rules.parentstudent

import android.util.Patterns
import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class StudentEmailValidator : Validator<String> {
    override fun validate(input: String): ValidationResult {
        val trimmed = input.trim()

        return when {
            trimmed.isBlank() -> ValidationResult(false, "Email không được để trống")
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> ValidationResult(false, "Email không đúng định dạng")
            else -> ValidationResult(true)
        }
    }
}
