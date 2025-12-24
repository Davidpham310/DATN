package com.example.datn.core.utils.validation.rules.parentstudent

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class StudentPasswordValidator : Validator<String> {
    override fun validate(input: String): ValidationResult {
        val regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{6,}$")

        return if (regex.matches(input)) {
            ValidationResult(true)
        } else {
            ValidationResult(
                false,
                "Mật khẩu phải ít nhất 6 ký tự, có chữ hoa, chữ thường và ký tự đặc biệt"
            )
        }
    }
}
