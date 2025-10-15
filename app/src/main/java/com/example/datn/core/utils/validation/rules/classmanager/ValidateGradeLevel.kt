package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateGradeLevel : Validator<Int> {
    override fun validate(value: Int): ValidationResult {
        if (value <= 0) {
            return ValidationResult(false, "Khối lớp phải lớn hơn 0")
        }
        if (value > 12) {
            return ValidationResult(false, "Khối lớp không được vượt quá 12")
        }
        return ValidationResult(true)
    }
}