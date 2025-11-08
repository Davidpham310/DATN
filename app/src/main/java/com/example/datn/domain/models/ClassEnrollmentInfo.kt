package com.example.datn.domain.models

import java.time.Instant

/**
 * Model chứa thông tin chi tiết về lớp học mà con của phụ huynh đang tham gia.
 * Kết hợp thông tin từ Class, Teacher, Student và ClassStudent.
 */
data class ClassEnrollmentInfo(
    // Thông tin lớp học
    val classId: String,
    val className: String,
    val classCode: String,
    val subject: String?,
    val gradeLevel: Int?,
    
    // Thông tin giáo viên
    val teacherId: String,
    val teacherName: String,
    val teacherAvatar: String?,
    val teacherSpecialization: String,
    
    // Thông tin học sinh (con)
    val studentId: String,
    val studentName: String,
    val studentAvatar: String?,
    
    // Thông tin tham gia
    val enrollmentStatus: EnrollmentStatus,
    val enrolledDate: Instant,
    val approvedBy: String?,
    val rejectionReason: String?,
    
    // Metadata
    val classCreatedAt: Instant,
    val classUpdatedAt: Instant
)
