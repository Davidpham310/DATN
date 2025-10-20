package com.example.datn.presentation.common.lesson

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.LessonContent
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import java.io.InputStream

data class LessonContentManagerState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val selectedFileName: String? = null,
    val selectedFileStream: InputStream? = null,
    val selectedFileSize: Long = 0L,
    val lessonContents: List<LessonContent> = emptyList(),
    val selectedContent: LessonContent? = null,
    val showAddEditDialog: Boolean = false,
    val editingContent: LessonContent? = null,
    val confirmDeleteState: ConfirmationDialogState<LessonContent> = ConfirmationDialogState.empty(),
    val currentLessonId: String = ""
) : BaseState