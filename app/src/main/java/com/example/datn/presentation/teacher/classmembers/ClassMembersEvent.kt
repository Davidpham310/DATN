package com.example.datn.presentation.teacher.classmembers

import com.example.datn.core.base.BaseEvent

/**
 * Events for Class Members Screen
 */
sealed class ClassMembersEvent : BaseEvent {
    /**
     * Load approved students in class
     */
    data class LoadMembers(val classId: String, val className: String) : ClassMembersEvent()
    
    /**
     * Search students by name
     */
    data class SearchStudents(val query: String) : ClassMembersEvent()
    
    /**
     * Refresh the student list
     */
    data object Refresh : ClassMembersEvent()
    
    /**
     * Clear any error messages
     */
    data object ClearError : ClassMembersEvent()
}
