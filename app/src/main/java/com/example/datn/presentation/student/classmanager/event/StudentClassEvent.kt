package com.example.datn.presentation.student.classmanager.event

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.Class

sealed class StudentClassEvent : BaseEvent {
    // Load my classes
    object LoadMyClasses : StudentClassEvent()
    
    // Search class by code
    data class SearchClassByCode(val classCode: String) : StudentClassEvent()
    
    // Join class
    data class JoinClass(val classId: String) : StudentClassEvent()
    
    // Withdraw from class
    data class WithdrawFromClass(val classId: String) : StudentClassEvent()
    
    // Select class to view detail
    data class SelectClass(val classModel: Class) : StudentClassEvent()
    
    // Load enrollment status
    data class LoadEnrollmentStatus(val classId: String) : StudentClassEvent()
    
    // Dialog actions
    object ShowJoinClassDialog : StudentClassEvent()
    object DismissJoinClassDialog : StudentClassEvent()
    data class ShowWithdrawConfirmDialog(val classModel: Class) : StudentClassEvent()
    object DismissWithdrawConfirmDialog : StudentClassEvent()
    
    // Clear messages
    object ClearMessages : StudentClassEvent()
}
