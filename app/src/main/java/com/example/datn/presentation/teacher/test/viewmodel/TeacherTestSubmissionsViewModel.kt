package com.example.datn.presentation.teacher.test.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.TestStatus
import com.example.datn.domain.models.User
import com.example.datn.domain.usecase.test.TestUseCases
import com.example.datn.domain.usecase.user.UserUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherTestSubmissionsViewModel @Inject constructor(
    private val testUseCases: TestUseCases,
    private val userUseCases: UserUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<TeacherTestSubmissionsState, TeacherTestSubmissionsEvent>(
    TeacherTestSubmissionsState(),
    notificationManager
) {

    override fun onEvent(event: TeacherTestSubmissionsEvent) {
        when (event) {
            is TeacherTestSubmissionsEvent.Load -> load(event.testId, event.testTitle)
        }
    }

    private fun load(testId: String, testTitle: String) {
        viewModelScope.launch {
            setState { copy(isLoading = true, testId = testId, testTitle = testTitle, error = null) }

            val resultsRes = awaitFirstNonLoading(testUseCases.getResultsByTest(testId))
            val results = (resultsRes as? Resource.Success)?.data
            if (results == null) {
                setState { copy(isLoading = false, error = (resultsRes as? Resource.Error)?.message) }
                showNotification((resultsRes as? Resource.Error)?.message ?: "Không thể tải danh sách bài nộp", NotificationType.ERROR)
                return@launch
            }

            val pending = results
                .filter { it.completionStatus == TestStatus.SUBMITTED }
                .sortedByDescending { it.submissionTime }

            val items = pending.mapNotNull { result ->
                val user = loadStudentUser(result.studentId)
                TeacherTestSubmissionUi(
                    result = result,
                    student = user
                )
            }

            setState { copy(isLoading = false, submissions = items, error = null) }
        }
    }

    private suspend fun loadStudentUser(studentId: String): User? {
        return try {
            val res = awaitFirstNonLoading(userUseCases.getStudentUser(studentId))
            (res as? Resource.Success)?.data
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun <T> awaitFirstNonLoading(flow: Flow<Resource<T>>): Resource<T> {
        var result: Resource<T>? = null
        flow
            .filter { it !is Resource.Loading }
            .take(1)
            .collect { value -> result = value }
        return result ?: Resource.Error("Không thể tải dữ liệu")
    }
}

data class TeacherTestSubmissionsState(
    val testId: String = "",
    val testTitle: String = "",
    val submissions: List<TeacherTestSubmissionUi> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null
) : com.example.datn.core.base.BaseState

data class TeacherTestSubmissionUi(
    val result: StudentTestResult,
    val student: User?
)

sealed class TeacherTestSubmissionsEvent : com.example.datn.core.base.BaseEvent {
    data class Load(val testId: String, val testTitle: String) : TeacherTestSubmissionsEvent()
}
