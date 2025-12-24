package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateQuestionTimeLimit : Validator<Long> {
    override fun validate(input: Long): ValidationResult {
        return when {
            input <= 0L -> ValidationResult(false, "Thời gian phải lớn hơn 0 giây")
            input > 3600L -> ValidationResult(false, "Thời gian không được vượt quá 3600 giây")
            else -> ValidationResult(true)
        }
    }
}
