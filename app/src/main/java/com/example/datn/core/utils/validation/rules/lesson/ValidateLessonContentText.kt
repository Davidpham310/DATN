package com.example.datn.core.utils.validation.rules.lesson

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateLessonContentText : Validator<String?> {
    override fun validate(value: String?): ValidationResult {
        val trimmed = value?.trim().orEmpty()

        return when {
            trimmed.isBlank() -> ValidationResult(false, "Nội dung văn bản không được để trống")
            trimmed.length < 5 -> ValidationResult(false, "Nội dung văn bản phải có ít nhất 5 ký tự")
            trimmed.length > 20000 -> ValidationResult(false, "Nội dung văn bản không được vượt quá 20000 ký tự")
            else -> ValidationResult(true)
        }
    }
}
