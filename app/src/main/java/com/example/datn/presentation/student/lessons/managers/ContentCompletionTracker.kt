package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.services.CompletionValidator

/**
 * Tracks completion status of individual lesson contents
 * Provides real-time completion percentage and validation
 */
class ContentCompletionTracker {
    companion object {
        private const val TAG = "ContentCompletionTracker"
    }

    private val validator = CompletionValidator()

    // Track metrics for current content
    private var currentContentId: String? = null
    private var currentContentType: ContentType? = null
    private var contentStartTime: Long = 0L
    private var contentViewTime: Long = 0L
    private var isContentClicked: Boolean = false

    // Media metrics
    private var mediaDuration: Long = 0L
    private var mediaPosition: Long = 0L
    private var mediaStartTime: Long = 0L

    // PDF metrics
    private var pdfPagesViewed: Int = 0
    private var pdfTotalPages: Int = 0

    // Game metrics
    private var isGameCompleted: Boolean = false

    /**
     * Start tracking a new content
     */
    fun startTracking(contentId: String, contentType: ContentType) {
        Log.d(TAG, "startTracking() contentId=$contentId, type=$contentType")

        currentContentId = contentId
        currentContentType = contentType
        contentStartTime = System.currentTimeMillis()
        contentViewTime = 0L
        isContentClicked = false

        // Reset media metrics
        mediaDuration = 0L
        mediaPosition = 0L
        mediaStartTime = 0L

        // Reset PDF metrics
        pdfPagesViewed = 0
        pdfTotalPages = 0

        // Reset game metrics
        isGameCompleted = false
    }

    /**
     * Record user click/interaction with content
     */
    fun recordClick() {
        Log.d(TAG, "recordClick() for $currentContentType")
        isContentClicked = true
    }

    /**
     * Update media playback info
     */
    fun updateMediaInfo(duration: Long, position: Long) {
        if (mediaDuration != duration || mediaPosition != position) {
            mediaDuration = duration
            mediaPosition = position

            // Update view time based on media position
            if (mediaStartTime == 0L) {
                mediaStartTime = System.currentTimeMillis()
            }
        }
    }

    /**
     * Record PDF page view
     */
    fun recordPdfPageView(currentPage: Int, totalPages: Int) {
        Log.d(TAG, "recordPdfPageView() page=$currentPage/$totalPages")
        pdfPagesViewed = currentPage
        pdfTotalPages = totalPages
    }

    /**
     * Record game completion
     */
    fun recordGameCompletion() {
        Log.d(TAG, "recordGameCompletion()")
        isGameCompleted = true
    }

    /**
     * Get current view time in milliseconds
     */
    fun getViewTime(): Long {
        return if (contentStartTime > 0) {
            System.currentTimeMillis() - contentStartTime
        } else 0L
    }

    /**
     * Get watch percentage for video/audio
     */
    fun getWatchPercentage(): Int {
        return if (mediaDuration > 0) {
            ((mediaPosition * 100) / mediaDuration).toInt()
        } else 0
    }

    /**
     * Check if current content is completed
     */
    fun isCompleted(): Boolean {
        val contentType = currentContentType ?: return false

        return validator.isContentCompleted(
            contentType = contentType,
            viewTime = getViewTime(),
            watchPercent = getWatchPercentage(),
            listenPercent = getWatchPercentage(), // Same as watch for audio
            pagesViewed = pdfPagesViewed,
            totalPages = pdfTotalPages,
            isClicked = isContentClicked,
            isGameCompleted = isGameCompleted
        )
    }

    /**
     * Get completion percentage (0-100)
     */
    fun getCompletionPercentage(): Int {
        val contentType = currentContentType ?: return 0

        return validator.getCompletionPercentage(
            contentType = contentType,
            viewTime = getViewTime(),
            watchPercent = getWatchPercentage(),
            listenPercent = getWatchPercentage(),
            pagesViewed = pdfPagesViewed,
            totalPages = pdfTotalPages,
            isClicked = isContentClicked,
            isGameCompleted = isGameCompleted
        )
    }

    /**
     * Get completion reason/message
     */
    fun getCompletionReason(): String {
        val contentType = currentContentType ?: return "Không xác định"

        return validator.getCompletionReason(
            contentType = contentType,
            viewTime = getViewTime(),
            watchPercent = getWatchPercentage(),
            listenPercent = getWatchPercentage(),
            pagesViewed = pdfPagesViewed,
            totalPages = pdfTotalPages,
            isClicked = isContentClicked,
            isGameCompleted = isGameCompleted
        )
    }

    /**
     * Reset tracker
     */
    fun reset() {
        Log.d(TAG, "reset()")
        currentContentId = null
        currentContentType = null
        contentStartTime = 0L
        contentViewTime = 0L
        isContentClicked = false
        mediaDuration = 0L
        mediaPosition = 0L
        mediaStartTime = 0L
        pdfPagesViewed = 0
        pdfTotalPages = 0
        isGameCompleted = false
    }

    /**
     * Get current tracking state (for debugging)
     */
    fun getDebugInfo(): String {
        return """
            Content: $currentContentId ($currentContentType)
            ViewTime: ${getViewTime()}ms
            WatchPercent: ${getWatchPercentage()}%
            Clicked: $isContentClicked
            Completed: ${isCompleted()}
            Reason: ${getCompletionReason()}
        """.trimIndent()
    }
}
