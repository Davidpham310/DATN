package com.example.datn.presentation.student.lessons

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.lesson.LessonUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentLessonViewViewModel @Inject constructor(
    private val lessonUseCases: LessonUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<StudentLessonViewState, StudentLessonViewEvent>(
    StudentLessonViewState(),
    notificationManager
) {

    override fun onEvent(event: StudentLessonViewEvent) {
        when (event) {
            is StudentLessonViewEvent.LoadLesson -> loadLesson(event.lessonId)
            StudentLessonViewEvent.NextContent -> navigateToNextContent()
            StudentLessonViewEvent.PreviousContent -> navigateToPreviousContent()
            is StudentLessonViewEvent.GoToContent -> navigateToContent(event.index)
            StudentLessonViewEvent.MarkCurrentAsViewed -> markCurrentContentAsViewed()
            StudentLessonViewEvent.ShowProgressDialog -> setState { copy(showProgressDialog = true) }
            StudentLessonViewEvent.DismissProgressDialog -> setState { copy(showProgressDialog = false) }
            StudentLessonViewEvent.SaveProgress -> saveProgress()
        }
    }

    private fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null, sessionStartTime = System.currentTimeMillis()) }

            combine(
                lessonUseCases.getLessonById(lessonId),
                lessonUseCases.getLessonContentsByLesson(lessonId)
            ) { lessonResult, contentsResult ->
                Pair(lessonResult, contentsResult)
            }.collectLatest { (lessonResult, contentsResult) ->
                when {
                    lessonResult is Resource.Loading || contentsResult is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    lessonResult is Resource.Success && contentsResult is Resource.Success -> {
                        setState {
                            copy(
                                lesson = lessonResult.data,
                                lessonContents = contentsResult.data?.sortedBy { it.order } ?: emptyList(),
                                currentContentIndex = 0,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    lessonResult is Resource.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = lessonResult.message
                            )
                        }
                        showNotification(lessonResult.message, NotificationType.ERROR)
                    }
                    contentsResult is Resource.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = contentsResult.message
                            )
                        }
                        showNotification(contentsResult.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }
    
    private fun navigateToNextContent() {
        val state = state.value
        if (state.canGoNext) {
            setState { copy(currentContentIndex = currentContentIndex + 1) }
        }
    }
    
    private fun navigateToPreviousContent() {
        val state = state.value
        if (state.canGoPrevious) {
            setState { copy(currentContentIndex = currentContentIndex - 1) }
        }
    }
    
    private fun navigateToContent(index: Int) {
        val state = state.value
        if (index in state.lessonContents.indices) {
            setState { copy(currentContentIndex = index) }
        }
    }
    
    private fun markCurrentContentAsViewed() {
        val state = state.value
        val currentContent = state.currentContent ?: return
        setState {
            copy(viewedContentIds = viewedContentIds + currentContent.id)
        }
    }
    
    private fun saveProgress() {
        // TODO: Implement progress saving logic
        // This will save to backend when progress use cases are available
    }
}
