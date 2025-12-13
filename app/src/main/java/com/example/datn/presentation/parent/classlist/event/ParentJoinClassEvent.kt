package com.example.datn.presentation.parent.classlist.event

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.Class
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.presentation.parent.classlist.state.SearchType

sealed class ParentJoinClassEvent : BaseEvent {
    // Load linked students
    object LoadLinkedStudents : ParentJoinClassEvent()
    
    // Student selection
    data class SelectStudent(val student: LinkedStudentInfo) : ParentJoinClassEvent()
    object ShowStudentSelectionDialog : ParentJoinClassEvent()
    object DismissStudentSelectionDialog : ParentJoinClassEvent()
    
    // Search classes
    data class UpdateSearchQuery(val query: String) : ParentJoinClassEvent()
    data class UpdateSearchType(val type: SearchType) : ParentJoinClassEvent()
    object SearchClasses : ParentJoinClassEvent()
    object ClearSearch : ParentJoinClassEvent()
    
    // Class details
    data class SelectClass(val classModel: Class) : ParentJoinClassEvent()
    object ShowClassDetailsDialog : ParentJoinClassEvent()
    object DismissClassDetailsDialog : ParentJoinClassEvent()
    data class LoadEnrollmentStatus(val classId: String, val studentId: String) : ParentJoinClassEvent()
    
    // Join class
    data class JoinClass(val classId: String, val studentId: String) : ParentJoinClassEvent()
    
    // Clear messages
    object ClearMessages : ParentJoinClassEvent()
}
