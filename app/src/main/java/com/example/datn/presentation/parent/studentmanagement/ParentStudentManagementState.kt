package com.example.datn.presentation.parent.studentmanagement

import com.example.datn.core.base.BaseState
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo

data class ParentStudentManagementState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val linkedStudents: List<LinkedStudentInfo> = emptyList(),
    val showCreateStudentDialog: Boolean = false,
    val showLinkStudentDialog: Boolean = false,
    val showEditStudentDialog: Boolean = false,
    val editingStudent: LinkedStudentInfo? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val deletingStudentInfo: LinkedStudentInfo? = null
) : BaseState

