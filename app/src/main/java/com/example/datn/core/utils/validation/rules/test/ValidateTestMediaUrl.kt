package com.example.datn.core.utils.validation.rules.test

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator
import java.net.URI

class ValidateTestMediaUrl : Validator<String?> {
    override fun validate(input: String?): ValidationResult {
        val value = input?.trim().orEmpty()
        if (value.isBlank()) return ValidationResult(true)

        // Accept MinIO object keys (relative paths) or full URLs
        val isObjectKey = value.startsWith("tests/") || value.startsWith("test_options/") || value.startsWith("lessons/")
        if (isObjectKey) return ValidationResult(true)

        return try {
            val uri = URI(value)
            val scheme = uri.scheme?.lowercase()
            if (scheme == "http" || scheme == "https") {
                ValidationResult(true)
            } else {
                ValidationResult(false, "Media URL không hợp lệ")
            }
        } catch (e: Exception) {
            ValidationResult(false, "Media URL không hợp lệ")
        }
    }
}
