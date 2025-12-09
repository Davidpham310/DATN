package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Quản lý hoàn thành nội dung bài học
 * 
 * Trách nhiệm:
 * - Kiểm tra xem nội dung có đáp ứng yêu cầu hoàn thành không
 * - Theo dõi thời gian xem cho TEXT, IMAGE
 * - Theo dõi % cuộn cho PDF
 * - Theo dõi % xem cho VIDEO, AUDIO
 * - Phát hiện tua nhanh video
 * - Gọi callback khi nội dung hoàn thành
 */
class ContentCompletionManager @Inject constructor() {
    
    // Lưu trữ trạng thái hoàn thành của từng nội dung
    // MutableStateFlow automatically replays the latest value to new subscribers
    private val _completionStatus = MutableStateFlow<Map<String, ContentCompletionStatus>>(emptyMap())
    val completionStatus: StateFlow<Map<String, ContentCompletionStatus>> = _completionStatus.asStateFlow()
    
    init {
        Log.d("ContentCompletionManager", "🔧 ContentCompletionManager initialized")
    }
    
    // Callback khi nội dung hoàn thành
    private var onContentCompletedCallback: ((String) -> Unit)? = null
    
    /**
     * Đăng ký callback khi nội dung hoàn thành
     */
    fun setOnContentCompletedCallback(callback: (String) -> Unit) {
        onContentCompletedCallback = callback
    }
    
    /**
     * Kiểm tra xem nội dung có hoàn thành không
     * 
     * @param contentId ID của nội dung
     * @param contentType Loại nội dung (TEXT, IMAGE, PDF, VIDEO, AUDIO)
     * @return true nếu hoàn thành, false nếu chưa
     */
    fun isContentCompleted(contentId: String, contentType: String): Boolean {
        val status = _completionStatus.value[contentId] ?: return false
        return status.isCompleted
    }
    
    /**
     * Cập nhật thời gian xem cho TEXT, IMAGE
     * 
     * @param contentId ID của nội dung
     * @param elapsedTimeSeconds Thời gian đã xem (giây)
     * @param contentType Loại nội dung
     */
    fun updateViewTime(contentId: String, elapsedTimeSeconds: Long, contentType: String) {
        Log.d("ContentCompletionManager", "📝 updateViewTime called: $contentId, $contentType, ${elapsedTimeSeconds}s")
        
        val minTime = when (contentType) {
            "TEXT" -> LearningProgressConfig.TEXT_MIN_VIEW_TIME_SECONDS
            "IMAGE" -> LearningProgressConfig.IMAGE_MIN_VIEW_TIME_SECONDS
            else -> {
                Log.d("ContentCompletionManager", "   - Unknown content type: $contentType")
                return
            }
        }
        
        Log.d("ContentCompletionManager", "   - Min time required: ${minTime}s")
        val isCompleted = elapsedTimeSeconds >= minTime
        Log.d("ContentCompletionManager", "   - Is completed: $isCompleted")
        updateStatus(contentId, contentType, isCompleted, elapsedTimeSeconds.toInt())
    }
    
    /**
     * Cập nhật % cuộn cho PDF
     * 
     * @param contentId ID của nội dung
     * @param scrollPercentage Phần trăm cuộn (0-100)
     */
    fun updatePdfScrollProgress(contentId: String, scrollPercentage: Int) {
        val minPercentage = LearningProgressConfig.PDF_MIN_SCROLL_PERCENTAGE
        val isCompleted = scrollPercentage >= minPercentage
        updateStatus(contentId, "PDF", isCompleted, scrollPercentage)
    }
    
    /**
     * Cập nhật % xem cho VIDEO
     * 
     * @param contentId ID của nội dung
     * @param viewPercentage Phần trăm đã xem (0-100)
     * @param isFastForwarded true nếu phát hiện tua nhanh
     */
    fun updateVideoProgress(contentId: String, viewPercentage: Int, isFastForwarded: Boolean = false) {
        val minPercentage = LearningProgressConfig.VIDEO_MIN_COMPLETION_PERCENTAGE
        val isCompleted = viewPercentage >= minPercentage && !isFastForwarded
        updateStatus(contentId, "VIDEO", isCompleted, viewPercentage, isFastForwarded)
    }
    
