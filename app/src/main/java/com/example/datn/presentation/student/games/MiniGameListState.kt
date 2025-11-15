package com.example.datn.presentation.student.games

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.MiniGame

data class MiniGameListState(
    val miniGames: List<MiniGame> = emptyList(),
    val lessonId: String? = null,
    val lessonTitle: String? = null,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState
