package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateClassCode : Validator<String> {
    override fun validate(value: String): ValidationResult {
        if (value.isBlank()) {
            return ValidationResult(false, "Mã lớp không được để trống")
        }
        val regex = Regex("^[A-Za-z0-9_-]{3,10}$")
        if (!regex.matches(value)) {
            return ValidationResult(false, "Mã lớp chỉ được chứa chữ, số, dấu gạch dưới hoặc gạch ngang (3–10 ký tự)")
        }
        return ValidationResult(true)
    }
}