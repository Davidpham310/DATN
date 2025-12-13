package com.example.datn.presentation.student.games.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameQuestion

data class MiniGamePlayState(
    val miniGame: MiniGame? = null,
    val questions: List<MiniGameQuestion> = emptyList(),
    val questionOptions: Map<String, List<com.example.datn.domain.models.MiniGameOption>> = emptyMap(),
    val answers: Map<String, String> = emptyMap(),
    val multipleChoiceAnswers: Map<String, Set<String>> = emptyMap(), // For multiple choice questions
    val matchingPairs: Map<String, Map<String, String>> = emptyMap(), // For matching games: questionId -> (optionId -> pairedOptionId)
    val timeRemaining: Int = 0,
    val score: Int = 0,
    val isSubmitted: Boolean = false,
    val showSubmitDialog: Boolean = false,
    val currentQuestionIndex: Int = 0, // For navigation between questions
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState
