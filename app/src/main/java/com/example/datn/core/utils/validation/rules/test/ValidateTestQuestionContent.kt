package com.example.datn.core.utils.validation.rules.test

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateTestQuestionContent : Validator<String> {
    override fun validate(input: String): ValidationResult {
        val value = input.trim()

        return when {
            value.isBlank() -> ValidationResult(false, "Nội dung câu hỏi không được để trống")
            value.length < 5 -> ValidationResult(false, "Nội dung câu hỏi phải có ít nhất 5 ký tự")
            else -> ValidationResult(true)
        }
    }
}
