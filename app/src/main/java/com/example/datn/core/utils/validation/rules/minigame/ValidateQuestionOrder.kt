package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateQuestionOrder : Validator<Int> {
    override fun validate(value: Int): ValidationResult {
        return when {
            value < 0 -> ValidationResult(false, "Thứ tự (order) phải là số không âm")
            value > 10000 -> ValidationResult(false, "Thứ tự (order) không được vượt quá 10000")
            else -> ValidationResult(true)
        }
    }
}
