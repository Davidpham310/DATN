package com.example.datn.presentation.common.lesson

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState

data class LessonManagerState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val lessons: List<Lesson> = emptyList(),
    val selectedLesson: Lesson? = null,
    val lessonContents: List<LessonContent> = emptyList(),
    val showAddEditDialog: Boolean = false,
    val editingLesson: Lesson? = null,
    val confirmDeleteState: ConfirmationDialogState<Lesson> = ConfirmationDialogState.empty(),
    val currentClassId: String = ""
) : BaseState