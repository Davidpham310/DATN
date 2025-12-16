package com.example.datn.presentation.parent.classlist.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.domain.usecase.parentstudent.ParentStudentUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.parent.classlist.event.ParentJoinClassEvent
import com.example.datn.presentation.parent.classlist.state.ParentJoinClassState
import com.example.datn.presentation.parent.classlist.state.SearchType
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
class ParentJoinClassViewModel @Inject constructor(
    private val classUseCases: ClassUseCases,
    private val parentStudentUseCases: ParentStudentUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ParentJoinClassState, ParentJoinClassEvent>(ParentJoinClassState(), notificationManager) {

    private suspend fun <T> awaitFirstNonLoading(flow: Flow<Resource<T>>): Resource<T> {
        var result: Resource<T>? = null
        flow
            .filter { it !is Resource.Loading }
            .take(1)
            .collect { value -> result = value }
        return result ?: Resource.Error("Không thể tải dữ liệu")
    }

    // Cache current parent ID to avoid repeated authentication checks
    private val currentParentIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    init {
        loadLinkedStudents()
    }

    override fun onEvent(event: ParentJoinClassEvent) {
        when (event) {
            is ParentJoinClassEvent.LoadLinkedStudents -> loadLinkedStudents()
            is ParentJoinClassEvent.SelectStudent -> setState { copy(selectedStudent = event.student) }
            is ParentJoinClassEvent.ShowStudentSelectionDialog -> setState { copy(showStudentSelectionDialog = true) }
            is ParentJoinClassEvent.DismissStudentSelectionDialog -> setState { copy(showStudentSelectionDialog = false) }
            
            is ParentJoinClassEvent.UpdateSearchQuery -> setState { copy(searchQuery = event.query) }
            is ParentJoinClassEvent.UpdateSearchType -> setState { copy(searchType = event.type) }
            is ParentJoinClassEvent.SearchClasses -> searchClasses()
            is ParentJoinClassEvent.ClearSearch -> setState { 
                copy(
                    searchQuery = "",
                    searchResults = emptyList(),
                    selectedClass = null,
                    enrollment = null
                ) 
            }
            
            is ParentJoinClassEvent.SelectClass -> {
                setState { copy(selectedClass = event.classModel) }
                // Auto load enrollment status if student is selected
                state.value.selectedStudent?.let { student ->
                    loadEnrollmentStatus(event.classModel.id, student.student.id)
                }
            }
            is ParentJoinClassEvent.ShowClassDetailsDialog -> setState { copy(showClassDetailsDialog = true) }
            is ParentJoinClassEvent.DismissClassDetailsDialog -> setState { copy(showClassDetailsDialog = false) }
            is ParentJoinClassEvent.LoadEnrollmentStatus -> loadEnrollmentStatus(event.classId, event.studentId)
            
            is ParentJoinClassEvent.JoinClass -> joinClass(event.classId, event.studentId)
            
            is ParentJoinClassEvent.ClearMessages -> setState { copy(successMessage = null, error = null) }
        }
    }

    private fun loadLinkedStudents() {
        launch {
            currentParentIdFlow
                .filter { it.isNotBlank() }
                .collectLatest { parentId ->
                    parentStudentUseCases.getLinkedStudents(parentId).collectLatest { result ->
                        when (result) {
                            is Resource.Loading -> setState { copy(isLoading = true) }
                            is Resource.Success -> {
                                val students = result.data ?: emptyList()
                                setState {
                                    copy(
                                        isLoading = false,
                                        linkedStudents = students,
                                        // Auto select first student if available and no student selected
                                        selectedStudent = if (selectedStudent == null && students.isNotEmpty()) {
                                            students.getOrNull(0)
                                        } else {
                                            selectedStudent
                                        },
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
    }

    private fun searchClasses() {
        val query = state.value.searchQuery.trim()
        if (query.isBlank()) {
            showNotification("Vui lòng nhập từ khóa tìm kiếm", NotificationType.ERROR)
            return
        }

        launch {
            when (state.value.searchType) {
                SearchType.BY_CODE -> searchByCode(query)
                SearchType.BY_NAME -> searchByName(query)
                SearchType.BY_SUBJECT -> searchBySubject(query)
            }
        }
    }

    private suspend fun searchByCode(classCode: String) {
        classUseCases.getClassByCode(classCode).collectLatest { result ->
            when (result) {
                is Resource.Loading -> setState { copy(isLoading = true) }
                is Resource.Success -> {
                    val classes = if (result.data != null) listOf(result.data) else emptyList()
                    setState {
                        copy(
                            isLoading = false,
                            searchResults = classes,
                            error = null
                        )
                    }
                    if (classes.isEmpty()) {
                        showNotification("Không tìm thấy lớp học với mã: $classCode", NotificationType.ERROR)
                    } else {
                        // Load enrollments for selected student
                        loadAllStudentEnrollments()
                    }
                }
                is Resource.Error -> {
                    setState { copy(isLoading = false, error = result.message) }
                    showNotification(result.message, NotificationType.ERROR)
                }
            }
        }
    }

    private suspend fun searchByName(name: String) {
        classUseCases.getAllClasses().collectLatest { result ->
            when (result) {
                is Resource.Loading -> setState { copy(isLoading = true) }
                is Resource.Success -> {
                    val filteredClasses = result.data.filter { classItem ->
                        classItem.name.contains(name, ignoreCase = true)
                    }
                    setState {
                        copy(
                            isLoading = false,
                            searchResults = filteredClasses,
                            error = null
                        )
                    }
                    if (filteredClasses.isEmpty()) {
                        showNotification("Không tìm thấy lớp học với tên: $name", NotificationType.ERROR)
                    }
                }
                is Resource.Error -> {
                    setState { copy(isLoading = false, error = result.message) }
                    showNotification(result.message, NotificationType.ERROR)
                }
            }
        }
    }

    private suspend fun searchBySubject(subject: String) {
        classUseCases.getAllClasses().collectLatest { result ->
            when (result) {
                is Resource.Loading -> setState { copy(isLoading = true) }
                is Resource.Success -> {
                    val filteredClasses = result.data.filter { classItem ->
                        classItem.subject?.contains(subject, ignoreCase = true) == true
                    }
                    setState {
                        copy(
                            isLoading = false,
                            searchResults = filteredClasses,
                            error = null
                        )
                    }
                    if (filteredClasses.isEmpty()) {
                        showNotification("Không tìm thấy lớp học với môn: $subject", NotificationType.ERROR)
                    }
                }
                is Resource.Error -> {
                    setState { copy(isLoading = false, error = result.message) }
                    showNotification(result.message, NotificationType.ERROR)
                }
            }
        }
    }

    private fun loadEnrollmentStatus(classId: String, studentId: String) {
        launch {
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
    }

    private fun joinClass(classId: String, studentId: String) {
        launch {
            // Validate student selection
            val student = state.value.selectedStudent
            if (student == null) {
                showNotification("Vui lòng chọn học sinh", NotificationType.ERROR)
                return@launch
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
                                    showClassDetailsDialog = false
                                )
                            }
                            showNotification("Gửi yêu cầu thành công", NotificationType.SUCCESS)
                            // Reload enrollment status
                            loadEnrollmentStatus(classId, studentId)
                        } else {
                            setState { copy(isLoading = false) }
                            showNotification("Học sinh đã gửi yêu cầu hoặc đã tham gia lớp này", NotificationType.ERROR)
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
    
    private fun loadAllStudentEnrollments() {
        // Load enrollments for each class in search results
        launch {
            val studentId = state.value.selectedStudent?.student?.id ?: return@launch
            val enrollmentMap = mutableMapOf<String, ClassStudent>()
            
            state.value.searchResults.forEach { classItem ->
                awaitFirstNonLoading(classUseCases.getEnrollment(classItem.id, studentId)).let { result ->
                    if (result is Resource.Success && result.data != null) {
                        enrollmentMap[classItem.id] = result.data
                    }
                }
            }
            
            setState { copy(studentEnrollments = enrollmentMap) }
        }
    }
}
