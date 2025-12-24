package com.example.datn.core.utils.validation.rules.test

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateTestDisplayOrder : Validator<Int> {
    override fun validate(input: Int): ValidationResult {
        return when {
            input <= 0 -> ValidationResult(false, "Thứ tự phải lớn hơn 0 giây")
            input > 10000 -> ValidationResult(false, "Thứ tự không được vượt quá 10000")
            else -> ValidationResult(true)
        }
    }
}
