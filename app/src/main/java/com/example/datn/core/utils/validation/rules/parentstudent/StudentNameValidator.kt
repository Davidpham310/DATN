package com.example.datn.core.utils.validation.rules.parentstudent

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class StudentNameValidator : Validator<String> {
    override fun validate(input: String): ValidationResult {
        val trimmed = input.trim()

        return when {
            trimmed.isBlank() -> ValidationResult(false, "Tên học sinh không được để trống")
            trimmed.length < 3 -> ValidationResult(false, "Tên học sinh phải có ít nhất 3 ký tự")
            !Regex("^[\\p{L} ]+$").matches(trimmed) -> ValidationResult(false, "Tên học sinh không được chứa ký tự đặc biệt")
            else -> ValidationResult(true)
        }
    }
}
