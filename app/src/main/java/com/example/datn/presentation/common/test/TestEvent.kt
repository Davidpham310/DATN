package com.example.datn.presentation.common.test

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.Test
import java.time.Instant

sealed class TestEvent : BaseEvent {
    data class LoadTests(val lessonId: String? = null) : TestEvent()
    object RefreshTests : TestEvent()
    data class SelectTest(val test: Test) : TestEvent()
    
    // Dialog events
    object ShowAddTestDialog : TestEvent()
    data class EditTest(val test: Test) : TestEvent()
    data class DeleteTest(val test: Test) : TestEvent()
    object DismissDialog : TestEvent()
    
    // CRUD events
    data class ConfirmAddTest(
        val classId: String,
        val lessonId: String,
        val title: String,
        val description: String?,
        val totalScore: Double,
        val startTime: Instant,
        val endTime: Instant
    ) : TestEvent()
    
    data class ConfirmEditTest(
        val id: String,
        val classId: String,
        val lessonId: String,
        val title: String,
        val description: String?,
        val totalScore: Double,
        val startTime: Instant,
        val endTime: Instant
    ) : TestEvent()
}
