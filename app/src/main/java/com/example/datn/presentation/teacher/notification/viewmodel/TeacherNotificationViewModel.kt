package com.example.datn.presentation.teacher.notification.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.validation.rules.notification.ValidateNotificationContent
import com.example.datn.core.utils.validation.rules.notification.ValidateNotificationTitle
import com.example.datn.domain.models.NotificationType
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.notification.SendTeacherNotificationParams
import com.example.datn.domain.usecase.notification.SendTeacherNotificationUseCase
import com.example.datn.domain.usecase.notification.SendBulkNotificationUseCase
import com.example.datn.domain.usecase.notification.SendBulkNotificationParams
import com.example.datn.domain.usecase.notification.RecipientType
import com.example.datn.domain.usecase.notification.GetReferenceObjectsUseCase
import com.example.datn.domain.usecase.notification.ReferenceObjectType
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.presentation.common.notifications.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel để quản lý UI logic cho màn hình gửi notification cho giáo viên
 * 
 * Chức năng:
 * - Quản lý form state cho việc nhập thông tin notification
 * - Validate dữ liệu trước khi gửi
 * - Gửi notification qua use case
 * - Xử lý kết quả và hiển thị notification cho user
 * 
 * @param sendTeacherNotificationUseCase Use case để gửi notification
 * @param notificationManager Manager để hiển thị thông báo UI
 */
