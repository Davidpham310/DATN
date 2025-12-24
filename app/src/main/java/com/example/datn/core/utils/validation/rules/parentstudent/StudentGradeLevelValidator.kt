package com.example.datn.core.utils.validation.rules.parentstudent

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class StudentGradeLevelValidator : Validator<String> {
    override fun validate(input: String): ValidationResult {
        val trimmed = input.trim()

        if (trimmed.isBlank()) {
            return ValidationResult(false, "Vui lòng chọn khối lớp")
        }

        val gradeNumberMatch = Regex("^(\\d{1,2})").find(trimmed)
        val gradeNumber = gradeNumberMatch?.groupValues?.getOrNull(1)?.toIntOrNull()

        return if (gradeNumber == null || gradeNumber !in 1..12) {
            ValidationResult(false, "Khối lớp phải từ 1 đến 12")
        } else {
            ValidationResult(true)
        }
    }
}
