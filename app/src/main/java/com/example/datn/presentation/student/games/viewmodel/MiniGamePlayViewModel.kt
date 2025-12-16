package com.example.datn.presentation.student.games.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.minigame.MiniGameUseCases
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.progress.LogDailyStudyTimeUseCase
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.StudentMiniGameAnswer
import com.example.datn.domain.models.CompletionStatus
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.student.games.state.MiniGamePlayState
import com.example.datn.presentation.student.games.event.MiniGamePlayEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiniGamePlayViewModel @Inject constructor(
    private val miniGameUseCases: MiniGameUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    private val logDailyStudyTime: LogDailyStudyTimeUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<MiniGamePlayState, MiniGamePlayEvent>(
    MiniGamePlayState(),
    notificationManager
) {

    private var timerJob: Job? = null

    override fun onEvent(event: MiniGamePlayEvent) {
        when (event) {
            is MiniGamePlayEvent.LoadMiniGame -> {
                loadMiniGame(event.miniGameId)
            }
            is MiniGamePlayEvent.AnswerQuestion -> {
                updateAnswer(event.questionId, event.answer)
            }
            is MiniGamePlayEvent.ToggleMultipleChoice -> {
                toggleMultipleChoiceAnswer(event.questionId, event.optionId)
            }
            is MiniGamePlayEvent.CreateMatchingPair -> {
                createMatchingPair(event.questionId, event.option1Id, event.option2Id)
            }
            is MiniGamePlayEvent.RemoveMatchingPair -> {
                removeMatchingPair(event.questionId, event.optionId)
            }
            is MiniGamePlayEvent.NavigateToQuestion -> {
                setState { copy(currentQuestionIndex = event.questionIndex) }
            }
            MiniGamePlayEvent.ShowSubmitDialog -> {
                setState { copy(showSubmitDialog = true) }
            }
            MiniGamePlayEvent.DismissSubmitDialog -> {
                setState { copy(showSubmitDialog = false) }
            }
            MiniGamePlayEvent.ConfirmSubmit -> {
                submitAnswers()
            }
            MiniGamePlayEvent.TimerTick -> {
                handleTimerTick()
            }
            MiniGamePlayEvent.ResetGame -> {
                resetGame()
            }
        }
    }

    private fun loadMiniGame(miniGameId: String) {
        launch {
            setState { copy(isLoading = true, error = null) }
            
            // Check if this is a lesson-based minigame
            if (miniGameId.startsWith("lesson_")) {
                val lessonId = miniGameId.removePrefix("lesson_")
                android.util.Log.d("MiniGamePlayVM", "🎮 Loading lesson-based minigame for lessonId: $lessonId")
                loadLessonBasedMiniGame(lessonId)
                return@launch
            }
            
            // Regular minigame loading
            miniGameUseCases.getMiniGameById(miniGameId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val miniGame = resource.data
                        if (miniGame != null) {
                            loadQuestions(miniGameId, miniGame)
                        } else {
                            setState {
                                copy(
                                    isLoading = false,
                                    error = "Mini game not found"
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = resource.message ?: "Failed to load mini game"
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun loadLessonBasedMiniGame(lessonId: String) {
        launch {
            android.util.Log.d("MiniGamePlayVM", "🎯 Loading all minigames from lesson: $lessonId")
            
            // Load all minigames from this lesson
            miniGameUseCases.getMiniGamesByLesson(lessonId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val games = resource.data ?: emptyList()
                        if (games.isNotEmpty()) {
                            android.util.Log.d("MiniGamePlayVM", "📚 Found ${games.size} games in lesson")
                            
                            // Create a virtual minigame that represents the lesson
                            val virtualMiniGame = com.example.datn.domain.models.MiniGame(
                                id = "lesson_$lessonId",
                                teacherId = games.getOrNull(0)?.teacherId ?: "",
                                lessonId = lessonId,
                                title = "Câu hỏi từ bài học",
                                description = "Tổng hợp câu hỏi từ ${games.size} minigame trong bài học",
                                gameType = com.example.datn.domain.models.GameType.QUIZ, // Default to QUIZ for mixed content
                                level = games.getOrNull(0)?.level ?: com.example.datn.domain.models.Level.EASY,
                                contentUrl = null,
                                createdAt = games.minOfOrNull { it.createdAt } ?: java.time.Instant.now(),
                                updatedAt = java.time.Instant.now()
                            )
                            
                            loadQuestionsFromAllGames(games, virtualMiniGame)
                        } else {
                            setState {
                                copy(
                                    isLoading = false,
                                    error = "Bài học này chưa có câu hỏi nào"
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = resource.message ?: "Không thể tải câu hỏi từ bài học"
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun loadQuestionsFromAllGames(games: List<com.example.datn.domain.models.MiniGame>, virtualMiniGame: com.example.datn.domain.models.MiniGame) {
        launch {
            val allQuestions = mutableListOf<com.example.datn.domain.models.MiniGameQuestion>()
            var completedGames = 0
            
            if (games.isEmpty()) {
                setState {
                    copy(
                        isLoading = false,
                        error = "Không có minigame nào trong bài học này"
                    )
                }
                return@launch
            }
            
            games.forEach { game ->
                launch {
                    miniGameUseCases.getQuestionsByMiniGame(game.id).collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                val questions = resource.data ?: emptyList()
                                synchronized(allQuestions) {
                                    allQuestions.addAll(questions)
                                    completedGames++
                                    
                                    android.util.Log.d("MiniGamePlayVM", "✅ Loaded ${questions.size} questions from game ${game.id}. Progress: $completedGames/${games.size}")
                                    
                                    // Check if all games are processed
                                    if (completedGames >= games.size) {
                                        val uniqueQuestions = allQuestions.distinctBy { it.id }
                                        android.util.Log.d("MiniGamePlayVM", "🎯 Total questions loaded: ${uniqueQuestions.size}")
                                        
                                        setState {
                                            copy(
                                                isLoading = false,
                                                miniGame = virtualMiniGame,
                                                questions = uniqueQuestions,
                                                timeRemaining = 600 // Default 10 minutes
                                            )
                                        }
                                        
                                        // Load options for all questions
                                        loadOptionsForQuestions(uniqueQuestions)
                                        startTimer()
                                    }
                                }
                            }
                            is Resource.Error -> {
                                android.util.Log.e("MiniGamePlayVM", "❌ Error loading questions from game ${game.id}: ${resource.message}")
                                synchronized(allQuestions) {
                                    completedGames++
                                    if (completedGames >= games.size && allQuestions.isEmpty()) {
                                        setState {
                                            copy(
                                                isLoading = false,
                                                error = "Không thể tải câu hỏi từ bài học"
                                            )
                                        }
                                    }
                                }
                            }
                            is Resource.Loading -> {
                                // Keep loading state
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun loadQuestions(miniGameId: String, miniGame: com.example.datn.domain.models.MiniGame) {
        launch {
            miniGameUseCases.getQuestionsByMiniGame(miniGameId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val questions = resource.data ?: emptyList()
                        setState {
                            copy(
                                isLoading = false,
                                miniGame = miniGame,
                                questions = questions,
                                timeRemaining = 600 // Default 10 minutes
                            )
                        }
                        loadOptionsForQuestions(questions)
                        startTimer()
                    }
                    is Resource.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = resource.message ?: "Failed to load questions"
                            )
                        }
                    }
                    is Resource.Loading -> {
                        // Keep loading state
                    }
                }
            }
        }
    }
    
    private fun loadOptionsForQuestions(questions: List<com.example.datn.domain.models.MiniGameQuestion>) {
        launch {
            val optionsMap = mutableMapOf<String, List<com.example.datn.domain.models.MiniGameOption>>()
            
            questions.forEach { question ->
                miniGameUseCases.getOptionsByQuestion(question.id).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            optionsMap[question.id] = resource.data ?: emptyList()
                            setState {
                                copy(questionOptions = optionsMap.toMap())
                            }
                        }
                        is Resource.Error -> {
                            // Log error but don't fail the whole flow
                        }
                        is Resource.Loading -> {
                            // Keep loading
                        }
                    }
                }
            }
        }
    }

    private fun updateAnswer(questionId: String, answer: String) {
        setState {
            val currentAnswers = answers.toMutableMap()
            currentAnswers[questionId] = answer
            copy(answers = currentAnswers)
        }
    }
    
    private fun toggleMultipleChoiceAnswer(questionId: String, optionId: String) {
        setState {
            val currentMultipleChoiceAnswers = multipleChoiceAnswers.toMutableMap()
            val currentSelections = currentMultipleChoiceAnswers[questionId]?.toMutableSet() ?: mutableSetOf()
            
            if (currentSelections.contains(optionId)) {
                currentSelections.remove(optionId)
            } else {
                currentSelections.add(optionId)
            }
            
            currentMultipleChoiceAnswers[questionId] = currentSelections
            
            // Also update the regular answers map with comma-separated values for scoring
            val currentAnswers = answers.toMutableMap()
            currentAnswers[questionId] = currentSelections.joinToString(",")
            
            copy(
                multipleChoiceAnswers = currentMultipleChoiceAnswers,
                answers = currentAnswers
            )
        }
    }
    
    private fun createMatchingPair(questionId: String, option1Id: String, option2Id: String) {
        setState {
            val currentMatchingPairs = matchingPairs.toMutableMap()
            val questionPairs = currentMatchingPairs[questionId]?.toMutableMap() ?: mutableMapOf()
            
            // Remove any existing pairs for these options
            questionPairs.entries.removeAll { it.key == option1Id || it.value == option1Id || it.key == option2Id || it.value == option2Id }
            
            // Create new pair
            questionPairs[option1Id] = option2Id
            questionPairs[option2Id] = option1Id
            
            currentMatchingPairs[questionId] = questionPairs
            
            // Update answers for scoring (format: "option1:option2,option3:option4")
            val currentAnswers = answers.toMutableMap()
            val pairStrings = questionPairs.entries
                .filter { it.key < it.value } // Avoid duplicates
                .map { "${it.key}:${it.value}" }
            currentAnswers[questionId] = pairStrings.joinToString(",")
            
            copy(
                matchingPairs = currentMatchingPairs,
                answers = currentAnswers
            )
        }
    }
    
    private fun removeMatchingPair(questionId: String, optionId: String) {
        setState {
            val currentMatchingPairs = matchingPairs.toMutableMap()
            val questionPairs = currentMatchingPairs[questionId]?.toMutableMap() ?: mutableMapOf()
            
            // Find and remove the pair containing this option
            val pairedOptionId = questionPairs[optionId]
            if (pairedOptionId != null) {
                questionPairs.remove(optionId)
                questionPairs.remove(pairedOptionId)
            }
            
            currentMatchingPairs[questionId] = questionPairs
            
            // Update answers for scoring
            val currentAnswers = answers.toMutableMap()
            val pairStrings = questionPairs.entries
                .filter { it.key < it.value }
                .map { "${it.key}:${it.value}" }
            currentAnswers[questionId] = pairStrings.joinToString(",")
            
            copy(
                matchingPairs = currentMatchingPairs,
                answers = currentAnswers
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (state.value.timeRemaining > 0 && !state.value.isSubmitted) {
                delay(1000)
                onEvent(MiniGamePlayEvent.TimerTick)
            }
            // Auto submit when time runs out
            if (state.value.timeRemaining <= 0) {
                submitAnswers()
            }
        }
    }

    private fun handleTimerTick() {
        setState {
            if (timeRemaining > 0) {
                copy(timeRemaining = timeRemaining - 1)
            } else {
                this
            }
        }
    }
    
    private fun resetGame() {
        timerJob?.cancel()
        setState {
            copy(
                answers = emptyMap(),
                multipleChoiceAnswers = emptyMap(),
                matchingPairs = emptyMap(),
                timeRemaining = 600, // Reset to 10 minutes
                score = 0,
                isSubmitted = false,
                showSubmitDialog = false,
                currentQuestionIndex = 0
            )
        }
        startTimer()
        android.util.Log.d("MiniGamePlayVM", "🔄 Game reset - ready to play again!")
    }
    
    /**
     * Normalize text for comparison - convert to lowercase and trim whitespace
     * This ensures case-insensitive comparison for all text-based answers
     */
    private fun normalizeText(text: String?): String {
        return text?.trim()?.lowercase() ?: ""
    }
    
    /**
     * Check if two texts are equal after normalization
     */
    private fun isTextEqual(userText: String?, correctText: String?): Boolean {
        return normalizeText(userText) == normalizeText(correctText)
    }

    private fun submitAnswers() {
        launch {
            timerJob?.cancel()
            
            val currentState = state.value
            val miniGame = currentState.miniGame ?: return@launch
            val answers = currentState.answers
            
            // Calculate score based on game type and question scoring
            var totalScore = 0.0
            var maxPossibleScore = 0.0
            var correctAnswers = 0
            
            currentState.questions.forEach { question ->
                val userAnswer = answers[question.id]
                val questionOptions = currentState.questionOptions[question.id] ?: emptyList()
                maxPossibleScore += question.score
                
                val isCorrect = when (question.questionType) {
                    com.example.datn.domain.models.QuestionType.SINGLE_CHOICE -> {
                        val correctOption = questionOptions.find { it.isCorrect }
                        userAnswer == correctOption?.id
                    }
                    com.example.datn.domain.models.QuestionType.MULTIPLE_CHOICE -> {
                        // For multiple choice, check if all selected answers are correct
                        val selectedIds = userAnswer?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                        val correctIds = questionOptions.filter { it.isCorrect }.map { it.id }
                        selectedIds.sorted() == correctIds.sorted() && selectedIds.isNotEmpty()
                    }
                    com.example.datn.domain.models.QuestionType.FILL_BLANK -> {
                        val correctOption = questionOptions.find { it.isCorrect }
                        isTextEqual(userAnswer, correctOption?.content)
                    }
                    com.example.datn.domain.models.QuestionType.ESSAY -> {
                        // For essay questions, check if answer contains expected keywords/phrases
                        // If no expected answer is provided, give credit for any non-blank answer
                        val expectedAnswer = questionOptions.find { it.isCorrect }?.content
                        if (expectedAnswer.isNullOrBlank()) {
                            // No expected answer, give credit for any response
                            !userAnswer.isNullOrBlank()
                        } else {
                            // Check if user answer contains expected keywords (case-insensitive)
                            val normalizedUserAnswer = normalizeText(userAnswer)
                            val normalizedExpected = normalizeText(expectedAnswer)
                            normalizedUserAnswer.contains(normalizedExpected) || 
                            normalizedExpected.split(" ").any { keyword -> 
                                normalizedUserAnswer.contains(keyword.trim()) 
                            }
                        }
                    }
                }
                
                // Special handling for matching games
                val finalIsCorrect = if (currentState.miniGame?.gameType == com.example.datn.domain.models.GameType.MATCHING) {
                    checkMatchingAnswer(question.id, userAnswer, questionOptions)
                } else {
                    isCorrect
                }
                
                if (finalIsCorrect) {
                    correctAnswers++
                    totalScore += question.score
                }
            }
            
            val finalScore = if (maxPossibleScore > 0) {
                ((totalScore / maxPossibleScore) * 100).toInt()
            } else 0
            
            android.util.Log.d("MiniGamePlayVM", "🎯 Game completed: $correctAnswers/${currentState.questions.size} correct, Score: $finalScore%")
            
            // Save result to Firebase
            saveGameResult(
                miniGame = miniGame,
                questions = currentState.questions,
                answers = answers,
                questionOptions = currentState.questionOptions,
                finalScore = finalScore,
                totalScore = totalScore,
                maxPossibleScore = maxPossibleScore
            )
            
            setState {
                copy(
                    isSubmitted = true,
                    score = finalScore,
                    showSubmitDialog = false
                )
            }
        }
    }

    /**
     * Save game result to Firebase
     */
    private fun saveGameResult(
        miniGame: com.example.datn.domain.models.MiniGame,
        questions: List<com.example.datn.domain.models.MiniGameQuestion>,
        answers: Map<String, String>,
        questionOptions: Map<String, List<com.example.datn.domain.models.MiniGameOption>>,
        finalScore: Int,
        totalScore: Double,
        maxPossibleScore: Double
    ) {
        launch {
            try {
                android.util.Log.d("MiniGamePlayVM", "🔍 Starting to save game result...")
                
                // Get current user ID - wait for actual user data
                var currentUserId: String? = null
                try {
                    authUseCases.getCurrentUser().collect { userResource ->
                        when (userResource) {
                            is Resource.Success -> {
                                currentUserId = userResource.data?.id
                                android.util.Log.d("MiniGamePlayVM", "✅ Got user from auth: ${currentUserId}")
                                return@collect // Exit collect once we get the user
                            }
                            is Resource.Error -> {
                                android.util.Log.e("MiniGamePlayVM", "❌ Auth error: ${userResource.message}")
                                return@collect
                            }
                            is Resource.Loading -> {
                                android.util.Log.d("MiniGamePlayVM", "⏳ Loading user...")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MiniGamePlayVM", "❌ Error getting user: ${e.message}")
                }
                
                android.util.Log.d("MiniGamePlayVM", "🔍 Current user ID: $currentUserId")
                
                if (currentUserId.isNullOrBlank()) {
                    android.util.Log.e("MiniGamePlayVM", "❌ Cannot save result: User not authenticated")
                    showNotification("Không thể lưu kết quả: Chưa đăng nhập", NotificationType.ERROR)
                    return@launch
                }
                
                // Get student profile
                var studentId: String? = null
                try {
                    getStudentProfileByUserId(currentUserId!!).collect { profileResource ->
                        when (profileResource) {
                            is Resource.Success -> {
                                studentId = profileResource.data?.id
                                android.util.Log.d("MiniGamePlayVM", "✅ Got student ID: $studentId")
                                return@collect // Exit collect once we get the profile
                            }
                            is Resource.Error -> {
                                android.util.Log.e("MiniGamePlayVM", "❌ Error getting student profile: ${profileResource.message}")
                                showNotification("Không thể lưu kết quả: ${profileResource.message}", NotificationType.ERROR)
                                return@collect
                            }
                            is Resource.Loading -> {
                                android.util.Log.d("MiniGamePlayVM", "⏳ Loading student profile...")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MiniGamePlayVM", "❌ Error getting student profile: ${e.message}")
                }
                
                if (studentId == null) {
                    android.util.Log.e("MiniGamePlayVM", "❌ Cannot save result: Student ID not found")
                    showNotification("Không thể lưu kết quả: Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                    return@launch
                }
                val resolvedStudentId = studentId!!
                
                // Calculate game duration
                val currentState = state.value
                val initialTime = 600 // 10 minutes in seconds
                val durationSeconds = (initialTime - currentState.timeRemaining).toLong()
                
                // Create result ID
                val resultId = UUID.randomUUID().toString()
                val now = Instant.now()
                
                // Create StudentMiniGameResult
                val gameResult = StudentMiniGameResult(
                    id = resultId,
                    studentId = resolvedStudentId,
                    miniGameId = miniGame.id,
                    score = totalScore,
                    maxScore = maxPossibleScore,
                    completionStatus = CompletionStatus.COMPLETED,
                    submissionTime = now,
                    durationSeconds = durationSeconds,
                    attemptNumber = 1, // TODO: Get actual attempt number
                    createdAt = now,
                    updatedAt = now
                )
                
                // Create StudentMiniGameAnswers
                val gameAnswers = mutableListOf<StudentMiniGameAnswer>()
                questions.forEach { question ->
                    val userAnswer = answers[question.id] ?: ""
                    val questionOptionsForQuestion = questionOptions[question.id] ?: emptyList()
                    
                    // Calculate if answer is correct and earned score
                    val (isCorrect, earnedScore) = calculateQuestionResult(
                        question = question,
                        userAnswer = userAnswer,
                        questionOptions = questionOptionsForQuestion,
                        miniGame = miniGame
                    )
                    
                    val answer = StudentMiniGameAnswer(
                        id = UUID.randomUUID().toString(),
                        resultId = resultId,
                        questionId = question.id,
                        answer = userAnswer,
                        isCorrect = isCorrect,
                        earnedScore = earnedScore,
                        createdAt = now,
                        updatedAt = now
                    )
                    gameAnswers.add(answer)
                }
                
                android.util.Log.d("MiniGamePlayVM", "💾 Saving game result: Score ${gameResult.score}/${gameResult.maxScore}, ${gameAnswers.size} answers")
                
                // Submit to database (Room + Firebase sync)
                android.util.Log.d("MiniGamePlayVM", "⏳ Submitting result to database...")
                
                // Submit using the project's repository pattern
                try {
                    miniGameUseCases.submitMiniGameResult(gameResult, gameAnswers).collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                android.util.Log.d("MiniGamePlayVM", "⏳ Saving result to database...")
                            }
                            is Resource.Success -> {
                                android.util.Log.d("MiniGamePlayVM", "✅ Game result saved successfully!")
                                android.util.Log.d("MiniGamePlayVM", "📊 Final result: Score ${resource.data?.score}/${resource.data?.maxScore}, Attempt #${resource.data?.attemptNumber}")

                                // Log study time for this minigame session into DailyStudyTime
                                logDailyStudyTime(resolvedStudentId, durationSeconds)

                                showNotification("Kết quả đã được lưu thành công!", NotificationType.SUCCESS)
                                return@collect // Exit after successful save
                            }
                            is Resource.Error -> {
                                android.util.Log.e("MiniGamePlayVM", "❌ Failed to save result: ${resource.message}")
                                showNotification("Lỗi khi lưu kết quả: ${resource.message}", NotificationType.ERROR)
                                return@collect
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MiniGamePlayVM", "❌ Exception during database submission: ${e.message}")
                    showNotification("Lỗi không mong muốn khi lưu: ${e.message}", NotificationType.ERROR)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MiniGamePlayVM", "❌ Exception saving result: ${e.message}")
                showNotification("Lỗi không mong muốn: ${e.message}", NotificationType.ERROR)
            }
        }
    }
    
    /**
     * Calculate if a question answer is correct and the earned score
     */
    private fun calculateQuestionResult(
        question: com.example.datn.domain.models.MiniGameQuestion,
        userAnswer: String,
        questionOptions: List<com.example.datn.domain.models.MiniGameOption>,
        miniGame: com.example.datn.domain.models.MiniGame
    ): Pair<Boolean, Double> {
        val isCorrect = when (question.questionType) {
            com.example.datn.domain.models.QuestionType.SINGLE_CHOICE -> {
                val correctOption = questionOptions.find { it.isCorrect }
                userAnswer == correctOption?.id
            }
            com.example.datn.domain.models.QuestionType.MULTIPLE_CHOICE -> {
                val selectedIds = userAnswer.split(",").filter { it.isNotBlank() }
                val correctIds = questionOptions.filter { it.isCorrect }.map { it.id }
                selectedIds.sorted() == correctIds.sorted() && selectedIds.isNotEmpty()
            }
            com.example.datn.domain.models.QuestionType.FILL_BLANK -> {
                val correctOption = questionOptions.find { it.isCorrect }
                isTextEqual(userAnswer, correctOption?.content)
            }
            com.example.datn.domain.models.QuestionType.ESSAY -> {
                val expectedAnswer = questionOptions.find { it.isCorrect }?.content
                if (expectedAnswer.isNullOrBlank()) {
                    userAnswer.isNotBlank()
                } else {
                    val normalizedUserAnswer = normalizeText(userAnswer)
                    val normalizedExpected = normalizeText(expectedAnswer)
                    normalizedUserAnswer.contains(normalizedExpected) || 
                    normalizedExpected.split(" ").any { keyword -> 
                        normalizedUserAnswer.contains(keyword.trim()) 
                    }
                }
            }
        }
        
        // Special handling for matching games
        val finalIsCorrect = if (miniGame.gameType == com.example.datn.domain.models.GameType.MATCHING) {
            checkMatchingAnswer(question.id, userAnswer, questionOptions)
        } else {
            isCorrect
        }
        
        val earnedScore = if (finalIsCorrect) question.score else 0.0
        return Pair(finalIsCorrect, earnedScore)
    }

    private fun checkMatchingAnswer(questionId: String, userAnswer: String?, questionOptions: List<com.example.datn.domain.models.MiniGameOption>): Boolean {
        if (userAnswer.isNullOrBlank()) return false
        
        // Parse user answer format: "option1:option2,option3:option4"
        val userPairs = userAnswer.split(",")
            .filter { it.isNotBlank() }
            .map { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .filterNotNull()
            .toSet()
        
        // Get correct pairs from options using pairId
        val correctPairs = mutableSetOf<Pair<String, String>>()
        questionOptions.forEach { option ->
            option.pairId?.let { pairId ->
                val pairedOption = questionOptions.find { it.id == pairId }
                if (pairedOption != null) {
                    // Ensure consistent ordering to avoid duplicates
                    val pair = if (option.id < pairedOption.id) {
                        option.id to pairedOption.id
                    } else {
                        pairedOption.id to option.id
                    }
                    correctPairs.add(pair)
                }
            }
        }
        
        // Normalize user pairs to match the same ordering
        val normalizedUserPairs = userPairs.map { (first, second) ->
            if (first < second) first to second else second to first
        }.toSet()
        
        return normalizedUserPairs == correctPairs && correctPairs.isNotEmpty()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
