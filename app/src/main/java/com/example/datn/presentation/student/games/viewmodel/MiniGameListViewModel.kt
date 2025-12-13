package com.example.datn.presentation.student.games.viewmodel

import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.minigame.MiniGameUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.student.games.state.MiniGameListState
import com.example.datn.presentation.student.games.event.MiniGameListEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MiniGameListViewModel @Inject constructor(
    private val miniGameUseCases: MiniGameUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<MiniGameListState, MiniGameListEvent>(
    MiniGameListState(),
    notificationManager
) {

    // Removed auto-loading in init - minigames should only be loaded by lesson

    override fun onEvent(event: MiniGameListEvent) {
        when (event) {
            is MiniGameListEvent.LoadMiniGamesByLesson -> {
                loadMiniGamesByLesson(event.lessonId, event.lessonTitle)
            }
        }
    }


    private fun loadMiniGamesByLesson(lessonId: String, lessonTitle: String?) {
        launch {
            android.util.Log.d("MiniGameListVM", "🎮 Loading minigames for lesson: $lessonId (title: $lessonTitle)")
            setState { 
                copy(
                    isLoading = true, 
                    error = null,
                    lessonId = lessonId,
                    lessonTitle = lessonTitle
                ) 
            }
            
            // Load mini games by lesson
            miniGameUseCases.getMiniGamesByLesson(lessonId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val games = resource.data ?: emptyList()
                        android.util.Log.d("MiniGameListVM", "✅ Loaded ${games.size} minigames for lesson $lessonId")
                        games.forEach { game ->
                            android.util.Log.d("MiniGameListVM", "  - ${game.title} (${game.level})")
                        }
                        setState {
                            copy(
                                isLoading = false,
                                miniGames = games,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        android.util.Log.e("MiniGameListVM", "❌ Error loading minigames: ${resource.message}")
                        setState {
                            copy(
                                isLoading = false,
                                error = resource.message ?: "Failed to load mini games for lesson"
                            )
                        }
                    }
                }
            }
        }
    }

}
