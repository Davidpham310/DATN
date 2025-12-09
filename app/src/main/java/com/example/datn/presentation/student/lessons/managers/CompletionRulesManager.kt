package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import com.example.datn.domain.models.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Quản lý quy tắc hoàn thành nội dung
 * 
 * Trách nhiệm:
 * - Kiểm tra xem nội dung có đáp ứng yêu cầu hoàn thành không
 * - Theo dõi tiến độ từng nội dung
 * - Gọi callback khi nội dung hoàn thành
 * - Cung cấp thông tin quy tắc hoàn thành
 */
class CompletionRulesManager @Inject constructor(
    private val contentCompletionManager: ContentCompletionManager,
    private val mediaProgressManager: MediaProgressManager
) {
    
    private val TAG = "CompletionRulesManager"
    
    // Callback khi nội dung hoàn thành
    private var onContentCompletedCallback: ((String) -> Unit)? = null
    
    /**s
     * Đăng ký callback khi nội dung hoàn thành
     */
    fun setOnContentCompletedCallback(callback: (String) -> Unit) {
        onContentCompletedCallback = callback
    }
    
    /**
     * Xử lý sự kiện TEXT được xem
     */
    fun handleTextViewed(contentId: String, elapsedSeconds: Long) {
        Log.d(TAG, "📄 TEXT viewed: $contentId for ${elapsedSeconds}s")
        Log.d(TAG, "   - Calling contentCompletionManager.updateViewTime()...")
        
        contentCompletionManager.updateViewTime(
            contentId = contentId,
            elapsedTimeSeconds = elapsedSeconds,
            contentType = "TEXT"
        )
        
        Log.d(TAG, "   - updateViewTime() completed")
        
        // Kiểm tra hoàn thành
        val isCompleted = contentCompletionManager.isContentCompleted(contentId, "TEXT")
        Log.d(TAG, "   - isContentCompleted: $isCompleted")
        if (isCompleted) {
            Log.d(TAG, "✅ TEXT completed: $contentId")
            onContentCompletedCallback?.invoke(contentId)
        }
    }
    
    /**
     * Xử lý sự kiện IMAGE được xem
     */
    fun handleImageViewed(contentId: String, elapsedSeconds: Long) {
        Log.d(TAG, "🖼️ IMAGE viewed: $contentId for ${elapsedSeconds}s")
        
        contentCompletionManager.updateViewTime(
            contentId = contentId,
            elapsedTimeSeconds = elapsedSeconds,
            contentType = "IMAGE"
        )
        
        // Kiểm tra hoàn thành
        val isCompleted = contentCompletionManager.isContentCompleted(contentId, "IMAGE")
        if (isCompleted) {
            Log.d(TAG, "✅ IMAGE completed: $contentId")
            onContentCompletedCallback?.invoke(contentId)
        }
    }
    
    /**
     * Xử lý sự kiện PDF được cuộn
     */
    fun handlePdfScrolled(contentId: String, scrollPercentage: Int) {
        Log.d(TAG, "📕 PDF scrolled: $contentId - $scrollPercentage%")
        
        contentCompletionManager.updatePdfScrollProgress(
            contentId = contentId,
            scrollPercentage = scrollPercentage
        )
        
        // Kiểm tra hoàn thành
        val isCompleted = contentCompletionManager.isContentCompleted(contentId, "PDF")
        if (isCompleted) {
            Log.d(TAG, "✅ PDF completed: $contentId")
            onContentCompletedCallback?.invoke(contentId)
        }
    }
    
    /**
     * Xử lý sự kiện VIDEO vị trí thay đổi
     */
    fun handleVideoPositionChanged(contentId: String, currentPositionMs: Long, durationMs: Long) {
        val percentage = if (durationMs > 0) {
            ((currentPositionMs * 100) / durationMs).toInt()
        } else {
            0
        }
        
        // Phát hiện tua nhanh
        val isFastForwarded = mediaProgressManager.updateVideoPosition(contentId, currentPositionMs, durationMs)
        
        if (isFastForwarded) {
            Log.w(TAG, "⚠️ Fast forward detected in VIDEO: $contentId")
            Log.w(TAG, "   - Current position: ${currentPositionMs}ms")
            Log.w(TAG, "   - Duration: ${durationMs}ms")
            Log.w(TAG, "   - Threshold: ${LearningProgressConfig.VIDEO_FAST_FORWARD_THRESHOLD_MS}ms")
        }
        
        // Cập nhật tiến độ
        contentCompletionManager.updateVideoProgress(
            contentId = contentId,
            viewPercentage = percentage,
            isFastForwarded = isFastForwarded
        )
        
        // Kiểm tra hoàn thành
        val isCompleted = contentCompletionManager.isContentCompleted(contentId, "VIDEO")
        if (isCompleted) {
            Log.d(TAG, "✅ VIDEO completed: $contentId")
            onContentCompletedCallback?.invoke(contentId)
        }
    }
    
    /**
     * Xử lý sự kiện AUDIO vị trí thay đổi
     */
    fun handleAudioPositionChanged(contentId: String, currentPositionMs: Long, durationMs: Long) {
        val percentage = if (durationMs > 0) {
            ((currentPositionMs * 100) / durationMs).toInt()
        } else {
            0
        }
        
        // Phát hiện tua nhanh
        val isFastForwarded = mediaProgressManager.updateAudioPosition(contentId, currentPositionMs, durationMs)
        
        if (isFastForwarded) {
            Log.w(TAG, "⚠️ Fast forward detected in AUDIO: $contentId")
            Log.w(TAG, "   - Current position: ${currentPositionMs}ms")
            Log.w(TAG, "   - Duration: ${durationMs}ms")
            Log.w(TAG, "   - Threshold: ${LearningProgressConfig.VIDEO_FAST_FORWARD_THRESHOLD_MS}ms")
        }
        
        // Cập nhật tiến độ
        contentCompletionManager.updateAudioProgress(
            contentId = contentId,
            listenPercentage = percentage,
            isFastForwarded = isFastForwarded
        )
        
        // Kiểm tra hoàn thành
        val isCompleted = contentCompletionManager.isContentCompleted(contentId, "AUDIO")
        if (isCompleted) {
            Log.d(TAG, "✅ AUDIO completed: $contentId")
            onContentCompletedCallback?.invoke(contentId)
        }
    }
    
    /**
     * Lấy quy tắc hoàn thành cho từng loại nội dung
     */
    fun getCompletionRule(contentType: String): String {
        val rule = when (contentType.uppercase()) {
            "TEXT" -> "Xem ≥ ${LearningProgressConfig.TEXT_MIN_VIEW_TIME_SECONDS}s"
            "IMAGE" -> "Xem ≥ ${LearningProgressConfig.IMAGE_MIN_VIEW_TIME_SECONDS}s"
            "PDF" -> "Cuộn ≥ ${LearningProgressConfig.PDF_MIN_SCROLL_PERCENTAGE}% + xem ≥ ${LearningProgressConfig.PDF_MIN_VIEW_TIME_SECONDS}s"
            "VIDEO" -> "Xem ≥ ${LearningProgressConfig.VIDEO_MIN_COMPLETION_PERCENTAGE}% + KHÔNG tua nhanh"
            "AUDIO" -> "Nghe ≥ ${LearningProgressConfig.AUDIO_MIN_COMPLETION_PERCENTAGE}% + KHÔNG tua nhanh"
            else -> "Hoàn thành nội dung"
        }
        Log.d(TAG, "📋 getCompletionRule($contentType) = $rule")
        return rule
    }
    
    /**
     * Kiểm tra xem nội dung có hoàn thành không
     */
    fun isContentCompleted(contentId: String): Boolean {
        val status = contentCompletionManager.getCompletionStatus(contentId)
        return status?.isCompleted ?: false
    }
    
    /**
     * Lấy danh sách nội dung đã hoàn thành
     */
    fun getCompletedContents(): List<String> {
        return contentCompletionManager.getCompletedContents()
    }
    
    /**
     * Lấy tiến độ của một nội dung
     */
    fun getContentProgress(contentId: String): Int {
        return contentCompletionManager.getCompletionStatus(contentId)?.progress ?: 0
    }
    
    /**
     * Xóa trạng thái hoàn thành (khi chuyển sang bài học khác)
     */
    fun clear() {
        Log.d(TAG, "🗑️ clear() - Clearing all completion rules")
        contentCompletionManager.clear()
        mediaProgressManager.clear()
        Log.d(TAG, "   ✅ Cleared successfully")
    }
}
