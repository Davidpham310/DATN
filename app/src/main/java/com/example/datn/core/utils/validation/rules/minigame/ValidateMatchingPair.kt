package com.example.datn.core.utils.validation.rules.minigame

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator

class ValidateMatchingPair : Validator<Pair<String?, String?>> {
    override fun validate(input: Pair<String?, String?>): ValidationResult {
        val (content, pairContent) = input
        val c = content?.trim().orEmpty()
        val p = pairContent?.trim().orEmpty()

        // If pairContent is used, content must exist; and pairContent must not equal content
        if (p.isBlank()) return ValidationResult(true)
        if (c.isBlank()) return ValidationResult(false, "Nội dung đáp án không được để trống khi dùng cặp ghép")
        if (c.equals(p, ignoreCase = true)) return ValidationResult(false, "Nội dung cặp ghép không được trùng với nội dung đáp án")

        return ValidationResult(true)
    }
}