@HiltViewModel
class TeacherNotificationViewModel @Inject constructor(
    private val sendTeacherNotificationUseCase: SendTeacherNotificationUseCase,
    private val sendBulkNotificationUseCase: SendBulkNotificationUseCase,
    private val getReferenceObjectsUseCase: GetReferenceObjectsUseCase,
    private val classRepository: IClassRepository,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<TeacherNotificationState, TeacherNotificationEvent>(
    TeacherNotificationState(),
    notificationManager
) {

    private val validateNotificationTitle = ValidateNotificationTitle()
    private val validateNotificationContent = ValidateNotificationContent()
    
    init {
        loadCurrentUser()
    }

    /**
     * Tự động lấy thông tin user hiện tại
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            authUseCases.getCurrentUser().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { user ->
                            setState {
                                copy(
                                    senderId = user.id,
                                    senderName = user.name
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        showNotification(
                            "Không thể lấy thông tin người gửi",
                            com.example.datn.presentation.common.notifications.NotificationType.ERROR
                        )
                    }
                    is Resource.Loading -> { /* Ignore */ }
                }
            }
        }
    }
    
    override fun onEvent(event: TeacherNotificationEvent) {
        when (event) {
            is TeacherNotificationEvent.OnRecipientTypeSelected -> {
                setState { 
                    copy(
                        recipientType = event.type,
                        selectedClassId = if (event.type.requiresClass) null else selectedClassId
                    ) 
                }
                // Load classes nếu cần
                if (event.type.requiresClass) {
                    loadClasses()
                }
            }
            is TeacherNotificationEvent.OnClassSelected -> {
                setState { copy(selectedClassId = event.classId) }
            }
            is TeacherNotificationEvent.OnTitleChanged -> {
                setState { copy(title = event.title) }
            }
            is TeacherNotificationEvent.OnContentChanged -> {
                setState { copy(content = event.content) }
            }
            is TeacherNotificationEvent.OnNotificationTypeSelected -> {
                setState { copy(selectedNotificationType = event.type) }
            }
            is TeacherNotificationEvent.OnReferenceTypeSelected -> {
                handleReferenceTypeSelection(event.type)
            }
            is TeacherNotificationEvent.OnReferenceObjectSelected -> {
                setState { copy(selectedReferenceObject = event.obj) }
            }
            is TeacherNotificationEvent.OnReferenceParentSelected -> {
                handleReferenceParentSelection(event.parentId)
            }
            is TeacherNotificationEvent.OnSendNotificationClicked -> {
                sendNotification()
            }
            is TeacherNotificationEvent.OnResetFormClicked -> {
                resetForm()
            }
            is TeacherNotificationEvent.OnCancelClicked -> {
                setState { copy(showCancelConfirmDialog = true) }
            }
            is TeacherNotificationEvent.OnDismissCancelConfirmDialog -> {
                setState { copy(showCancelConfirmDialog = false) }
            }
            is TeacherNotificationEvent.OnConfirmCancel -> {
                setState { copy(showCancelConfirmDialog = false, shouldNavigateBack = true) }
            }
            is TeacherNotificationEvent.OnDismissSuccessDialog -> {
                setState { copy(showSuccessDialog = false) }
            }
        }
    }

    /**
     * Validate và gửi notification (bulk hoặc single)
     */
    private fun sendNotification() {
        val currentState = state.value
        
        // Validate inputs
        if (!validateInputs(currentState)) {
            return
        }
        
        // Check sender ID
        if (currentState.senderId.isNullOrBlank()) {
            showNotification(
                "Đang tải thông tin người gửi, vui lòng thử lại",
                com.example.datn.presentation.common.notifications.NotificationType.ERROR
            )
            return
        }
        
        // Gửi bulk notification
        sendBulkNotification()
    }
    
    /**
     * Gửi notification cho nhiều người
     */
    private fun sendBulkNotification() {
        val currentState = state.value
        
        viewModelScope.launch {
            // Lấy reference info từ selected object
            val refObjectId = currentState.selectedReferenceObject?.id
            val refObjectType = if (currentState.selectedReferenceType != ReferenceObjectType.NONE) {
                currentState.selectedReferenceType.value
            } else null
            
            val params = SendBulkNotificationParams(
                senderId = currentState.senderId!!, // Đã check ở trên
                recipientType = currentState.recipientType,
                type = currentState.selectedNotificationType,
                title = currentState.title.trim(),
                content = currentState.content.trim(),
                referenceObjectId = refObjectId,
                referenceObjectType = refObjectType,
                classId = currentState.selectedClassId
            )
            
            sendBulkNotificationUseCase(params)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> {
                            setState { copy(isLoading = true, error = null, isSent = false) }
                        }
                        is Resource.Success -> {
                            val bulkResult = result.data
                            setState { 
                                copy(
                                    isLoading = false, 
                                    isSent = true,
                                    showSuccessDialog = true,
                                    bulkSendResult = bulkResult,
                                    error = null
                                ) 
                            }
                            
                            val message = if (bulkResult?.isFullySuccessful == true) {
                                "Gửi thành công cho ${bulkResult.successCount} người!"
                            } else {
                                "Gửi thành công ${bulkResult?.successCount}/${bulkResult?.totalRecipients} người"
                            }
                            
                            showNotification(
                                message,
                                com.example.datn.presentation.common.notifications.NotificationType.SUCCESS
                            )
                            // Tự động reset form sau 2s
                            kotlinx.coroutines.delay(2000)
                            resetForm()
                        }
                        is Resource.Error -> {
                            setState { 
                                copy(
                                    isLoading = false, 
                                    error = result.message,
                                    isSent = false
                                ) 
                            }
                            showNotification(
                                result.message ?: "Không thể gửi thông báo",
                                com.example.datn.presentation.common.notifications.NotificationType.ERROR,
                                duration = 5000L
                            )
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * Validate các trường input
     */
    private fun validateInputs(state: TeacherNotificationState): Boolean {
        when {
            state.recipientType.requiresClass && state.selectedClassId.isNullOrBlank() -> {
                showNotification(
                    "Vui lòng chọn lớp học",
                    com.example.datn.presentation.common.notifications.NotificationType.ERROR
                )
                return false
            }
        }

        val titleResult = validateNotificationTitle.validate(state.title)
        if (!titleResult.successful) {
            showNotification(
                titleResult.errorMessage ?: "Tiêu đề không hợp lệ",
                com.example.datn.presentation.common.notifications.NotificationType.ERROR
            )
            return false
        }

        val contentResult = validateNotificationContent.validate(state.content)
        if (!contentResult.successful) {
            showNotification(
                contentResult.errorMessage ?: "Nội dung thông báo không hợp lệ",
                com.example.datn.presentation.common.notifications.NotificationType.ERROR
            )
            return false
        }

        return true
    }

    /**
     * Xử lý khi chọn reference type
     */
    private fun handleReferenceTypeSelection(type: ReferenceObjectType) {
        setState { 
            copy(
                selectedReferenceType = type,
                selectedReferenceObject = null,
                availableReferenceObjects = emptyList(),
                referenceParentId = null
            ) 
        }
        
        // Nếu chọn NONE hoặc MESSAGE, không cần load
        if (type == ReferenceObjectType.NONE || type == ReferenceObjectType.MESSAGE) {
            return
        }
        
        // Nếu là CLASS, load ngay
        if (type == ReferenceObjectType.CLASS) {
            loadReferenceObjects(type, null)
        }
        // Các type khác cần chọn parent trước (sẽ handle trong UI)
    }
    
    /**
     * Xử lý khi chọn parent (class/lesson) cho reference
     */
    private fun handleReferenceParentSelection(parentId: String) {
        val currentType = state.value.selectedReferenceType
        setState { copy(referenceParentId = parentId) }
        
        // Load objects dựa vào parent đã chọn
        loadReferenceObjects(currentType, parentId)
    }
    
    /**
     * Load danh sách reference objects
     */
    private fun loadReferenceObjects(type: ReferenceObjectType, parentId: String?) {
        viewModelScope.launch {
            setState { copy(isLoadingReferences = true) }
            
            getReferenceObjectsUseCase(type, parentId)
                .collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            setState { 
                                copy(
                                    isLoadingReferences = false,
                                    availableReferenceObjects = result.data ?: emptyList()
                                ) 
                            }
                        }
                        is Resource.Error -> {
                            setState { copy(isLoadingReferences = false) }
                            showNotification(
                                result.message ?: "Không thể tải danh sách",
                                com.example.datn.presentation.common.notifications.NotificationType.ERROR
                            )
                        }
                        is Resource.Loading -> {
                            setState { copy(isLoadingReferences = true) }
                        }
                    }
                }
        }
    }
    
    /**
     * Load danh sách classes
     */
    private fun loadClasses() {
        viewModelScope.launch {
            setState { copy(isLoadingClasses = true) }
            
            classRepository.getAllClasses()
                .collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            setState { 
                                copy(
                                    isLoadingClasses = false,
                                    availableClasses = result.data ?: emptyList()
                                ) 
                            }
                        }
                        is Resource.Error -> {
                            setState { copy(isLoadingClasses = false) }
                            showNotification(
                                result.message ?: "Không thể tải danh sách lớp",
                                com.example.datn.presentation.common.notifications.NotificationType.ERROR
                            )
                        }
                        is Resource.Loading -> {
                            setState { copy(isLoadingClasses = true) }
                        }
                    }
                }
        }
    }
    
    /**
     * Reset form về trạng thái ban đầu (giữ nguyên sender info)
     */
    private fun resetForm() {
        setState {
            copy(
                recipientType = RecipientType.ALL_TEACHERS,
                selectedClassId = null,
                availableClasses = emptyList(),
                isLoadingClasses = false,
                title = "",
                content = "",
                selectedNotificationType = NotificationType.SYSTEM_ALERT,
                selectedReferenceType = ReferenceObjectType.NONE,
                availableReferenceObjects = emptyList(),
                selectedReferenceObject = null,
                isLoadingReferences = false,
                referenceParentId = null,
                isSent = false,
                isLoading = false,
                error = null,
                showSuccessDialog = false,
                bulkSendResult = null,
                showCancelConfirmDialog = false,
                shouldNavigateBack = false
            )
        }
    }
}
