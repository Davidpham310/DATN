package com.example.datn.presentation.teacher.test

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseEvent
import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Test
import com.example.datn.domain.usecase.test.TestUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LessonTestState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val tests: List<Test> = emptyList(),
    val lessonId: String = "",
    val lessonTitle: String = ""
): BaseState

sealed class LessonTestEvent : BaseEvent {
    data class Load(val lessonId: String) : LessonTestEvent()
    object Refresh : LessonTestEvent()
}

@HiltViewModel
class LessonTestManagerViewModel @Inject constructor(
    private val useCases: TestUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<LessonTestState, LessonTestEvent>(LessonTestState(), notificationManager) {

    override fun onEvent(event: LessonTestEvent) {
        when (event) {
            is LessonTestEvent.Load -> load(event.lessonId)
            LessonTestEvent.Refresh -> refresh()
        }
    }

    fun setLesson(lessonId: String, lessonTitle: String) {
        setState { copy(lessonId = lessonId, lessonTitle = lessonTitle) }
        load(lessonId)
    }

    private fun load(lessonId: String) {
        viewModelScope.launch {
            useCases.listByLesson(lessonId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> setState { copy(isLoading = false, tests = result.data ?: emptyList(), error = null) }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải danh sách bài kiểm tra thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun refresh() {
        val id = state.value.lessonId
        if (id.isNotEmpty()) load(id)
    }
}


