package com.example.datn.domain.models

import java.time.Instant

data class ClassStudent(
    val classId: String,
    val studentId: String,
    val enrolledDate: Instant,
    val enrollmentStatus : EnrollmentStatus,
    val approvedBy: String,
    val rejectionReason : String
)