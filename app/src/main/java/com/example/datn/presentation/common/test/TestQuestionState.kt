package com.example.datn.presentation.common.test

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestQuestion
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import java.io.InputStream

data class TestQuestionState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val selectedFileName: String? = null,
    val selectedFileStream: InputStream? = null,
    val selectedFileSize: Long = 0L,
    val selectedFileMimeType: String? = null,
    val testId: String = "",
    val testTitle: String = "",
    val questions: List<TestQuestion> = emptyList(),
    val selectedQuestion: TestQuestion? = null,
    val showAddEditDialog: Boolean = false,
    val editingQuestion: TestQuestion? = null,
    val confirmDeleteState: ConfirmationDialogState<TestQuestion> = ConfirmationDialogState.empty(),
    val currentTestId: String = "",
    val isUploadDialogVisible: Boolean = false,
    val uploadFileName: String? = null,
    val uploadBytesUploaded: Long = 0L,
    val uploadTotalBytes: Long = 0L,
    val uploadProgressPercent: Int = 0
) : BaseState
