package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateClassCode : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Mã lớp không được để trống")
        }
        if (trimmed.length < 6 || trimmed.length > 10) {
            return ValidationResult(false, "Mã lớp phải từ 6–10 ký tự")
        }
        val regex = Regex("^[A-Z0-9]{6,10}$")
        if (!regex.matches(trimmed)) {
            return ValidationResult(false, "Mã lớp chỉ gồm chữ HOA và số")
        }
        return ValidationResult(true)
    }
}