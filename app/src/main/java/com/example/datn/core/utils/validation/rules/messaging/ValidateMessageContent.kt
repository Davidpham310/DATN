package com.example.datn.core.utils.validation.rules.messaging

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateMessageContent : Validator<String> {
    override fun validate(value: String): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult(false, "Vui lòng nhập nội dung tin nhắn")
        } else {
            ValidationResult(true)
        }
    }
}
