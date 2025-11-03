package com.example.datn.presentation.parent.studentmanagement

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.RelationshipType
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import java.time.LocalDate

sealed class ParentStudentManagementEvent : BaseEvent {
    object ShowCreateStudentDialog : ParentStudentManagementEvent()
    object DismissCreateStudentDialog : ParentStudentManagementEvent()
    data class CreateStudentAccount(
        val name: String,
        val email: String,
        val password: String,
        val dateOfBirth: LocalDate,
        val gradeLevel: String
    ) : ParentStudentManagementEvent()
    
    object ShowLinkStudentDialog : ParentStudentManagementEvent()
    object DismissLinkStudentDialog : ParentStudentManagementEvent()
    data class LinkStudent(
        val studentId: String,
        val relationship: RelationshipType,
        val isPrimaryGuardian: Boolean
    ) : ParentStudentManagementEvent()
    
    data class ShowEditStudentDialog(val studentInfo: LinkedStudentInfo) : ParentStudentManagementEvent()
    object DismissEditStudentDialog : ParentStudentManagementEvent()
    data class UpdateStudentInfo(
        val studentId: String,
        val name: String,
        val dateOfBirth: LocalDate,
        val gradeLevel: String
    ) : ParentStudentManagementEvent()
    
    data class UpdateRelationship(
        val studentId: String,
        val relationship: RelationshipType,
        val isPrimaryGuardian: Boolean
    ) : ParentStudentManagementEvent()
    
    data class ShowDeleteConfirmDialog(val studentInfo: LinkedStudentInfo) : ParentStudentManagementEvent()
    object DismissDeleteConfirmDialog : ParentStudentManagementEvent()
    data class ConfirmDeleteLink(val studentId: String) : ParentStudentManagementEvent()
}

