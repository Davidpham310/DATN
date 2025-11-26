package com.example.datn.presentation.teacher.classmembers

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.User

/**
 * State for Class Members Screen
 * Displays list of approved students in a class
 */
data class ClassMemberUi(
    val studentId: String,
    val user: User
)

data class ClassMembersState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    
    // Class information
    val classId: String = "",
    val className: String = "",
    
    // List of approved students
    val students: List<ClassMemberUi> = emptyList(),
    
    // Search and filter
    val searchQuery: String = "",
    
    // Statistics
    val totalStudents: Int = 0
) : BaseState
