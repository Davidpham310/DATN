package com.example.datn.presentation.teacher.enrollment

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.models.User
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.domain.usecase.user.UserUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnrollmentManagementViewModel @Inject constructor(
    private val classUseCases: ClassUseCases,
    private val userUseCases: UserUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<EnrollmentManagementState, EnrollmentManagementEvent>(
    EnrollmentManagementState(),
    notificationManager
) {

    // Cache current teacher ID
    private val currentTeacherIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    override fun onEvent(event: EnrollmentManagementEvent) {
        when (event) {
            is EnrollmentManagementEvent.LoadPendingEnrollments -> loadPendingEnrollments(event.classId)
            is EnrollmentManagementEvent.SelectEnrollment -> setState { copy(selectedEnrollment = event.enrollment) }
            is EnrollmentManagementEvent.ShowApproveDialog -> setState { copy(showApproveDialog = true) }
            is EnrollmentManagementEvent.ShowRejectDialog -> setState { copy(showRejectDialog = true) }
            is EnrollmentManagementEvent.DismissApproveDialog -> setState { copy(showApproveDialog = false) }
            is EnrollmentManagementEvent.DismissRejectDialog -> setState { 
                copy(
                    showRejectDialog = false,
                    rejectionReason = ""
                ) 
            }
            
            is EnrollmentManagementEvent.ApproveEnrollment -> approveEnrollment(event.classId, event.studentId)
            is EnrollmentManagementEvent.RejectEnrollment -> rejectEnrollment(
                event.classId,
                event.studentId,
                event.reason
            )
            is EnrollmentManagementEvent.UpdateRejectionReason -> setState { copy(rejectionReason = event.reason) }
            
            is EnrollmentManagementEvent.BatchApproveAll -> batchApproveAll(event.classId)
            
            is EnrollmentManagementEvent.UpdateSearchQuery -> {
                setState { copy(searchQuery = event.query) }
                filterEnrollments()
            }
            is EnrollmentManagementEvent.UpdateSortType -> {
                setState { copy(sortBy = event.sortType) }
                sortEnrollments()
            }
            
            is EnrollmentManagementEvent.Refresh -> {
                val classId = state.value.classId
                if (classId.isNotBlank()) {
                    loadPendingEnrollments(classId)
                }
            }
            
            is EnrollmentManagementEvent.ClearMessages -> setState { 
                copy(
                    successMessage = null,
                    error = null
                ) 
            }
        }
    }

    private fun loadPendingEnrollments(classId: String) {
        launch {
            android.util.Log.d("EnrollmentVM", "==== START loadPendingEnrollments ====")
            android.util.Log.d("EnrollmentVM", "ClassId: $classId")
            setState { copy(isLoading = true, classId = classId) }
            
            classUseCases.getPendingEnrollments(classId).collectLatest { result ->
                android.util.Log.d("EnrollmentVM", "getPendingEnrollments result: ${result::class.simpleName}")
                when (result) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val enrollments = result.data ?: emptyList()
                        android.util.Log.d("EnrollmentVM", "Received ${enrollments.size} pending enrollments")
                        enrollments.forEachIndexed { index, enrollment ->
                            android.util.Log.d("EnrollmentVM", "  [$index] studentId: ${enrollment.studentId}, status: ${enrollment.enrollmentStatus}")
                        }
                        
                        // Load student info for each enrollment
                        val enrollmentsWithStudents = loadStudentInfoForEnrollments(enrollments)
                        android.util.Log.d("EnrollmentVM", "Loaded student info for ${enrollmentsWithStudents.size} enrollments")
                        
                        setState {
                            copy(
                                isLoading = false,
                                pendingEnrollments = enrollmentsWithStudents,
                                error = null
                            )
                        }
                        
                        if (enrollmentsWithStudents.isEmpty()) {
                            android.util.Log.w("EnrollmentVM", "No enrollments with student info to display")
                            showNotification("Không có yêu cầu chờ duyệt", NotificationType.INFO)
                        } else {
                            android.util.Log.d("EnrollmentVM", "Successfully loaded ${enrollmentsWithStudents.size} enrollments to display")
                        }
                    }
                    is Resource.Error -> {
                        setState { 
                            copy(
                                isLoading = false,
                                error = result.message
                            ) 
                        }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                    else -> {
                        android.util.Log.w("EnrollmentVM", "Unknown result type: ${result::class.simpleName}")
                    }
                }
            }
        }
    }

    private suspend fun loadStudentInfoForEnrollments(
        enrollments: List<ClassStudent>
    ): List<EnrollmentWithStudent> {
        android.util.Log.d("EnrollmentVM", "loadStudentInfoForEnrollments: Processing ${enrollments.size} enrollments")
        val enrollmentsWithStudents = mutableListOf<EnrollmentWithStudent>()
        
        for ((index, enrollment) in enrollments.withIndex()) {
            android.util.Log.d("EnrollmentVM", "  Processing enrollment [$index]: studentId=${enrollment.studentId}")
            try {
                // Get User from studentId using UseCase (handles Student -> User lookup)
                var userResult: Resource<User?>? = null
                
                userUseCases.getStudentUser(enrollment.studentId).collect { result ->
                    android.util.Log.d("EnrollmentVM", "    getStudentUser emit: ${result::class.simpleName}")
                    
                    when (result) {
                        is Resource.Loading -> {
                            // Skip loading state
                        }
                        is Resource.Success -> {
                            userResult = result
                            return@collect  // Stop collecting after Success
                        }
                        is Resource.Error -> {
                            userResult = result
                            return@collect  // Stop collecting after Error
                        }
                        else -> {
                            android.util.Log.w("EnrollmentVM", "    ⚠️ Unknown result type: ${result::class.simpleName}")
                        }
                    }
                }
                
                android.util.Log.d("EnrollmentVM", "    getStudentUser final result: ${userResult?.javaClass?.simpleName ?: "null"}")
                
                when (userResult) {
                    is Resource.Success -> {
                        val user = (userResult as Resource.Success<User?>).data
                        if (user != null) {
                            android.util.Log.d("EnrollmentVM", "    ✅ Found user: ${user.name} (${user.email})")
                            enrollmentsWithStudents.add(
                                EnrollmentWithStudent(
                                    enrollment = enrollment,
                                    studentInfo = user
                                )
                            )
                        } else {
                            android.util.Log.e("EnrollmentVM", 
                                "    ❌ User data is NULL for studentId: ${enrollment.studentId}")
                            showNotification(
                                "Không tìm thấy thông tin học sinh (ID: ${enrollment.studentId.take(8)})",
                                NotificationType.ERROR
                            )
                        }
                    }
                    is Resource.Error -> {
                        val errorMsg = (userResult as Resource.Error<User?>).message
                        android.util.Log.e("EnrollmentVM", 
                            "    ❌ ERROR loading user for studentId ${enrollment.studentId}: $errorMsg")
                        showNotification(
                            "Lỗi tải thông tin học sinh: $errorMsg",
                            NotificationType.ERROR
                        )
                    }
                    null -> {
                        android.util.Log.e("EnrollmentVM", 
                            "    ❌ No result received for studentId: ${enrollment.studentId}")
                    }
                    else -> {
                        android.util.Log.w("EnrollmentVM", 
                            "    ⚠️ Unexpected userResult type: ${userResult?.javaClass?.simpleName}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EnrollmentVM", 
                    "    ❌ EXCEPTION loading student ${enrollment.studentId}: ${e.message}", e)
                showNotification(
                    "Lỗi: ${e.message}",
                    NotificationType.ERROR
                )
            }
        }
        
        android.util.Log.d("EnrollmentVM", "loadStudentInfoForEnrollments: Returning ${enrollmentsWithStudents.size} enrollments")
        return enrollmentsWithStudents
    }

    private fun approveEnrollment(classId: String, studentId: String) {
        launch {
            val teacherId = currentTeacherIdFlow.value
            if (teacherId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            classUseCases.approveEnrollment(
                classId = classId,
                studentId = studentId,
                approvedBy = teacherId
            ).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        if (result.data == true) {
                            setState {
                                copy(
                                    isLoading = false,
                                    showApproveDialog = false,
                                    selectedEnrollment = null,
                                    successMessage = "Đã phê duyệt yêu cầu tham gia"
                                )
                            }
                            showNotification("Phê duyệt thành công", NotificationType.SUCCESS)
                            
                            // Reload pending enrollments
                            loadPendingEnrollments(classId)
                        } else {
                            setState { copy(isLoading = false) }
                            showNotification("Không thể phê duyệt yêu cầu", NotificationType.ERROR)
                        }
                    }
                    is Resource.Error -> {
                        setState { 
                            copy(
                                isLoading = false,
                                error = result.message
                            ) 
                        }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                    else -> {
                        android.util.Log.w("EnrollmentVM", "Unknown result type in approveEnrollment")
                    }
                }
            }
        }
    }

    private fun rejectEnrollment(classId: String, studentId: String, reason: String) {
        if (reason.isBlank()) {
            showNotification("Vui lòng nhập lý do từ chối", NotificationType.ERROR)
            return
        }

        launch {
            val teacherId = currentTeacherIdFlow.value
            if (teacherId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            classUseCases.rejectEnrollment(
                classId = classId,
                studentId = studentId,
                rejectionReason = reason,
                rejectedBy = teacherId
            ).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        if (result.data == true) {
                            setState {
                                copy(
                                    isLoading = false,
                                    showRejectDialog = false,
                                    selectedEnrollment = null,
                                    rejectionReason = "",
                                    successMessage = "Đã từ chối yêu cầu tham gia"
                                )
                            }
                            showNotification("Từ chối thành công", NotificationType.SUCCESS)
                            
                            // Reload pending enrollments
                            loadPendingEnrollments(classId)
                        } else {
                            setState { copy(isLoading = false) }
                            showNotification("Không thể từ chối yêu cầu", NotificationType.ERROR)
                        }
                    }
                    is Resource.Error -> {
                        setState { 
                            copy(
                                isLoading = false,
                                error = result.message
                            ) 
                        }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun batchApproveAll(classId: String) {
        launch {
            val teacherId = currentTeacherIdFlow.value
            if (teacherId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            val studentIds = state.value.pendingEnrollments.map { it.enrollment.studentId }
            if (studentIds.isEmpty()) {
                showNotification("Không có yêu cầu để phê duyệt", NotificationType.INFO)
                return@launch
            }

            classUseCases.batchApproveEnrollments(
                classId = classId,
                studentIds = studentIds,
                approvedBy = teacherId
            ).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        val successCount = result.data?.count { it } ?: 0
                        if (successCount > 0) {
                            setState {
                                copy(
                                    isLoading = false,
                                    successMessage = "Đã phê duyệt tất cả yêu cầu"
                                )
                            }
                            showNotification(
                                "Phê duyệt $successCount/${studentIds.size} yêu cầu thành công",
                                NotificationType.SUCCESS
                            )
                            
                            // Reload pending enrollments
                            loadPendingEnrollments(classId)
                        } else {
                            setState { copy(isLoading = false) }
                            showNotification("Không thể phê duyệt các yêu cầu", NotificationType.ERROR)
                        }
                    }
                    is Resource.Error -> {
                        setState { 
                            copy(
                                isLoading = false,
                                error = result.message
                            ) 
                        }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun filterEnrollments() {
        val query = state.value.searchQuery.lowercase()
        if (query.isBlank()) {
            // Reset to show all (already handled by state)
            return
        }
        
        // Filter logic will be applied in UI composable
        // This is just to trigger recomposition
        setState { copy(searchQuery = query) }
    }

    private fun sortEnrollments() {
        // Sort logic will be applied in UI composable
        // This is just to trigger recomposition
        val currentSort = state.value.sortBy
        setState { copy(sortBy = currentSort) }
    }
}
