package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.usecase.progress.UpdateLessonProgressUseCase
import com.example.datn.domain.usecase.progress.UpdateLessonProgressParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Quản lý tự động lưu tiến độ học tập lên Firebase + Room
 *
 * Trách nhiệm:
 * - Tự động lưu tiến độ mỗi 10 giây
 * - Lưu khi chuyển sang nội dung khác
 * - Lưu khi kết thúc phiên học
 * - Lưu khẩn cấp khi app vào background/screen off/tắt nguồn
 * - Retry tự động nếu lỗi
 *
 * Sử dụng StudentLessonProgress làm source of truth
 */
class ProgressAutoSaveManager @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val updateLessonProgressUseCase: UpdateLessonProgressUseCase
) {

    private val TAG = "ProgressAutoSaveManager"

    private var autoSaveJob: Job? = null
    private var retryJob: Job? = null
    private var emergencySaveJob: Job? = null

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _lastSaveTime = MutableStateFlow(0L)
    val lastSaveTime: StateFlow<Long> = _lastSaveTime.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState.IDLE)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _emergencySaveStatus = MutableStateFlow<EmergencySaveStatus?>(null)
    val emergencySaveStatus: StateFlow<EmergencySaveStatus?> = _emergencySaveStatus.asStateFlow()

    private val _pendingSaveCount = MutableStateFlow(0)
    val pendingSaveCount: StateFlow<Int> = _pendingSaveCount.asStateFlow()

    private var pendingProgressParams: UpdateLessonProgressParams? = null
    private var lastSavedParams: UpdateLessonProgressParams? = null

    /**
     * Trạng thái lưu
     */
    enum class SaveState {
        IDLE,           // Không có gì để lưu
        PENDING,        // Có dữ liệu chờ lưu
        SAVING,         // Đang lưu
        SAVED,          // Đã lưu thành công
        ERROR,          // Lỗi khi lưu
        EMERGENCY_SAVING // Đang lưu khẩn cấp
    }

    /**
     * Trạng thái lưu khẩn cấp
     */
    data class EmergencySaveStatus(
        val reason: EmergencySaveReason,
        val attemptCount: Int,
        val maxAttempts: Int,
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Lý do lưu khẩn cấp
     */
    enum class EmergencySaveReason {
        APP_BACKGROUND,     // App vào background
        SCREEN_OFF,         // Màn hình tắt
        LOW_BATTERY,        // Pin yếu
        DEVICE_SHUTDOWN,    // Tắt nguồn
        FORCE_EXIT          // Buộc thoát
    }

    /**
     * Bắt đầu tự động lưu tiến độ
     */
    fun startAutoSave() {
        stopAutoSave()

        Log.d(TAG, "🚀 startAutoSave() - Starting auto-save loop (every ${LearningProgressConfig.AUTO_SAVE_INTERVAL_SECONDS}s)")
        autoSaveJob = coroutineScope.launch {
            while (true) {
                delay((LearningProgressConfig.AUTO_SAVE_INTERVAL_SECONDS * 1000).toLong())

                Log.d(TAG, "⏰ Auto-save interval reached, checking for pending progress...")
                pendingProgressParams?.let {
                    // Chỉ lưu nếu có thay đổi so với lần lưu trước
                    if (hasChanges(it)) {
                        Log.d(TAG, "✅ Found changed progress, saving...")
                        saveProgress(it)
                    } else {
                        Log.d(TAG, "⏭️ No changes detected, skipping save")
                    }
                } ?: run {
                    Log.w(TAG, "⚠️ No pending progress params to save")
                }
            }
        }
    }

    /**
     * Dừng tự động lưu
     */
    fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    /**
     * Lưu tiến độ ngay lập tức
     */
    fun saveProgressImmediately(params: UpdateLessonProgressParams) {
        pendingProgressParams = params
        _saveState.value = SaveState.PENDING
        _pendingSaveCount.value++

        coroutineScope.launch {
            saveProgress(params)
        }
    }

    /**
     * Cập nhật dữ liệu tiến độ chờ lưu
     */
    fun updatePendingProgress(params: UpdateLessonProgressParams) {
        Log.d(TAG, "📝 updatePendingProgress() - Setting pending params")
        Log.d(TAG, "   - Student: ${params.studentId}")
        Log.d(TAG, "   - Lesson: ${params.lessonId}")
        Log.d(TAG, "   - Progress: ${params.progressPercentage}%")
        Log.d(TAG, "   - Time: ${params.additionalTimeSeconds}s")

        pendingProgressParams = params
        _saveState.value = SaveState.PENDING
    }

    /**
     * Lưu khẩn cấp khi app vào background
     */
    fun emergencySaveForBackground() {
        performEmergencySave(EmergencySaveReason.APP_BACKGROUND)
    }

    /**
     * Lưu khẩn cấp khi màn hình tắt
     */
    fun emergencySaveForScreenOff() {
        performEmergencySave(EmergencySaveReason.SCREEN_OFF)
    }

    /**
     * Lưu khẩn cấp khi pin yếu
     */
    fun emergencySaveForLowBattery() {
        performEmergencySave(EmergencySaveReason.LOW_BATTERY)
    }

    /**
     * Lưu khẩn cấp khi tắt nguồn
     */
    fun emergencySaveForShutdown() {
        performEmergencySave(EmergencySaveReason.DEVICE_SHUTDOWN)
    }

    /**
     * Lưu khẩn cấp khi buộc thoát
     */
    fun emergencySaveForForceExit() {
        performEmergencySave(EmergencySaveReason.FORCE_EXIT)
    }

    /**
     * Thực hiện lưu khẩn cấp với retry
     */
    private fun performEmergencySave(reason: EmergencySaveReason) {
        val params = pendingProgressParams
        if (params == null) {
            Log.w(TAG, "⚠️ No pending progress to emergency save")
            return
        }

        // Hủy các job đang chạy
        emergencySaveJob?.cancel()
        retryJob?.cancel()

        _saveState.value = SaveState.EMERGENCY_SAVING

        Log.w(TAG, "🚨 EMERGENCY SAVE triggered: $reason")

        emergencySaveJob = coroutineScope.launch {
            var attemptCount = 0
            var isSuccess = false
            var errorMessage: String? = null

            while (attemptCount < LearningProgressConfig.EMERGENCY_SAVE_RETRY_COUNT && !isSuccess) {
                attemptCount++

                _emergencySaveStatus.value = EmergencySaveStatus(
                    reason = reason,
                    attemptCount = attemptCount,
                    maxAttempts = LearningProgressConfig.EMERGENCY_SAVE_RETRY_COUNT,
                    isSuccess = false
                )

                Log.d(TAG, "🔄 Emergency save attempt $attemptCount/${LearningProgressConfig.EMERGENCY_SAVE_RETRY_COUNT}")

                try {
                    // Timeout cho mỗi lần thử
                    val result = withTimeoutOrNull(3000L) {
                        updateLessonProgressUseCase(params).first()
                    }

                    when (result) {
                        is Resource.Success -> {
                            isSuccess = true
                            _lastSaveTime.value = System.currentTimeMillis()
                            lastSavedParams = params
                            Log.d(TAG, "✅ Emergency save SUCCESS on attempt $attemptCount")
                        }
                        is Resource.Error -> {
                            errorMessage = result.message
                            Log.e(TAG, "❌ Emergency save FAILED on attempt $attemptCount: ${result.message}")
                        }
                        else -> {
                            errorMessage = "Timeout or unknown error"
                            Log.e(TAG, "❌ Emergency save TIMEOUT on attempt $attemptCount")
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = e.message
                    Log.e(TAG, "❌ Emergency save EXCEPTION on attempt $attemptCount: ${e.message}", e)
                }

                if (!isSuccess && attemptCount < LearningProgressConfig.EMERGENCY_SAVE_RETRY_COUNT) {
                    delay(LearningProgressConfig.EMERGENCY_SAVE_RETRY_DELAY_MS)
                }
            }

            _emergencySaveStatus.value = EmergencySaveStatus(
                reason = reason,
                attemptCount = attemptCount,
                maxAttempts = LearningProgressConfig.EMERGENCY_SAVE_RETRY_COUNT,
                isSuccess = isSuccess,
                errorMessage = if (!isSuccess) errorMessage else null
            )

            _saveState.value = if (isSuccess) SaveState.SAVED else SaveState.ERROR

            if (isSuccess) {
                Log.d(TAG, "✅ Emergency save completed successfully")
            } else {
                Log.e(TAG, "❌ Emergency save FAILED after $attemptCount attempts: $errorMessage")
                _saveError.value = errorMessage
            }
        }
    }

    /**
     * Kiểm tra xem có thay đổi so với lần lưu trước không
     */
    private fun hasChanges(params: UpdateLessonProgressParams): Boolean {
        val last = lastSavedParams ?: return true

        return params.progressPercentage != last.progressPercentage ||
                params.additionalTimeSeconds != last.additionalTimeSeconds ||
                params.lastAccessedContentId != last.lastAccessedContentId
    }

    /**
     * Dừng tất cả các tác vụ lưu
     */
    fun stop() {
        stopAutoSave()
        retryJob?.cancel()
        retryJob = null
        emergencySaveJob?.cancel()
        emergencySaveJob = null
    }

    /**
     * Reset trạng thái
     */
    fun reset() {
        stop()
        pendingProgressParams = null
        lastSavedParams = null
        _isSaving.value = false
        _lastSaveTime.value = 0L
        _saveError.value = null
        _saveState.value = SaveState.IDLE
        _emergencySaveStatus.value = null
        _pendingSaveCount.value = 0

        Log.d(TAG, "🔄 Reset complete")
    }

    /**
     * Lấy pending params
     */
    fun getPendingParams(): UpdateLessonProgressParams? = pendingProgressParams

    private suspend fun saveProgress(params: UpdateLessonProgressParams) {
        if (_isSaving.value) return

        _isSaving.value = true
        _saveState.value = SaveState.SAVING
        _saveError.value = null

        try {
            // Lưu tiến độ vào Room + Firebase
            Log.d(TAG, "💾 Saving progress for lesson: ${params.lessonId}")
            Log.d(TAG, "   - Student ID: ${params.studentId}")
            Log.d(TAG, "   - Progress: ${params.progressPercentage}%")

            val result = updateLessonProgressUseCase(params).first()

            when (result) {
                is Resource.Success -> {
                    // Cập nhật thời gian lưu
                    _lastSaveTime.value = System.currentTimeMillis()
                    lastSavedParams = params
                    _saveState.value = SaveState.SAVED

                    // Xóa lỗi trước đó
                    retryJob?.cancel()
                    retryJob = null

                    Log.d(TAG, "✅ Progress saved to Room + Firebase successfully: ${params.lessonId}")
                }
                is Resource.Error -> {
                    _saveError.value = result.message
                    _saveState.value = SaveState.ERROR
                    Log.e(TAG, "❌ Error saving progress: ${result.message}")
                    scheduleRetry(params)
                }
                is Resource.Loading -> {
                    Log.d(TAG, "⏳ Saving progress...")
                }
            }
        } catch (e: Exception) {
            _saveError.value = e.message
            _saveState.value = SaveState.ERROR
            Log.e(TAG, "❌ Exception when saving progress", e)
            scheduleRetry(params)
        } finally {
            _isSaving.value = false
        }
    }

    private fun scheduleRetry(params: UpdateLessonProgressParams) {
        retryJob?.cancel()
        retryJob = coroutineScope.launch {
            delay(5000)  // Retry sau 5 giây
            saveProgress(params)
        }
    }
}
