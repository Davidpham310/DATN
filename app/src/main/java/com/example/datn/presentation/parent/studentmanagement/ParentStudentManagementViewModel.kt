package com.example.datn.presentation.parent.studentmanagement

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.RelationshipType
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.domain.usecase.parentstudent.ParentStudentUseCases
import com.example.datn.domain.usecase.parentstudent.StudentSearchResult
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val isSearching: Boolean = false,
    val searchResults: List<StudentSearchResult> = emptyList(),
    val searchError: String? = null
)

@HiltViewModel
class ParentStudentManagementViewModel @Inject constructor(
    private val parentStudentUseCases: ParentStudentUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ParentStudentManagementState, ParentStudentManagementEvent>(
    ParentStudentManagementState(),
    notificationManager
) {
    
    private val currentParentIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
    
    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    init {
        loadLinkedStudents()
    }
    
    fun searchStudents(query: String) {
        viewModelScope.launch {
            parentStudentUseCases.searchStudent(query).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _searchState.value = _searchState.value.copy(isSearching = true)
                    }
                    is Resource.Success -> {
                        _searchState.value = _searchState.value.copy(
                            isSearching = false,
                            searchResults = resource.data ?: emptyList(),
                            searchError = null
                        )
                    }
                    is Resource.Error -> {
                        _searchState.value = _searchState.value.copy(
                            isSearching = false,
                            searchError = resource.message
                        )
                    }
                }
            }
        }
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadLinkedStudents() {
        viewModelScope.launch {
            currentParentIdFlow
                .filter { it.isNotBlank() }
                .flatMapLatest { parentId ->
                    parentStudentUseCases.getLinkedStudents(parentId)
                }
                .distinctUntilChanged()
                .collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            setState {
                                copy(
                                    linkedStudents = resource.data ?: emptyList(),
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            setState {
                                copy(
                                    error = resource.message,
                                    isLoading = false
                                )
                            }
                            showNotification(
                                resource.message ?: "Lỗi tải danh sách",
                                NotificationType.ERROR
                            )
                        }
                        is Resource.Loading -> {
                            setState { copy(isLoading = true, error = null) }
                        }
                    }
                }
        }
    }
    
    override fun onEvent(event: ParentStudentManagementEvent) {
        when (event) {
            is ParentStudentManagementEvent.ShowCreateStudentDialog -> {
                setState { copy(showCreateStudentDialog = true) }
            }
            ParentStudentManagementEvent.DismissCreateStudentDialog -> {
                setState { copy(showCreateStudentDialog = false) }
            }
            is ParentStudentManagementEvent.CreateStudentAccount -> {
                createStudentAccount(
                    event.name,
                    event.email,
                    event.password,
                    event.dateOfBirth,
                    event.gradeLevel
                )
            }
            is ParentStudentManagementEvent.ShowLinkStudentDialog -> {
                setState { copy(showLinkStudentDialog = true) }
            }
            ParentStudentManagementEvent.DismissLinkStudentDialog -> {
                setState { copy(showLinkStudentDialog = false) }
            }
            is ParentStudentManagementEvent.LinkStudent -> {
                linkStudent(
                    event.studentId,
                    event.relationship,
                    event.isPrimaryGuardian
                )
            }
            is ParentStudentManagementEvent.ShowEditStudentDialog -> {
                setState {
                    copy(
                        showEditStudentDialog = true,
                        editingStudent = event.studentInfo
                    )
                }
            }
            ParentStudentManagementEvent.DismissEditStudentDialog -> {
                setState {
                    copy(
                        showEditStudentDialog = false,
                        editingStudent = null
                    )
                }
            }
            is ParentStudentManagementEvent.UpdateStudentInfo -> {
                updateStudentInfo(
                    event.studentId,
                    event.name,
                    event.dateOfBirth,
                    event.gradeLevel
                )
            }
            is ParentStudentManagementEvent.UpdateRelationship -> {
                updateRelationship(
                    event.studentId,
                    event.relationship,
                    event.isPrimaryGuardian
                )
            }
            is ParentStudentManagementEvent.ShowDeleteConfirmDialog -> {
                setState {
                    copy(
                        showDeleteConfirmDialog = true,
                        deletingStudentInfo = event.studentInfo
                    )
                }
            }
            ParentStudentManagementEvent.DismissDeleteConfirmDialog -> {
                setState {
                    copy(
                        showDeleteConfirmDialog = false,
                        deletingStudentInfo = null
                    )
                }
            }
            is ParentStudentManagementEvent.ConfirmDeleteLink -> {
                unlinkStudent(event.studentId)
            }
        }
    }
    
    private fun createStudentAccount(
        name: String,
        email: String,
        password: String,
        dateOfBirth: java.time.LocalDate,
        gradeLevel: String
    ) {
        viewModelScope.launch {
            val parentId = currentParentIdFlow.value
            if (parentId.isBlank()) {
                showNotification("Không xác định được phụ huynh", NotificationType.ERROR)
                return@launch
            }
            
            parentStudentUseCases.createStudentAccount(
                com.example.datn.domain.usecase.parentstudent.CreateStudentAccountParams(
                    parentId = parentId,
                    name = name,
                    email = email,
                    password = password,
                    dateOfBirth = dateOfBirth,
                    gradeLevel = gradeLevel
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                showCreateStudentDialog = false
                            )
                        }
                        showNotification(
                            "Tạo tài khoản và liên kết học sinh thành công!",
                            NotificationType.SUCCESS
                        )
                        // Tự động tải lại danh sách
                        loadLinkedStudents()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(
                            resource.message ?: "Tạo tài khoản thất bại",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
    
    private fun linkStudent(
        studentId: String,
        relationship: RelationshipType,
        isPrimaryGuardian: Boolean
    ) {
        val parentId = currentParentIdFlow.value
        if (parentId.isBlank()) {
            showNotification("Không xác định được phụ huynh", NotificationType.ERROR)
            return
        }
        
        viewModelScope.launch {
            parentStudentUseCases.linkStudent(
                com.example.datn.domain.usecase.parentstudent.LinkStudentParams(
                    parentId = parentId,
                    studentId = studentId,
                    relationship = relationship,
                    isPrimaryGuardian = isPrimaryGuardian
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                showLinkStudentDialog = false
                            )
                        }
                        showNotification(
                            "Liên kết học sinh thành công!",
                            NotificationType.SUCCESS
                        )
                        loadLinkedStudents()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(
                            resource.message ?: "Liên kết thất bại",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
    
    private fun updateStudentInfo(
        studentId: String,
        name: String,
        dateOfBirth: java.time.LocalDate,
        gradeLevel: String
    ) {
        viewModelScope.launch {
            // Get current student info
            val studentInfo = state.value.linkedStudents.find { it.student.id == studentId }
                ?: return@launch
            
            val updatedStudent = studentInfo.student.copy(
                dateOfBirth = dateOfBirth,
                gradeLevel = gradeLevel,
                updatedAt = java.time.Instant.now()
            )
            val updatedUser = studentInfo.user.copy(
                name = name,
                updatedAt = java.time.Instant.now()
            )
            
            parentStudentUseCases.updateStudentInfo(
                com.example.datn.domain.usecase.parentstudent.UpdateStudentInfoParams(
                    studentId = studentId,
                    student = updatedStudent,
                    user = updatedUser
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                showEditStudentDialog = false,
                                editingStudent = null
                            )
                        }
                        showNotification(
                            "Cập nhật thông tin thành công!",
                            NotificationType.SUCCESS
                        )
                        loadLinkedStudents()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(
                            resource.message ?: "Cập nhật thất bại",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
    
    private fun updateRelationship(
        studentId: String,
        relationship: RelationshipType,
        isPrimaryGuardian: Boolean
    ) {
        viewModelScope.launch {
            val parentId = currentParentIdFlow.value
            if (parentId.isBlank()) {
                showNotification("Không xác định được phụ huynh", NotificationType.ERROR)
                return@launch
            }
            
            parentStudentUseCases.updateRelationship(
                com.example.datn.domain.usecase.parentstudent.UpdateRelationshipParams(
                    parentId = parentId,
                    studentId = studentId,
                    relationship = relationship,
                    isPrimaryGuardian = isPrimaryGuardian
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                showEditStudentDialog = false,
                                editingStudent = null
                            )
                        }
                        showNotification(
                            "Cập nhật quan hệ thành công!",
                            NotificationType.SUCCESS
                        )
                        loadLinkedStudents()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(
                            resource.message ?: "Cập nhật quan hệ thất bại",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
    
    private fun unlinkStudent(studentId: String) {
        val parentId = currentParentIdFlow.value
        if (parentId.isBlank()) {
            showNotification("Không xác định được phụ huynh", NotificationType.ERROR)
            return
        }
        
        viewModelScope.launch {
            parentStudentUseCases.unlinkStudent(
                com.example.datn.domain.usecase.parentstudent.UnlinkStudentParams(
                    parentId = parentId,
                    studentId = studentId
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                showDeleteConfirmDialog = false,
                                deletingStudentInfo = null
                            )
                        }
                        showNotification(
                            "Hủy liên kết học sinh thành công!",
                            NotificationType.SUCCESS
                        )
                        loadLinkedStudents()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(
                            resource.message ?: "Hủy liên kết thất bại",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
}
