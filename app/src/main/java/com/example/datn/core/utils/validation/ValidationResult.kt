package com.example.datn.core.utils.validation

data class ValidationResult(
    val successful: Boolean,
    val errorMessage: String? = null
)
