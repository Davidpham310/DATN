package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator
import com.example.datn.core.utils.validation.AllowedSubjects

class ValidateSubject : Validator<String> {
    override fun validate(value: String): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Môn học không được để trống")
        }
        if (!AllowedSubjects.allowedSubjects.contains(trimmed)) {
            return ValidationResult(false, "Môn học không hợp lệ")
        }
        return ValidationResult(true)
    }
}