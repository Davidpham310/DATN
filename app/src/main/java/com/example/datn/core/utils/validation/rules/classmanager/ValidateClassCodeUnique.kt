package com.example.datn.core.utils.validation.rules.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.domain.models.Class
import com.example.datn.domain.usecase.classmanager.GetClassByCodeUseCase
import kotlinx.coroutines.flow.first

class ValidateClassCodeUnique(
    private val getClassByCodeUseCase: GetClassByCodeUseCase
) {
    suspend fun validate(classCode: String, currentClassId: String? = null): ValidationResult {
        val trimmed = classCode.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Mã lớp không được để trống")
        }

        val result = getClassByCodeUseCase(trimmed)
            .first { it !is Resource.Loading }

        return when (result) {
            is Resource.Success -> {
                val existing: Class? = result.data
                if (existing == null) {
                    ValidationResult(true)
                } else if (currentClassId != null && existing.id == currentClassId) {
                    ValidationResult(true)
                } else {
                    ValidationResult(false, "Mã lớp đã tồn tại")
                }
            }

            is Resource.Error -> ValidationResult(false, result.message ?: "Không thể kiểm tra mã lớp")

            is Resource.Loading -> ValidationResult(true)
        }
    }
}
