package com.example.datn.presentation.student.classmanager.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.student.classmanager.event.StudentClassEvent
import com.example.datn.presentation.student.classmanager.state.StudentClassState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentClassViewModel @Inject constructor(
    private val classUseCases: ClassUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentClassState, StudentClassEvent>(StudentClassState(), notificationManager) {

    // Cache current user ID to avoid timing issues
    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

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

    private suspend fun awaitNonBlank(flow: Flow<String>): String {
        var result = ""
        flow
            .filter { it.isNotBlank() }
            .take(1)
            .collect { value -> result = value }
        return result
    }

    private fun loadMyClasses() {
        launch {
            // Sử dụng cached StateFlow để tránh timing issues
            val currentUserId = currentUserIdFlow.value.ifBlank {
                awaitNonBlank(currentUserIdFlow)
            }
            if (currentUserId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            // Lấy student profile ID từ user ID
            getStudentProfileByUserId(currentUserId).collectLatest { profileResult ->
                when (profileResult) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        val studentId = profileResult.data?.id
                        if (studentId.isNullOrBlank()) {
                            setState { copy(isLoading = false, error = "Không tìm thấy thông tin học sinh") }
                            showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                            return@collectLatest
                        }

                        // Lấy danh sách lớp học bằng student profile ID
                        classUseCases.getClassesByStudent(studentId).collectLatest { result ->
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
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = profileResult.message) }
                        showNotification(profileResult.message, NotificationType.ERROR)
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
            // Sử dụng cached StateFlow
            val currentUserId = currentUserIdFlow.value.ifBlank {
                awaitNonBlank(currentUserIdFlow)
            }
            if (currentUserId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            // Lấy student profile ID từ user ID
            getStudentProfileByUserId(currentUserId).collectLatest { profileResult ->
                when (profileResult) {
                    is Resource.Success -> {
                        val studentId = profileResult.data?.id
                        if (studentId.isNullOrBlank()) {
                            showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                            return@collectLatest
                        }

                        classUseCases.addStudentToClass(
                            classId = classId,
                            studentId = studentId,
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
                    is Resource.Error -> {
                        showNotification(profileResult.message, NotificationType.ERROR)
                    }
                    is Resource.Loading -> { /* Ignore */ }
                }
            }
        }
    }

    private fun withdrawFromClass(classId: String) {
        launch {
            // Sử dụng cached StateFlow
            val currentUserId = currentUserIdFlow.value.ifBlank {
                awaitNonBlank(currentUserIdFlow)
            }
            if (currentUserId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            // Lấy student profile ID từ user ID
            getStudentProfileByUserId(currentUserId).collectLatest { profileResult ->
                when (profileResult) {
                    is Resource.Success -> {
                        val studentId = profileResult.data?.id
                        if (studentId.isNullOrBlank()) {
                            showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                            return@collectLatest
                        }

                        classUseCases.removeStudentFromClass(
                            classId = classId,
                            studentId = studentId
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
                    is Resource.Error -> {
                        showNotification(profileResult.message, NotificationType.ERROR)
                    }
                    is Resource.Loading -> { /* Ignore */ }
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
            // Sử dụng cached StateFlow
            val currentUserId = currentUserIdFlow.value.ifBlank {
                awaitNonBlank(currentUserIdFlow)
            }
            if (currentUserId.isBlank()) return@launch

            // Lấy student profile ID từ user ID
            getStudentProfileByUserId(currentUserId).collectLatest { profileResult ->
                when (profileResult) {
                    is Resource.Success -> {
                        val studentId = profileResult.data?.id
                        if (studentId.isNullOrBlank()) {
                            setState { copy(enrollment = null) }
                            return@collectLatest
                        }

                        classUseCases.getEnrollment(classId, studentId).collectLatest { result ->
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
                    is Resource.Error -> {
                        setState { copy(enrollment = null) }
                    }
                    is Resource.Loading -> { /* Do nothing */ }
                }
            }
        }
    }
}