    /**
     * Cập nhật % nghe cho AUDIO
     * 
     * @param contentId ID của nội dung
     * @param listenPercentage Phần trăm đã nghe (0-100)
     * @param isFastForwarded true nếu phát hiện tua nhanh
     */
    fun updateAudioProgress(contentId: String, listenPercentage: Int, isFastForwarded: Boolean = false) {
        val minPercentage = LearningProgressConfig.AUDIO_MIN_COMPLETION_PERCENTAGE
        val isCompleted = listenPercentage >= minPercentage && !isFastForwarded
        updateStatus(contentId, "AUDIO", isCompleted, listenPercentage, isFastForwarded)
    }
    
    /**
     * Đánh dấu nội dung là đã xem
     */
    fun markAsViewed(contentId: String, contentType: String) {
        updateStatus(contentId, contentType, true, 100)
    }
    
    /**
     * Lấy thông tin hoàn thành của một nội dung
     */
    fun getCompletionStatus(contentId: String): ContentCompletionStatus? {
        return _completionStatus.value[contentId]
    }
    
    /**
     * Lấy danh sách nội dung đã hoàn thành
     */
    fun getCompletedContents(): List<String> {
        return _completionStatus.value
            .filter { it.value.isCompleted }
            .map { it.key }
    }
    
    /**
     * Lấy danh sách nội dung chưa hoàn thành
     */
    fun getIncompleteContents(): List<String> {
        return _completionStatus.value
            .filter { !it.value.isCompleted }
            .map { it.key }
    }
    
    /**
     * Xóa trạng thái hoàn thành (khi chuyển sang bài học khác)
     */
    fun clear() {
        _completionStatus.value = emptyMap()
    }
    
    private fun updateStatus(
        contentId: String,
        contentType: String,
        isCompleted: Boolean,
        progress: Int,
        isFastForwarded: Boolean = false
    ) {
        Log.d("ContentCompletionManager", "🔄 updateStatus called: $contentId, isCompleted=$isCompleted, progress=$progress")
        
        val currentStatus = _completionStatus.value.toMutableMap()
        val previousStatus = currentStatus[contentId]
        val wasCompleted = previousStatus?.isCompleted ?: false
        
        Log.d("ContentCompletionManager", "   - Previous status: $previousStatus")
        Log.d("ContentCompletionManager", "   - Was completed: $wasCompleted")
        
        val newStatus = ContentCompletionStatus(
            contentId = contentId,
            contentType = contentType,
            isCompleted = isCompleted,
            progress = progress,
            isFastForwarded = isFastForwarded,
            lastUpdatedTime = System.currentTimeMillis()
        )
        currentStatus[contentId] = newStatus
        
        Log.d("ContentCompletionManager", "   - Emitting new status: $newStatus")
        // Force emit by creating a new map instance using update()
        _completionStatus.update { currentStatus.toMap() }
        Log.d("ContentCompletionManager", "   - Emitted. Total items: ${_completionStatus.value.size}")
        
        // Gọi callback nếu nội dung vừa hoàn thành (từ chưa hoàn thành → hoàn thành)
        if (isCompleted && !wasCompleted) {
            Log.d("ContentCompletionManager", "✅ Content completed: $contentId ($contentType)")
            onContentCompletedCallback?.invoke(contentId)
        }
    }
}

/**
 * Trạng thái hoàn thành của một nội dung
 */
data class ContentCompletionStatus(
    val contentId: String,
    val contentType: String,
    val isCompleted: Boolean,
    val progress: Int,  // Thời gian (giây) hoặc phần trăm (%)
    val isFastForwarded: Boolean = false,
    val lastUpdatedTime: Long = System.currentTimeMillis()
)
