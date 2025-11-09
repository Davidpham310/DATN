package com.example.datn.presentation.parent.classlist

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.ParentStudentUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho màn hình danh sách lớp học của phụ huynh
 */
@HiltViewModel
class ParentClassListViewModel @Inject constructor(
    private val parentStudentUseCases: ParentStudentUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ParentClassListState, ParentClassListEvent>(
    ParentClassListState(),
    notificationManager
) {

    private val currentParentIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    init {
        android.util.Log.d("ParentClassListVM", "🚀 ViewModel initialized - Loading data...")
        
        // Log current parentId để debug
        viewModelScope.launch {
            currentParentIdFlow.collect { parentId ->
                android.util.Log.d("ParentClassListVM", "👤 Current parentId changed: '$parentId' (isBlank: ${parentId.isBlank()})")
                if (parentId.isNotBlank()) {
                    android.util.Log.i("ParentClassListVM", "✅ ParentId available - Reloading data...")
                    loadLinkedStudents()
                    loadClasses()
                }
            }
        }
    }

    override fun onEvent(event: ParentClassListEvent) {
        when (event) {
            is ParentClassListEvent.LoadLinkedStudents -> loadLinkedStudents()
            is ParentClassListEvent.LoadClasses -> loadClasses()
            is ParentClassListEvent.Refresh -> refresh()
            is ParentClassListEvent.FilterByStudent -> filterByStudent(event.studentId)
            is ParentClassListEvent.FilterByEnrollmentStatus -> filterByStatus(event.status)
            is ParentClassListEvent.ClearFilters -> clearFilters()
            is ParentClassListEvent.ToggleFilterDialog -> toggleFilterDialog()
            is ParentClassListEvent.NavigateToClassDetail -> {
                // Handle navigation in composable
            }
            is ParentClassListEvent.NavigateToStudentProfile -> {
                // Handle navigation in composable
            }
        }
    }

    /**
     * Load danh sách con của phụ huynh
     */
    private fun loadLinkedStudents() {
        viewModelScope.launch {
            val parentId = currentParentIdFlow.value
            if (parentId.isBlank()) {
                android.util.Log.w("ParentClassListVM", "⚠️ Cannot load students: parentId is blank")
                return@launch
            }

            android.util.Log.d("ParentClassListVM", "👨‍👩‍👧 Loading linked students for parent: $parentId")

            parentStudentUseCases.getLinkedStudents(parentId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        android.util.Log.d("ParentClassListVM", "⏳ Loading students...")
                        setState { copy(isLoadingStudents = true, studentsError = null) }
                    }
                    is Resource.Success -> {
                        val students = resource.data?.map { it.student } ?: emptyList()
                        android.util.Log.i("ParentClassListVM", "✅ Loaded ${students.size} linked students")
                        students.forEachIndexed { index, student ->
                            android.util.Log.d("ParentClassListVM", "  [$index] StudentID: ${student.id}, UserID: ${student.userId}, Grade: ${student.gradeLevel}")
                        }
                        setState {
                            copy(
                                linkedStudents = students,
                                isLoadingStudents = false,
                                studentsError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        setState {
                            copy(
                                isLoadingStudents = false,
                                studentsError = resource.message
                            )
                        }
                        showNotification(
                            message = resource.message ?: "Không thể tải danh sách con",
                            type = com.example.datn.presentation.common.notifications.NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }

    /**
     * Load danh sách lớp học
     */
    private fun loadClasses() {
        viewModelScope.launch {
            val parentId = currentParentIdFlow.value
            if (parentId.isBlank()) {
                android.util.Log.w("ParentClassListVM", "⚠️ Cannot load classes: parentId is blank")
                return@launch
            }

            val studentId = state.value.selectedStudentId
            val enrollmentStatus = state.value.selectedEnrollmentStatus

            android.util.Log.d("ParentClassListVM", "🔄 Loading classes for parent: $parentId, student: $studentId, status: $enrollmentStatus")

            parentStudentUseCases.getStudentClassesForParent(
                parentId = parentId,
                studentId = studentId,
                enrollmentStatus = enrollmentStatus
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        android.util.Log.d("ParentClassListVM", "⏳ Loading classes...")
                        setState { copy(isLoadingClasses = true, classesError = null) }
                    }
                    is Resource.Success -> {
                        val classes = resource.data ?: emptyList()
                        android.util.Log.i("ParentClassListVM", "✅ Loaded ${classes.size} classes successfully")
                        classes.forEachIndexed { index, enrollment ->
                            android.util.Log.d("ParentClassListVM", "  [$index] ${enrollment.className} - ${enrollment.enrollmentStatus} - ${enrollment.studentName}")
                        }
                        setState {
                            copy(
                                classEnrollments = classes,
                                isLoadingClasses = false,
                                classesError = null,
                                refreshing = false
                            )
                        }
                    }
                    is Resource.Error -> {
                        android.util.Log.e("ParentClassListVM", "❌ Error loading classes: ${resource.message}")
                        setState {
                            copy(
                                isLoadingClasses = false,
                                classesError = resource.message,
                                refreshing = false
                            )
                        }
                        showNotification(
                            message = resource.message ?: "Không thể tải danh sách lớp học",
                            type = com.example.datn.presentation.common.notifications.NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }

    /**
     * Refresh toàn bộ dữ liệu
     */
    private fun refresh() {
        setState { copy(refreshing = true) }
        loadLinkedStudents()
        loadClasses()
    }

    /**
     * Filter theo học sinh
     */
    private fun filterByStudent(studentId: String?) {
        setState { copy(selectedStudentId = studentId) }
        loadClasses()
    }

    /**
     * Filter theo trạng thái enrollment
     */
    private fun filterByStatus(status: EnrollmentStatus?) {
        setState { copy(selectedEnrollmentStatus = status) }
        loadClasses()
    }

    /**
     * Xóa tất cả filter
     */
    private fun clearFilters() {
        setState {
            copy(
                selectedStudentId = null,
                selectedEnrollmentStatus = null
            )
        }
        loadClasses()
    }

    /**
     * Toggle filter dialog
     */
    private fun toggleFilterDialog() {
        setState { copy(showFilterDialog = !showFilterDialog) }
    }
}
