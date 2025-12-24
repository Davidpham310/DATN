package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateGradeLevel : Validator<Int> {
    override fun validate(value: Int): ValidationResult {
        if (value < 1 || value > 12) {
            return ValidationResult(false, "Khối lớp phải từ 1 đến 12")
        }
        return ValidationResult(true)
    }
}