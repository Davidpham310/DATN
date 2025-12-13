package com.example.datn.presentation.student.games.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.domain.usecase.minigame.MiniGameUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.student.games.state.MiniGameResultState
import com.example.datn.presentation.student.games.event.MiniGameResultEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiniGameResultViewModel @Inject constructor(
    private val miniGameUseCases: MiniGameUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    private val syncManager: com.example.datn.data.sync.FirebaseRoomSyncManager,
    notificationManager: NotificationManager
) : BaseViewModel<MiniGameResultState, MiniGameResultEvent>(
    MiniGameResultState(),
    notificationManager
) {

    companion object {
        private const val TAG = "MiniGameResultVM"
    }

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
            initialValue = ""
        )

    override fun onEvent(event: MiniGameResultEvent) {
        when (event) {
            is MiniGameResultEvent.LoadResult -> {
                loadResult(event.miniGameId, event.resultId)
            }
            MiniGameResultEvent.ToggleDetailedAnswers -> {
                setState { copy(showDetailedAnswers = !showDetailedAnswers) }
            }
            MiniGameResultEvent.PlayAgain -> {
                handlePlayAgain()
            }
        }
    }

    private fun loadResult(miniGameId: String, resultId: String) {
        Log.d(TAG, "[loadResult] START - miniGameId: $miniGameId, resultId: $resultId")
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            // Get student ID
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.first { it.isNotBlank() }
            }

            var tempStudentId: String? = null
            try {
                getStudentProfileByUserId(currentUserId).collect { profileResult ->
                    when (profileResult) {
                        is Resource.Success -> {
                            tempStudentId = profileResult.data?.id
                            Log.d(TAG, "[loadResult] Got student ID: $tempStudentId")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "[loadResult] Error getting profile: ${profileResult.message}")
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[loadResult] Exception getting profile: ${e.message}")
            }

            val studentId = tempStudentId
            if (studentId == null) {
                Log.e(TAG, "[loadResult] Student ID not found")
                setState { copy(isLoading = false, error = "Không tìm thấy thông tin học sinh") }
                showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                return@launch
            }

            // ========== SYNC DATA FIRST ==========
            try {
                Log.d(TAG, "[loadResult] Syncing minigame data...")
                syncManager.syncMiniGameData(miniGameId, forceSync = false)
                Log.d(TAG, "[loadResult] MiniGame data synced")
                
                Log.d(TAG, "[loadResult] Syncing student results...")
                syncManager.syncStudentMiniGameResults(studentId, miniGameId, forceSync = false)
                Log.d(TAG, "[loadResult] Student results synced")
            } catch (e: Exception) {
                Log.w(TAG, "[loadResult] Sync warning: ${e.message} - Will try to use cached data")
            }
            // ==========================================

            // Load data
            try {
                var miniGame: com.example.datn.domain.models.MiniGame? = null
                var currentResult: com.example.datn.domain.models.StudentMiniGameResult? = null
                var allResults: List<com.example.datn.domain.models.StudentMiniGameResult> = emptyList()
                var questions: List<com.example.datn.domain.models.MiniGameQuestion> = emptyList()
                var answers: List<com.example.datn.domain.models.StudentMiniGameAnswer> = emptyList()

                // Load mini game details
                miniGameUseCases.getMiniGameById(miniGameId).collect { gameResult ->
                    when (gameResult) {
                        is Resource.Success -> {
                            miniGame = gameResult.data
                            Log.d(TAG, "[loadResult] MiniGame loaded: ${miniGame?.title}")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "[loadResult] Error loading minigame: ${gameResult.message}")
                        }
                        else -> {}
                    }
                }

                // Load current result by ID
                miniGameUseCases.getStudentResultById(resultId).collect { resultResult ->
                    when (resultResult) {
                        is Resource.Success -> {
                            currentResult = resultResult.data
                            Log.d(TAG, "[loadResult] Current result loaded - score: ${currentResult?.score}")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "[loadResult] Error loading result: ${resultResult.message}")
                        }
                        else -> {}
                    }
                }

                // Load ALL results for history
                miniGameUseCases.getAllStudentResults(studentId, miniGameId).collect { allResultsResult ->
                    when (allResultsResult) {
                        is Resource.Success -> {
                            allResults = allResultsResult.data ?: emptyList()
                            Log.d(TAG, "[loadResult] All results loaded: ${allResults.size} attempts")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "[loadResult] Error loading all results: ${allResultsResult.message}")
                        }
                        else -> {}
                    }
                }

                // Load questions
                miniGameUseCases.getQuestionsByMiniGame(miniGameId).collect { questionsResult ->
                    when (questionsResult) {
                        is Resource.Success -> {
                            questions = questionsResult.data ?: emptyList()
                            Log.d(TAG, "[loadResult] Questions loaded: ${questions.size}")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "[loadResult] Error loading questions: ${questionsResult.message}")
                        }
                        else -> {}
                    }
                }

                // Load student answers
                val resultForAnswers = currentResult
                if (resultForAnswers != null) {
                    miniGameUseCases.getMiniGameAnswers(resultForAnswers.id).collect { answersResult ->
                        when (answersResult) {
                            is Resource.Success -> {
                                answers = answersResult.data ?: emptyList()
                                Log.d(TAG, "[loadResult] Answers loaded: ${answers.size}")
                            }
                            is Resource.Error -> {
                                Log.e(TAG, "[loadResult] Error loading answers: ${answersResult.message}")
                            }
                            else -> {}
                        }
                    }
                }

                // Build QuestionWithAnswer list
                val questionsWithAnswers = if (questions.isNotEmpty()) {
                    buildMiniGameQuestionsWithAnswers(questions, answers, miniGameUseCases)
                } else {
                    emptyList()
                }

                // Update state
                if (miniGame != null && currentResult != null) {
                    Log.d(TAG, "[loadResult] SUCCESS - MiniGame result data loaded")
                    setState {
                        copy(
                            miniGame = miniGame,
                            result = currentResult,
                            questions = questionsWithAnswers,
                            allResults = allResults,
                            isLoading = false,
                            error = null
                        )
                    }
                    showNotification("Kết quả mini game đã sẵn sàng", NotificationType.SUCCESS)
                } else {
                    Log.e(TAG, "[loadResult] Missing data - minigame: ${miniGame != null}, result: ${currentResult != null}")
                    setState {
                        copy(
                            isLoading = false,
                            error = "Không thể tải đầy đủ thông tin kết quả"
                        )
                    }
                    showNotification("Không thể tải đầy đủ thông tin kết quả", NotificationType.ERROR)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[loadResult] Exception: ${e.message}")
                setState {
                    copy(isLoading = false, error = "Lỗi: ${e.message}")
                }
                showNotification("Lỗi: ${e.message}", NotificationType.ERROR)
            }
        }
    }

    private fun handlePlayAgain() {
        // TODO: Implement play again logic
        // Navigate back to game play screen
        Log.d(TAG, "[handlePlayAgain] Play again requested")
        showNotification("Chức năng chơi lại đang được phát triển", NotificationType.INFO)
    }
}
