package com.example.datn.core.utils.validation

interface Validator<T> {
    fun validate(input: T): ValidationResult
}