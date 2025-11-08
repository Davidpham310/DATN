package com.example.datn.presentation.teacher.enrollment

import com.example.datn.core.base.BaseEvent

/**
 * Events for Enrollment Management Screen
 */
sealed class EnrollmentManagementEvent : BaseEvent {
    // Load pending enrollments
    data class LoadPendingEnrollments(val classId: String) : EnrollmentManagementEvent()
    
    // Enrollment actions
    data class SelectEnrollment(val enrollment: EnrollmentWithStudent) : EnrollmentManagementEvent()
    object ShowApproveDialog : EnrollmentManagementEvent()
    object ShowRejectDialog : EnrollmentManagementEvent()
    object DismissApproveDialog : EnrollmentManagementEvent()
    object DismissRejectDialog : EnrollmentManagementEvent()
    
    // Approve/Reject actions
    data class ApproveEnrollment(
        val classId: String,
        val studentId: String
    ) : EnrollmentManagementEvent()
    
    data class RejectEnrollment(
        val classId: String,
        val studentId: String,
        val reason: String
    ) : EnrollmentManagementEvent()
    
    data class UpdateRejectionReason(val reason: String) : EnrollmentManagementEvent()
    
    // Batch operations
    data class BatchApproveAll(val classId: String) : EnrollmentManagementEvent()
    
    // Filter and sort
    data class UpdateSearchQuery(val query: String) : EnrollmentManagementEvent()
    data class UpdateSortType(val sortType: SortType) : EnrollmentManagementEvent()
    
    // Refresh
    object Refresh : EnrollmentManagementEvent()
    
    // Clear messages
    object ClearMessages : EnrollmentManagementEvent()
}
