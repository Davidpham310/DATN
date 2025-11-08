package com.example.datn.presentation.teacher.enrollment

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.models.User

/**
 * State for Enrollment Management Screen
 * Manages pending enrollment requests for teacher approval/rejection
 */
data class EnrollmentManagementState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    
    // Current class being managed
    val classId: String = "",
    val className: String = "",
    
    // List of pending enrollments with student info
    val pendingEnrollments: List<EnrollmentWithStudent> = emptyList(),
    
    // Selected enrollment for action
    val selectedEnrollment: EnrollmentWithStudent? = null,
    
    // Dialog states
    val showApproveDialog: Boolean = false,
    val showRejectDialog: Boolean = false,
    val rejectionReason: String = "",
    
    // Filter and sort
    val searchQuery: String = "",
    val sortBy: SortType = SortType.BY_DATE_DESC,
    
    // Success message
    val successMessage: String? = null
) : BaseState

/**
 * Data class combining ClassStudent enrollment with User info
 */
data class EnrollmentWithStudent(
    val enrollment: ClassStudent,
    val studentInfo: User
)

/**
 * Sort options for pending enrollments
 */
enum class SortType(val displayName: String) {
    BY_DATE_ASC("Cũ nhất"),
    BY_DATE_DESC("Mới nhất"),
    BY_NAME_ASC("Tên A-Z"),
    BY_NAME_DESC("Tên Z-A");
    
    companion object {
        fun fromDisplayName(name: String): SortType? {
            return values().find { it.displayName == name }
        }
    }
}
