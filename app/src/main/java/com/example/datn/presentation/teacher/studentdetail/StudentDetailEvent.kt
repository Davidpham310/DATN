package com.example.datn.presentation.teacher.studentdetail

import com.example.datn.core.base.BaseEvent

/**
 * Events for Student Detail Screen
 */
sealed class StudentDetailEvent : BaseEvent {
    /**
     * Load comprehensive student information
     */
    data class LoadStudentDetail(
        val studentId: String,
        val classId: String
    ) : StudentDetailEvent()
    
    /**
     * Change selected tab (Overview, Tests, Assignments, etc.)
     */
    data class ChangeTab(val tabIndex: Int) : StudentDetailEvent()
    
    /**
     * Refresh all student data
     */
    data object Refresh : StudentDetailEvent()
    
    /**
     * Clear error message
     */
    data object ClearError : StudentDetailEvent()
}
