package com.example.datn.presentation.parent.classlist.event

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.EnrollmentStatus

/**
 * Events cho màn hình danh sách lớp học của phụ huynh
 */
sealed class ParentClassListEvent : BaseEvent {
    // Load data
    object LoadLinkedStudents : ParentClassListEvent()
    object LoadClasses : ParentClassListEvent()
    object Refresh : ParentClassListEvent()
    
    // Filter actions
    data class FilterByStudent(val studentId: String?) : ParentClassListEvent()
    data class FilterByEnrollmentStatus(val status: EnrollmentStatus?) : ParentClassListEvent()
    object ClearFilters : ParentClassListEvent()
    object ToggleFilterDialog : ParentClassListEvent()
    
    // Navigation
    data class NavigateToClassDetail(val classId: String) : ParentClassListEvent()
    data class NavigateToStudentProfile(val studentId: String) : ParentClassListEvent()
}
