package com.example.datn.presentation.student.games.event

import com.example.datn.core.base.BaseEvent

sealed class MiniGameListEvent : BaseEvent {
    data class LoadMiniGamesByLesson(val lessonId: String, val lessonTitle: String? = null) : MiniGameListEvent()
}
