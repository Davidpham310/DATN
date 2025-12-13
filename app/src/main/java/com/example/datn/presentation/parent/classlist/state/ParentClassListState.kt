package com.example.datn.presentation.parent.classlist.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.ClassEnrollmentInfo
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.models.Student

/**
 * State cho màn hình danh sách lớp học của phụ huynh
 */
data class ParentClassListState(
    // BaseState requirements
    override val isLoading: Boolean = false,
    override val error: String? = null,
    
    // Danh sách con của phụ huynh
    val linkedStudents: List<Student> = emptyList(),
    val isLoadingStudents: Boolean = false,
    val studentsError: String? = null,
    
    // Danh sách lớp học
    val classEnrollments: List<ClassEnrollmentInfo> = emptyList(),
    val isLoadingClasses: Boolean = false,
    val classesError: String? = null,
    
    // Filter
    val selectedStudentId: String? = null, // null = tất cả học sinh
    val selectedEnrollmentStatus: EnrollmentStatus? = null, // null = tất cả trạng thái
    
    // UI State
    val showFilterDialog: Boolean = false,
    val refreshing: Boolean = false
) : BaseState
