package com.example.datn.presentation.teacher.test.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseViewModel
import android.util.Log
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.validation.rules.test.ValidateTestDisplayOrder
import com.example.datn.core.utils.validation.rules.test.ValidateTestMediaUrl
import com.example.datn.core.utils.validation.rules.test.ValidateTestQuestionContent
import com.example.datn.core.utils.validation.rules.test.ValidateTestQuestionScore
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.usecase.minio.MinIOUseCase
import com.example.datn.domain.usecase.test.ImportTestQuestionsFromExcelUseCase
import com.example.datn.domain.usecase.test.TestQuestionUseCases
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.test.TestQuestionEvent
import com.example.datn.presentation.common.test.TestQuestionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject


@HiltViewModel
class TestQuestionManagerViewModel @Inject constructor(
    private val testQuestionUseCases: TestQuestionUseCases,
    private val importTestQuestionsFromExcelUseCase: ImportTestQuestionsFromExcelUseCase,
    private val minIOUseCase: MinIOUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<TestQuestionState, TestQuestionEvent>(TestQuestionState(), notificationManager) {

    private val questionContentValidator = ValidateTestQuestionContent()
    private val questionScoreValidator = ValidateTestQuestionScore()
    private val questionMediaUrlValidator = ValidateTestMediaUrl()
    private val displayOrderValidator = ValidateTestDisplayOrder()

    fun onFileSelected(fileName: String, stream: InputStream, size: Long, mimeType: String?) {
        state.value.selectedFileStream?.close()
        setState {
            copy(
                selectedFileName = fileName,
                selectedFileStream = stream,
                selectedFileSize = size,
                selectedFileMimeType = mimeType
            )
        }
    }

    fun resetFileSelection() {
        state.value.selectedFileStream?.close()
        setState {
            copy(
                selectedFileName = null,
                selectedFileStream = null,
                selectedFileSize = 0L,
                selectedFileMimeType = null
            )
        }
    }

    private fun resetUploadDialog() {
        setState {
            copy(
                isUploadDialogVisible = false,
                uploadFileName = null,
                uploadBytesUploaded = 0L,
                uploadTotalBytes = 0L,
                uploadProgressPercent = 0
            )
        }
    }

    private fun updateUploadProgress(uploaded: Long, total: Long) {
        val safeTotal = if (total > 0) total else 1L
        val percent = ((uploaded * 100) / safeTotal).toInt().coerceIn(0, 100)
        setState {
            copy(
                isUploadDialogVisible = true,
                uploadBytesUploaded = uploaded,
                uploadTotalBytes = total,
                uploadProgressPercent = percent
            )
        }
    }

    private fun guessExtension(fileName: String?, mimeType: String?): String {
        val nameLower = fileName.orEmpty().lowercase()
        return when {
            nameLower.endsWith(".pdf") || mimeType == "application/pdf" -> ".pdf"
            nameLower.endsWith(".mp4") || (mimeType?.startsWith("video/") == true) -> ".mp4"
            nameLower.endsWith(".mp3") || (mimeType?.startsWith("audio/") == true) -> ".mp3"
            nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") || mimeType == "image/jpeg" -> ".jpg"
            nameLower.endsWith(".png") || mimeType == "image/png" -> ".png"
            mimeType?.startsWith("image/") == true -> ".jpg"
            else -> ""
        }
    }

    private fun guessContentType(mimeType: String?): String {
        return mimeType?.trim().takeUnless { it.isNullOrBlank() } ?: "application/octet-stream"
    }

    private suspend fun uploadSelectedFileAndGetUrl(testId: String): String? {
        val fileStream = state.value.selectedFileStream ?: return null
        val fileSize = state.value.selectedFileSize
        val fileName = state.value.selectedFileName
        val mimeType = state.value.selectedFileMimeType

        if (fileSize <= 0) return null

        val ext = guessExtension(fileName, mimeType)
        val objectName = "tests/$testId/questions/question_${System.currentTimeMillis()}$ext"
        Log.d("TestQuestionVM", "objectName=$objectName, fileName=$fileName, mimeType=$mimeType, size=$fileSize")

        setState {
            copy(
                isUploadDialogVisible = true,
                uploadFileName = fileName,
                uploadBytesUploaded = 0L,
                uploadTotalBytes = fileSize,
                uploadProgressPercent = 0
            )
        }

        return try {
            minIOUseCase.uploadFile(
                objectName = objectName,
                inputStream = fileStream,
                size = fileSize,
                contentType = guessContentType(mimeType),
                onProgress = ::updateUploadProgress
            )
            Log.i("TestQuestionVM", "Uploaded media to MinIO: $objectName")
            objectName
        } finally {
            resetFileSelection()
        }
    }

    override fun onEvent(event: TestQuestionEvent) {
        when (event) {
            is TestQuestionEvent.LoadQuestions -> load(event.testId)
            TestQuestionEvent.RefreshQuestions -> refresh()
            TestQuestionEvent.ShowAddQuestionDialog -> showAddDialog()
            is TestQuestionEvent.EditQuestion -> showEditDialog(event.question)
            is TestQuestionEvent.DeleteQuestion -> showConfirmDelete(event.question)
            TestQuestionEvent.DismissDialog -> dismissDialog()
            is TestQuestionEvent.ConfirmAddQuestion -> addQuestion(
                event.testId,
                event.content,
                event.score,
                event.timeLimit,
                event.order,
                event.questionType,
                event.mediaUrl
            )
            is TestQuestionEvent.ConfirmEditQuestion -> updateQuestion(
                event.id,
                event.testId,
                event.content,
                event.score,
                event.timeLimit,
                event.order,
                event.questionType,
                event.mediaUrl
            )
            is TestQuestionEvent.SelectQuestion -> {}
        }
    }

    fun setTest(testId: String, testTitle: String) {
        setState { copy(testId = testId, testTitle = testTitle) }
        load(testId)
    }

    private fun load(testId: String) {
        testQuestionUseCases.listByTest(testId)
            .onEach { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        val questions = (result.data ?: emptyList()).sortedBy { it.order }
                        setState {
                            copy(isLoading = false, questions = questions, error = null)
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải câu hỏi thất bại", NotificationType.ERROR)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun showAddDialog() {
        resetFileSelection()
        resetUploadDialog()
        setState { copy(showAddEditDialog = true, editingQuestion = null) }
    }

    private fun showEditDialog(question: TestQuestion) {
        resetFileSelection()
        resetUploadDialog()
        setState { copy(showAddEditDialog = true, editingQuestion = question) }
    }

    private fun dismissDialog() {
        resetFileSelection()
        resetUploadDialog()
        setState { copy(showAddEditDialog = false, editingQuestion = null) }
    }

    private fun showConfirmDelete(question: TestQuestion) {
        setState {
            copy(
                confirmDeleteState = confirmDeleteState.copy(
                    isShowing = true,
                    title = "Xác nhận xóa câu hỏi",
                    message = "Bạn có chắc chắn muốn xóa câu hỏi này?\nHành động này không thể hoàn tác.",
                    data = question
                )
            )
        }
    }

    fun dismissConfirmDeleteDialog() {
        setState { copy(confirmDeleteState = confirmDeleteState.copy(isShowing = false, data = null)) }
    }

    fun confirmDeleteQuestion(question: TestQuestion) {
        viewModelScope.launch {
            testQuestionUseCases.delete(question.id)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { 
                                copy(
                                    isLoading = false,
                                    confirmDeleteState = confirmDeleteState.copy(isShowing = false, data = null)
                                ) 
                            }
                            showNotification("Xóa câu hỏi thành công", NotificationType.SUCCESS, 3000L)
                            refresh()
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            showNotification(result.message ?: "Xóa câu hỏi thất bại", NotificationType.ERROR)
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun addQuestion(
        testId: String,
        content: String,
        score: Double,
        timeLimit: Int,
        order: Int,
        questionType: QuestionType,
        mediaUrl: String?
    ) {
        val trimmedContent = content.trim()
        val trimmedMediaUrl = mediaUrl?.trim()?.ifBlank { null }

        val contentResult = questionContentValidator.validate(trimmedContent)
        if (!contentResult.successful) {
            showNotification(contentResult.errorMessage ?: "Nội dung câu hỏi không hợp lệ", NotificationType.ERROR)
            return
        }

        val scoreResult = questionScoreValidator.validate(score)
        if (!scoreResult.successful) {
            showNotification(scoreResult.errorMessage ?: "Điểm số không hợp lệ", NotificationType.ERROR)
            return
        }

        val mediaUrlResult = questionMediaUrlValidator.validate(trimmedMediaUrl)
        if (!mediaUrlResult.successful) {
            showNotification(mediaUrlResult.errorMessage ?: "Media URL không hợp lệ", NotificationType.ERROR)
            return
        }

        val timeLimitResult = displayOrderValidator.validate(timeLimit)
        if (!timeLimitResult.successful) {
            showNotification(timeLimitResult.errorMessage ?: "Thời gian trả lời không hợp lệ", NotificationType.ERROR)
            return
        }

        val orderResult = displayOrderValidator.validate(order)
        if (!orderResult.successful) {
            showNotification(orderResult.errorMessage ?: "Thứ tự hiển thị không hợp lệ", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            val uploadedUrl = try {
                uploadSelectedFileAndGetUrl(testId)
            } catch (e: Exception) {
                resetUploadDialog()
                showNotification("Upload file thất bại: ${e.message}", NotificationType.ERROR)
                return@launch
            } finally {
                resetUploadDialog()
            }

            val newQuestion = TestQuestion(
                id = "",
                testId = testId,
                content = trimmedContent,
                score = score,
                questionType = questionType,
                mediaUrl = uploadedUrl ?: trimmedMediaUrl,
                timeLimit = timeLimit,
                order = order,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            testQuestionUseCases.create(newQuestion)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { copy(isLoading = false, showAddEditDialog = false) }
                            showNotification("Thêm câu hỏi thành công", NotificationType.SUCCESS)
                            refresh()
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            val msg = result.message ?: "Thêm câu hỏi thất bại"
                            val type = if (msg.contains("Tổng điểm câu hỏi")) NotificationType.ERROR else NotificationType.ERROR
                            showNotification(msg, type)
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun updateQuestion(
        id: String,
        testId: String,
        content: String,
        score: Double,
        timeLimit: Int,
        order: Int,
        questionType: QuestionType,
        mediaUrl: String?
    ) {
        val existing = state.value.editingQuestion
        if (existing == null) {
            showNotification("Không tìm thấy câu hỏi cần chỉnh sửa", NotificationType.ERROR)
            return
        }

        if (existing.questionType != questionType) {
            showNotification("Không thể thay đổi loại câu hỏi sau khi tạo", NotificationType.ERROR)
            return
        }

        val trimmedContent = content.trim()
        val trimmedMediaUrl = mediaUrl?.trim()?.ifBlank { null }

        val contentResult = questionContentValidator.validate(trimmedContent)
        if (!contentResult.successful) {
            showNotification(contentResult.errorMessage ?: "Nội dung câu hỏi không hợp lệ", NotificationType.ERROR)
            return
        }

        val scoreResult = questionScoreValidator.validate(score)
        if (!scoreResult.successful) {
            showNotification(scoreResult.errorMessage ?: "Điểm số không hợp lệ", NotificationType.ERROR)
            return
        }

        val mediaUrlResult = questionMediaUrlValidator.validate(trimmedMediaUrl)
        if (!mediaUrlResult.successful) {
            showNotification(mediaUrlResult.errorMessage ?: "Media URL không hợp lệ", NotificationType.ERROR)
            return
        }

        val timeLimitResult = displayOrderValidator.validate(timeLimit)
        if (!timeLimitResult.successful) {
            showNotification(timeLimitResult.errorMessage ?: "Thời gian trả lời không hợp lệ", NotificationType.ERROR)
            return
        }

        val orderResult = displayOrderValidator.validate(order)
        if (!orderResult.successful) {
            showNotification(orderResult.errorMessage ?: "Thứ tự hiển thị không hợp lệ", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            val uploadedUrl = try {
                uploadSelectedFileAndGetUrl(testId)
            } catch (e: Exception) {
                resetUploadDialog()
                showNotification("Upload file thất bại: ${e.message}", NotificationType.ERROR)
                return@launch
            } finally {
                resetUploadDialog()
            }

            val updated = existing.copy(
                content = trimmedContent,
                score = score,
                questionType = existing.questionType,
                mediaUrl = uploadedUrl ?: trimmedMediaUrl,
                timeLimit = timeLimit,
                order = order,
                updatedAt = Instant.now()
            )

            testQuestionUseCases.update(updated)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { copy(isLoading = false, showAddEditDialog = false, editingQuestion = null) }
                            showNotification("Cập nhật câu hỏi thành công", NotificationType.SUCCESS)
                            refresh()
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            val msg = result.message ?: "Cập nhật câu hỏi thất bại"
                            val type = if (msg.contains("Tổng điểm câu hỏi")) NotificationType.ERROR else NotificationType.ERROR
                            showNotification(msg, type)
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun refresh() {
        val id = state.value.testId
        if (id.isNotEmpty()) load(id)
    }

    fun importFromExcel(testId: String, inputStream: InputStream) {
        val existingMaxOrder = state.value.questions.maxOfOrNull { it.order } ?: 0
        val startingOrder = existingMaxOrder + 1

        importTestQuestionsFromExcelUseCase(testId, inputStream, startingOrder)
            .onEach { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        val summary = result.data
                        if (summary != null) {
                            showNotification(
                                "Import xong: ${summary.importedQuestions} câu hỏi, ${summary.importedOptions} đáp án. Bỏ qua ${summary.skippedRows}/${summary.totalRows} dòng.",
                                NotificationType.SUCCESS
                            )
                        } else {
                            showNotification("Import xong", NotificationType.SUCCESS)
                        }
                        refresh()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Import thất bại", NotificationType.ERROR)
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}


