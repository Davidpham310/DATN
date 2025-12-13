package com.example.datn.presentation.student.lessons.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.domain.usecase.lesson.GetLessonListRequest
import com.example.datn.domain.usecase.lesson.LessonUseCases
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.student.lessons.state.StudentClassDetailState
import com.example.datn.presentation.student.lessons.event.StudentClassDetailEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentClassDetailViewModel @Inject constructor(
    private val classUseCases: ClassUseCases,
    private val lessonUseCases: LessonUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentClassDetailState, StudentClassDetailEvent>(
    StudentClassDetailState(),
    notificationManager
) {

    private val currentClassIdFlow = MutableStateFlow("")
    private val currentStudentIdFlow = MutableStateFlow("")
    
    // Cache current user ID
    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    override fun onEvent(event: StudentClassDetailEvent) {
        when (event) {
            is StudentClassDetailEvent.LoadClassDetail -> loadClassDetail(event.classId)
            is StudentClassDetailEvent.NavigateToLesson -> {
                // Navigation handled by screen
            }
            StudentClassDetailEvent.ShowWithdrawDialog -> {
                setState { copy(showWithdrawDialog = true) }
            }
            StudentClassDetailEvent.DismissWithdrawDialog -> {
                setState { copy(showWithdrawDialog = false) }
            }
            StudentClassDetailEvent.ConfirmWithdraw -> withdrawFromClass()
        }
    }

    private fun loadClassDetail(classId: String) {
        currentClassIdFlow.value = classId
        
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            
            // Get student ID first
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.first { it.isNotBlank() }
            }
            
            getStudentProfileByUserId(currentUserId).collect { profileResult ->
                when (profileResult) {
                    is Resource.Success -> {
                        val studentId = profileResult.data?.id
                        if (studentId.isNullOrBlank()) {
                            setState { copy(isLoading = false, error = "Không tìm thấy thông tin học sinh") }
                            return@collect
                        }
                        currentStudentIdFlow.value = studentId
                        
                        // Fetch class info, lessons (with status), and students
                        combine(
                            classUseCases.getClassById(classId),
                            lessonUseCases.getLessonList(
                                GetLessonListRequest(
                                    studentId = studentId,
                                    classId = classId
                                )
                            ),
                            classUseCases.getStudentsInClass(classId, null)
                        ) { classResult, lessonsResult, studentsResult ->
                            Triple(classResult, lessonsResult, studentsResult)
                        }.collect { (classResult, lessonsResult, studentsResult) ->
                            when {
                                classResult is Resource.Loading || lessonsResult is Resource.Loading -> {
                                    setState { copy(isLoading = true) }
                                }
                                classResult is Resource.Success && lessonsResult is Resource.Success -> {
                                    val classData = classResult.data
                                    val lessonsData = lessonsResult.data ?: emptyList()
                                    val studentCount = if (studentsResult is Resource.Success) {
                                        studentsResult.data?.filter { it.enrollmentStatus.name == "APPROVED" }?.size ?: 0
                                    } else 0

                                    // Build progress map from lesson list status
                                    val progressMap = lessonsData
                                        .mapNotNull { wrapper ->
                                            val progress = wrapper.progress ?: return@mapNotNull null
                                            wrapper.lesson.id to progress
                                        }
                                        .toMap()

                                    // Load lesson content counts per lesson to support X/Y progress display
                                    val contentCounts: Map<String, Int> = if (lessonsData.isNotEmpty()) {
                                        val map = mutableMapOf<String, Int>()
                                        for (wrapper in lessonsData) {
                                            val lesson = wrapper.lesson
                                            try {
                                                val contentsRes = lessonUseCases
                                                    .getLessonContentsByLesson(lesson.id)
                                                    .first { it !is Resource.Loading }

                                                val count = when (contentsRes) {
                                                    is Resource.Success -> contentsRes.data?.size ?: 0
                                                    is Resource.Error -> {
                                                        showNotification(contentsRes.message, NotificationType.ERROR)
                                                        0
                                                    }
                                                    else -> 0
                                                }
                                                map[lesson.id] = count
                                            } catch (e: Exception) {
                                                showNotification(e.message.toString(), NotificationType.ERROR)
                                            }
                                        }
                                        map
                                    } else {
                                        emptyMap()
                                    }

                                    setState {
                                        copy(
                                            classInfo = classData,
                                            lessons = lessonsData,
                                            studentCount = studentCount,
                                            lessonProgress = progressMap,
                                            lessonContentCounts = contentCounts,
                                            isLoading = false,
                                            error = null
                                        )
                                    }
                                }
                                classResult is Resource.Error -> {
                                    setState {
                                        copy(
                                            isLoading = false,
                                            error = classResult.message
                                        )
                                    }
                                    showNotification(classResult.message, NotificationType.ERROR)
                                }
                                lessonsResult is Resource.Error -> {
                                    setState {
                                        copy(
                                            isLoading = false,
                                            error = lessonsResult.message
                                        )
                                    }
                                    showNotification(lessonsResult.message, NotificationType.ERROR)
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = profileResult.message) }
                        showNotification(profileResult.message, NotificationType.ERROR)
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun withdrawFromClass() {
        viewModelScope.launch {
            val classId = currentClassIdFlow.value
            val studentId = currentStudentIdFlow.value
            
            if (classId.isBlank() || studentId.isBlank()) {
                showNotification("Không thể rời khỏi lớp", NotificationType.ERROR)
                return@launch
            }
            
            setState { copy(showWithdrawDialog = false, isLoading = true) }
            
            classUseCases.removeStudentFromClass(classId, studentId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        showNotification("Đã rời khỏi lớp thành công", NotificationType.SUCCESS)
                        setState { copy(isLoading = false) }
                        // Navigation back will be handled by screen
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                }
            }
        }
    }
}
