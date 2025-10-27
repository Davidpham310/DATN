package com.example.datn.presentation.teacher.minigame

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.GameType
import com.example.datn.domain.models.Level
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.minigame.MiniGameUseCases
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.minigame.MiniGameManagerEvent
import com.example.datn.presentation.common.minigame.MiniGameManagerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class LessonMiniGameManagerViewModel @Inject constructor(
    private val miniGameUseCases: MiniGameUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<MiniGameManagerState, MiniGameManagerEvent>(
    MiniGameManagerState(), notificationManager
) {

    private var currentTeacherId: String = ""

    init {
        viewModelScope.launch {
            authUseCases.getCurrentIdUser()
                .distinctUntilChanged()
                .collect { id ->
                    currentTeacherId = id
                    Log.d("MiniGameManagerVM", "Loaded teacherId: $currentTeacherId")
                }
        }
    }

    // =====================================================
    // EVENT HANDLING
    // =====================================================
    override fun onEvent(event: MiniGameManagerEvent) {
        when (event) {
            is MiniGameManagerEvent.LoadGamesForLesson -> loadGames(event.lessonId)
            is MiniGameManagerEvent.RefreshGames -> refreshGames()
            is MiniGameManagerEvent.SelectGame -> setState { copy(selectedGame = event.game) }

            is MiniGameManagerEvent.ShowAddGameDialog -> {
                setState { copy(showAddEditDialog = true, editingGame = null) }
            }
            is MiniGameManagerEvent.EditGame -> {
                setState { copy(showAddEditDialog = true, editingGame = event.game) }
            }
            is MiniGameManagerEvent.DeleteGame -> showConfirmDeleteGame(event.game)
            is MiniGameManagerEvent.DismissDialog -> {
                setState { copy(showAddEditDialog = false, editingGame = null) }
            }

            is MiniGameManagerEvent.ConfirmAddGame -> addGame(event)
            is MiniGameManagerEvent.ConfirmEditGame -> updateGame(event)
        }
    }

    // =====================================================
    // CRUD & LOAD GAMES
    // =====================================================
    private fun loadGames(lessonId: String) {
        setState { copy(currentLessonId = lessonId) }
        viewModelScope.launch {
            miniGameUseCases.getMiniGamesByLesson(lessonId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        val games = result.data ?: emptyList()
                        setState {
                            copy(
                                miniGames = games,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải mini game thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun refreshGames() {
        val lessonId = state.value.currentLessonId
        if (lessonId.isNotEmpty()) loadGames(lessonId)
    }

    private fun addGame(event: MiniGameManagerEvent.ConfirmAddGame) {
        if (currentTeacherId.isBlank()) {
            showNotification("Không xác định được giáo viên", NotificationType.ERROR)
            return
        }

        if (event.title.isBlank()) {
            showNotification("Tiêu đề không được để trống", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            val newGame = MiniGame(
                id = "",
                teacherId = currentTeacherId,
                lessonId = event.lessonId,
                title = event.title.trim(),
                description = event.description.trim(),
                gameType = event.gameType,
                level = event.level,
                contentUrl = event.contentUrl?.trim(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            miniGameUseCases.createMiniGame(newGame).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false) }
                        showNotification("Thêm mini game thành công!", NotificationType.SUCCESS)
                        refreshGames()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Thêm mini game thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun updateGame(event: MiniGameManagerEvent.ConfirmEditGame) {
        if (currentTeacherId.isBlank()) {
            showNotification("Không xác định được giáo viên", NotificationType.ERROR)
            return
        }

        if (event.title.isBlank()) {
            showNotification("Tiêu đề không được để trống", NotificationType.ERROR)
            return
        }

        // Get the original game to preserve createdAt
        val originalGame = state.value.miniGames.find { it.id == event.id }

        viewModelScope.launch {
            val updatedGame = MiniGame(
                id = event.id,
                teacherId = currentTeacherId,
                lessonId = event.lessonId,
                title = event.title.trim(),
                description = event.description.trim(),
                gameType = event.gameType,
                level = event.level,
                contentUrl = event.contentUrl?.trim(),
                createdAt = originalGame?.createdAt ?: Instant.now(),
                updatedAt = Instant.now()
            )

            miniGameUseCases.updateMiniGame(updatedGame).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false, editingGame = null) }
                        showNotification("Cập nhật mini game thành công!", NotificationType.SUCCESS)
                        refreshGames()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Cập nhật mini game thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    // =====================================================
    // DELETE GAME
    // =====================================================
    private fun showConfirmDeleteGame(game: MiniGame) {
        setState {
            copy(
                confirmDeleteState = ConfirmationDialogState(
                    isShowing = true,
                    title = "Xác nhận xóa mini game",
                    message = "Bạn có chắc chắn muốn xóa mini game \"${game.title}\"?\nHành động này sẽ xóa toàn bộ câu hỏi và không thể hoàn tác.",
                    data = game
                )
            )
        }
    }

    fun dismissConfirmDeleteDialog() {
        setState { copy(confirmDeleteState = ConfirmationDialogState.empty()) }
    }

    fun confirmDeleteGame(game: MiniGame) {
        dismissConfirmDeleteDialog()
        viewModelScope.launch {
            miniGameUseCases.deleteMiniGame(game.id).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        showNotification("Xóa mini game thành công!", NotificationType.SUCCESS)
                        refreshGames()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Xóa mini game thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }
}
