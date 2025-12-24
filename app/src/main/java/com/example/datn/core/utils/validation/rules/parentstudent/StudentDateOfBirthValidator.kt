package com.example.datn.core.utils.validation.rules.parentstudent

import com.example.datn.core.utils.validation.ValidationResult
import com.example.datn.core.utils.validation.Validator
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class StudentDateOfBirthValidator(
    private val minAge: Int = 6,
    private val maxAge: Int = 18
) : Validator<String> {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parse(input: String): LocalDate? {
        return try {
            LocalDate.parse(input.trim(), formatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    override fun validate(input: String): ValidationResult {
        val dob = parse(input) ?: return ValidationResult(false, "Ngày sinh không hợp lệ")

        val today = LocalDate.now()
        if (dob.isAfter(today)) {
            return ValidationResult(false, "Ngày sinh không hợp lệ")
        }

        val age = Period.between(dob, today).years
        if (age !in minAge..maxAge) {
            return ValidationResult(false, "Tuổi học sinh không phù hợp với hệ thống")
        }

        return ValidationResult(true)
    }
}
