package com.example.datn.core.utils.validation.rules.auth

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class PasswordValidator : Validator<String> {
    override fun validate(input: String): ValidationResult {
        // Regex:
        // ^                 : bắt đầu chuỗi
        // (?=.*[a-z])       : ít nhất 1 chữ thường
        // (?=.*[A-Z])       : ít nhất 1 chữ hoa
        // (?=.*[!@#\$%^&*]) : ít nhất 1 ký tự đặc biệt (có thể thêm ký tự khác nếu muốn)
        // .{6,}             : ít nhất 6 ký tự
        // $                 : kết thúc chuỗi
        val regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\$%^&*]).{6,}\$")

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