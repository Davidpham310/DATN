package com.example.datn.presentation.teacher.notification.viewmodel

import com.example.datn.core.base.BaseEvent
import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.NotificationType
import com.example.datn.domain.usecase.notification.RecipientType
import com.example.datn.domain.usecase.notification.BulkSendResult
import com.example.datn.domain.usecase.notification.ReferenceObjectType
import com.example.datn.domain.usecase.notification.ReferenceObject

/**
 * State cho TeacherNotificationScreen
 */
data class TeacherNotificationState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    
    // Auto-filled fields
    val senderId: String? = null,
    val senderName: String? = null,
    
    // Form fields
    val recipientType: RecipientType = RecipientType.ALL_TEACHERS,
    val selectedClassId: String? = null, // For STUDENTS_IN_CLASS and PARENTS_IN_CLASS
    val availableClasses: List<com.example.datn.domain.models.Class> = emptyList(),
    val isLoadingClasses: Boolean = false,
    val title: String = "",
    val content: String = "",
    val selectedNotificationType: NotificationType = NotificationType.SYSTEM_ALERT,
    
    // Reference object fields
    val selectedReferenceType: ReferenceObjectType = ReferenceObjectType.NONE,
    val availableReferenceObjects: List<ReferenceObject> = emptyList(),
    val selectedReferenceObject: ReferenceObject? = null,
    val isLoadingReferences: Boolean = false,
    val referenceParentId: String? = null, // For nested selections (e.g., classId for lessons)
    
    // UI state
    val isSent: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val bulkSendResult: BulkSendResult? = null,
    val showCancelConfirmDialog: Boolean = false,
    val shouldNavigateBack: Boolean = false
) : BaseState

/**
 * Events cho TeacherNotificationScreen
 */
sealed class TeacherNotificationEvent : BaseEvent {
    // Form input events
    data class OnRecipientTypeSelected(val type: RecipientType) : TeacherNotificationEvent()
    data class OnClassSelected(val classId: String?) : TeacherNotificationEvent()
    data class OnTitleChanged(val title: String) : TeacherNotificationEvent()
    data class OnContentChanged(val content: String) : TeacherNotificationEvent()
    data class OnNotificationTypeSelected(val type: NotificationType) : TeacherNotificationEvent()
    
    // Reference object events
    data class OnReferenceTypeSelected(val type: ReferenceObjectType) : TeacherNotificationEvent()
    data class OnReferenceObjectSelected(val obj: ReferenceObject?) : TeacherNotificationEvent()
    data class OnReferenceParentSelected(val parentId: String) : TeacherNotificationEvent()
    
    // Action events
    object OnSendNotificationClicked : TeacherNotificationEvent()
    object OnResetFormClicked : TeacherNotificationEvent()
    object OnCancelClicked : TeacherNotificationEvent()
    object OnDismissCancelConfirmDialog : TeacherNotificationEvent()
    object OnConfirmCancel : TeacherNotificationEvent()
    object OnDismissSuccessDialog : TeacherNotificationEvent()
}
