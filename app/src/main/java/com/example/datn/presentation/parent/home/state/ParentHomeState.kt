package com.example.datn.presentation.parent.home.state

import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo

data class ParentHomeState(
    val isLoading: Boolean = false,
    val linkedStudents: List<LinkedStudentInfo> = emptyList(),
    val error: String? = null
)
