package com.example.datn.core.utils.validation.rules.lesson

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator
import com.example.datn.domain.models.ContentType
import java.io.InputStream

data class LessonContentFileValidationInput(
    val contentType: ContentType,
    val stream: InputStream?,
    val size: Long
)

class ValidateLessonContentFile : Validator<LessonContentFileValidationInput> {
    override fun validate(input: LessonContentFileValidationInput): ValidationResult {
        val stream = input.stream
        val size = input.size

        val maxSizeBytes = when (input.contentType) {
            ContentType.VIDEO -> 10L * 1024 * 1024 * 1024
            else -> Long.MAX_VALUE
        }

        return when {
            stream == null -> ValidationResult(false, "Vui lòng chọn tệp tin")
            size <= 0L -> ValidationResult(false, "Kích thước tệp tin không hợp lệ")
            size > maxSizeBytes -> ValidationResult(false, "Dung lượng video không được vượt quá 10GB")
            else -> ValidationResult(true)
        }
    }
}
