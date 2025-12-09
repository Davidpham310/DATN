package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Quản lý tiến độ phát media (VIDEO, AUDIO)
 * 
 * Trách nhiệm:
 * - Theo dõi vị trí phát hiện tại
 * - Phát hiện tua nhanh
 * - Tính phần trăm xem/nghe
 * - Kiểm tra hoàn thành
 */
class MediaProgressManager @Inject constructor() {
    
    private val TAG = "MediaProgressManager"
    
    private val _mediaProgress = MutableStateFlow<Map<String, MediaProgress>>(emptyMap())
    val mediaProgress: StateFlow<Map<String, MediaProgress>> = _mediaProgress.asStateFlow()
    
    /**
     * Cập nhật vị trí phát hiện tại cho VIDEO
     * 
     * @param contentId ID của video
     * @param currentPositionMs Vị trí hiện tại (milliseconds)
     * @param durationMs Tổng thời lượng (milliseconds)
     * @return true nếu phát hiện tua nhanh, false nếu không
     */
    fun updateVideoPosition(contentId: String, currentPositionMs: Long, durationMs: Long): Boolean {
        val progress = _mediaProgress.value[contentId]
        val isFastForwarded = detectFastForward(progress?.lastPositionMs ?: 0, currentPositionMs)
        
        val viewPercentage = if (durationMs > 0) {
            ((currentPositionMs * 100) / durationMs).toInt()
        } else {
            0
        }
        
        if (isFastForwarded) {
            Log.w(TAG, "⏩ VIDEO FAST-FORWARD DETECTED!")
            Log.w(TAG, "   - Content: $contentId")
            Log.w(TAG, "   - Jump: ${progress?.lastPositionMs ?: 0}ms → ${currentPositionMs}ms")
            Log.w(TAG, "   - Threshold: ${LearningProgressConfig.VIDEO_FAST_FORWARD_THRESHOLD_MS}ms")
        } else {
            Log.d(TAG, "▶️ VIDEO POSITION UPDATE: $contentId")
            Log.d(TAG, "   - Position: ${currentPositionMs}ms / ${durationMs}ms")
            Log.d(TAG, "   - Progress: $viewPercentage%")
        }
        
        updateProgress(
            contentId,
            "VIDEO",
            currentPositionMs,
            durationMs,
            viewPercentage,
            isFastForwarded
        )
        
        return isFastForwarded
    }
    
    /**
     * Cập nhật vị trí phát hiện tại cho AUDIO
     * 
     * @param contentId ID của audio
     * @param currentPositionMs Vị trí hiện tại (milliseconds)
     * @param durationMs Tổng thời lượng (milliseconds)
     * @return true nếu phát hiện tua nhanh, false nếu không
     */
    fun updateAudioPosition(contentId: String, currentPositionMs: Long, durationMs: Long): Boolean {
        val progress = _mediaProgress.value[contentId]
        val isFastForwarded = detectFastForward(progress?.lastPositionMs ?: 0, currentPositionMs)
        
        val listenPercentage = if (durationMs > 0) {
            ((currentPositionMs * 100) / durationMs).toInt()
        } else {
            0
        }
        
        updateProgress(
            contentId,
            "AUDIO",
            currentPositionMs,
            durationMs,
            listenPercentage,
            isFastForwarded
        )
        
        return isFastForwarded
    }
    
    /**
     * Lấy tiến độ của một media
     */
    fun getProgress(contentId: String): MediaProgress? {
        val progress = _mediaProgress.value[contentId]
        Log.d(TAG, "📊 getProgress($contentId) - ${progress?.progressPercentage ?: 0}%")
        return progress
    }
    
    /**
     * Lấy phần trăm xem/nghe
     */
    fun getProgressPercentage(contentId: String): Int {
        return _mediaProgress.value[contentId]?.progressPercentage ?: 0
    }
    
    /**
     * Kiểm tra xem media có hoàn thành không
     */
    fun isMediaCompleted(contentId: String, mediaType: String): Boolean {
        val progress = _mediaProgress.value[contentId] ?: run {
            Log.w(TAG, "❌ isMediaCompleted($contentId) - No progress found")
            return false
        }
        val minPercentage = when (mediaType) {
            "VIDEO" -> LearningProgressConfig.VIDEO_MIN_COMPLETION_PERCENTAGE
            "AUDIO" -> LearningProgressConfig.AUDIO_MIN_COMPLETION_PERCENTAGE
            else -> return false
        }
        val isCompleted = progress.progressPercentage >= minPercentage && !progress.isFastForwarded
        if (isCompleted) {
            Log.d(TAG, "✅ $mediaType COMPLETED: $contentId (${progress.progressPercentage}%)")
        } else {
            Log.d(TAG, "⏳ $mediaType IN PROGRESS: $contentId (${progress.progressPercentage}% / $minPercentage%)")
        }
        return isCompleted
    }
    
    /**
     * Tạm dừng media khi app đi vào background
     */
    fun pauseForBackground() {
        Log.d(TAG, "⏸️ pauseForBackground() - Media paused")
    }

    /**
     * Tiếp tục media khi app quay lại foreground
     */
    fun resumeFromBackground() {
        Log.d(TAG, "▶️ resumeFromBackground() - Media resumed")
    }

    /**
     * Tạm dừng media khi màn hình tắt
     */
    fun pauseForScreenOff() {
        Log.d(TAG, "⏸️ pauseForScreenOff() - Media paused due to screen off")
    }

    /**
     * Tiếp tục media khi màn hình bật
     */
    fun resumeFromScreenOff() {
        Log.d(TAG, "▶️ resumeFromScreenOff() - Media resumed after screen on")
    }

    /**
     * Xóa tiến độ (khi chuyển sang media khác)
     */
    fun clear() {
        Log.d(TAG, "🗑️ clear() - Clearing all media progress (${_mediaProgress.value.size} items)")
        _mediaProgress.value = emptyMap()
    }
    
    /**
     * Phát hiện tua nhanh
     * Nếu nhảy > 5 giây thì coi là tua nhanh
     */
    private fun detectFastForward(lastPositionMs: Long, currentPositionMs: Long): Boolean {
        val threshold = LearningProgressConfig.VIDEO_FAST_FORWARD_THRESHOLD_MS
        return (currentPositionMs - lastPositionMs) > threshold
    }
    
    private fun updateProgress(
        contentId: String,
        mediaType: String,
        currentPositionMs: Long,
        durationMs: Long,
        progressPercentage: Int,
        isFastForwarded: Boolean
    ) {
        val currentProgress = _mediaProgress.value.toMutableMap()
        currentProgress[contentId] = MediaProgress(
            contentId = contentId,
            mediaType = mediaType,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            progressPercentage = progressPercentage,
            isFastForwarded = isFastForwarded,
            lastPositionMs = currentPositionMs,
            lastUpdatedTime = System.currentTimeMillis()
        )
        _mediaProgress.value = currentProgress
    }
}

/**
 * Tiến độ phát media
 */
data class MediaProgress(
    val contentId: String,
    val mediaType: String,  // VIDEO hoặc AUDIO
    val currentPositionMs: Long,
    val durationMs: Long,
    val progressPercentage: Int,
    val isFastForwarded: Boolean,
    val lastPositionMs: Long,
    val lastUpdatedTime: Long = System.currentTimeMillis()
)
