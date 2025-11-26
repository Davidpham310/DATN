package com.example.datn.presentation.teacher.classmembers

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.domain.usecase.user.UserUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Class Members Screen
 * Manages the list of approved students in a class
 */
@HiltViewModel
class ClassMembersViewModel @Inject constructor(
    private val classUseCases: ClassUseCases,
    private val userUseCases: UserUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ClassMembersState, ClassMembersEvent>(
    ClassMembersState(),
    notificationManager
) {

    override fun onEvent(event: ClassMembersEvent) {
        when (event) {
            is ClassMembersEvent.LoadMembers -> loadMembers(event.classId, event.className)
            is ClassMembersEvent.SearchStudents -> searchStudents(event.query)
            is ClassMembersEvent.Refresh -> refreshMembers()
            is ClassMembersEvent.ClearError -> setState { copy(error = null) }
        }
    }

    private fun loadMembers(classId: String, className: String) {
        launch {
            setState { copy(isLoading = true, classId = classId, className = className) }

            classUseCases.getApprovedStudentsInClass(classId).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val studentIds = result.data ?: emptyList()

                        // Load full user information (User) together với studentId
                        val students = loadStudentUsers(studentIds.map { it.studentId })

                        setState {
                            copy(
                                isLoading = false,
                                students = students,
                                totalStudents = students.size,
                                error = null
                            )
                        }
                        
                        if (students.isEmpty()) {
                            showNotification("Chưa có học sinh nào trong lớp", NotificationType.INFO)
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
                        android.util.Log.w("ClassMembersVM", "Unknown result type")
                    }
                }
            }
        }
    }

    private suspend fun loadStudentUsers(studentIds: List<String>): List<ClassMemberUi> {
        val users = mutableListOf<ClassMemberUi>()

        for (studentId in studentIds) {
            try {
                var userResult: Resource<com.example.datn.domain.models.User?>? = null

                userUseCases.getStudentUser(studentId).collect { result ->
                    when (result) {
                        is Resource.Loading -> { /* Skip */ }
                        is Resource.Success -> {
                            userResult = result
                            return@collect
                        }
                        is Resource.Error -> {
                            userResult = result
                            return@collect
                        }
                        else -> { /* Skip */ }
                    }
                }

                when (userResult) {
                    is Resource.Success -> {
                        val user = (userResult as Resource.Success<com.example.datn.domain.models.User?>).data
                        if (user != null) {
                            users.add(
                                ClassMemberUi(
                                    studentId = studentId,
                                    user = user
                                )
                            )
                        }
                    }
                    else -> { /* Skip failed users */ }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClassMembersVM", "Error loading student $studentId", e)
            }
        }

        return users.sortedBy { it.user.name }
    }

    private fun searchStudents(query: String) {
        setState { copy(searchQuery = query) }
    }

    private fun refreshMembers() {
        val classId = state.value.classId
        val className = state.value.className
        if (classId.isNotEmpty()) {
            loadMembers(classId, className)
        }
    }
    
    /**
     * Get filtered students based on search query
     */
    fun getFilteredStudents(): List<ClassMemberUi> {
        val query = state.value.searchQuery.lowercase()
        if (query.isBlank()) return state.value.students

        return state.value.students.filter {
            it.user.name.lowercase().contains(query) ||
                    it.user.email.lowercase().contains(query)
        }
    }
}
