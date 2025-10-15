package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateSubject : Validator<String> {
    override fun validate(value: String): ValidationResult {
        if (value.isBlank()) {
            return ValidationResult(false, "Môn học không được để trống")
        }
        if (value.length < 2) {
            return ValidationResult(false, "Tên môn học phải có ít nhất 2 ký tự")
        }
        return ValidationResult(true)
    }
}