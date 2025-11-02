package com.example.datn.presentation.student.classmanager

import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentClassViewModel @Inject constructor(
    private val classUseCases: ClassUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<StudentClassState, StudentClassEvent>(StudentClassState(), notificationManager) {

    init {
        loadMyClasses()
    }

    override fun onEvent(event: StudentClassEvent) {
        when (event) {
            is StudentClassEvent.LoadMyClasses -> loadMyClasses()
            is StudentClassEvent.SearchClassByCode -> searchClassByCode(event.classCode)
            is StudentClassEvent.JoinClass -> joinClass(event.classId)
            is StudentClassEvent.WithdrawFromClass -> withdrawFromClass(event.classId)
            is StudentClassEvent.SelectClass -> selectClass(event.classModel)
            is StudentClassEvent.LoadEnrollmentStatus -> loadEnrollmentStatus(event.classId)
            is StudentClassEvent.ShowJoinClassDialog -> setState { copy(showJoinClassDialog = true) }
            is StudentClassEvent.DismissJoinClassDialog -> setState { copy(showJoinClassDialog = false, searchedClass = null, searchCode = "") }
            is StudentClassEvent.ShowWithdrawConfirmDialog -> setState { copy(showWithdrawConfirmDialog = true, selectedClass = event.classModel) }
            is StudentClassEvent.DismissWithdrawConfirmDialog -> setState { copy(showWithdrawConfirmDialog = false) }
            is StudentClassEvent.ClearMessages -> setState { copy(successMessage = null, error = null) }
        }
    }

    private fun loadMyClasses() {
        launch {
            val currentUserId = authUseCases.getCurrentIdUser.invoke().first()
            if (currentUserId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            classUseCases.getClassesByStudent(currentUserId).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                myClasses = result.data,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun searchClassByCode(classCode: String) {
        if (classCode.isBlank()) {
            showNotification("Vui lòng nhập mã lớp", NotificationType.ERROR)
            return
        }

        launch {
            classUseCases.getClassByCode(classCode.trim()).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        if (result.data == null) {
                            setState { copy(isLoading = false, searchedClass = null) }
                            showNotification("Không tìm thấy lớp học với mã: $classCode", NotificationType.ERROR)
                        } else {
                            setState {
                                copy(
                                    isLoading = false,
                                    searchedClass = result.data,
                                    searchCode = classCode,
                                    error = null
                                )
                            }
                            // Load enrollment status
                            loadEnrollmentStatus(result.data.id)
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun joinClass(classId: String) {
        launch {
            val currentUserId = authUseCases.getCurrentIdUser.invoke().first()
            if (currentUserId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            classUseCases.addStudentToClass(
                classId = classId,
                studentId = currentUserId,
                status = EnrollmentStatus.PENDING,
                approvedBy = ""
            ).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        if (result.data) {
                            setState {
                                copy(
                                    isLoading = false,
                                    successMessage = "Gửi yêu cầu tham gia lớp thành công! Vui lòng chờ giáo viên phê duyệt.",
                                    showJoinClassDialog = false,
                                    searchedClass = null,
                                    searchCode = ""
                                )
                            }
                            showNotification("Gửi yêu cầu thành công", NotificationType.SUCCESS)
                            loadMyClasses()
                        } else {
                            setState { copy(isLoading = false) }
                            showNotification("Bạn đã gửi yêu cầu hoặc đã tham gia lớp này", NotificationType.ERROR)
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun withdrawFromClass(classId: String) {
        launch {
            val currentUserId = authUseCases.getCurrentIdUser.invoke().first()
            if (currentUserId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            classUseCases.removeStudentFromClass(
                classId = classId,
                studentId = currentUserId
            ).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        if (result.data) {
                            setState {
                                copy(
                                    isLoading = false,
                                    successMessage = "Rời khỏi lớp thành công",
                                    showWithdrawConfirmDialog = false,
                                    selectedClass = null
                                )
                            }
                            showNotification("Rời khỏi lớp thành công", NotificationType.SUCCESS)
                            loadMyClasses()
                        } else {
                            setState { copy(isLoading = false) }
                            showNotification("Không thể rời khỏi lớp", NotificationType.ERROR)
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun selectClass(classModel: com.example.datn.domain.models.Class) {
        setState { copy(selectedClass = classModel) }
        loadEnrollmentStatus(classModel.id)
    }

    private fun loadEnrollmentStatus(classId: String) {
        launch {
            val currentUserId = authUseCases.getCurrentIdUser.invoke().first()
            if (currentUserId.isBlank()) return@launch

            classUseCases.getEnrollment(classId, currentUserId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        setState { copy(enrollment = result.data) }
                    }
                    is Resource.Error -> {
                        // Silent fail - enrollment might not exist yet
                        setState { copy(enrollment = null) }
                    }
                    is Resource.Loading -> { /* Do nothing */ }
                }
            }
        }
    }
}
