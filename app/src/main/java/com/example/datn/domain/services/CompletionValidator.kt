package com.example.datn.domain.services

import android.util.Log
import com.example.datn.domain.models.ContentType

/**
 * Validates whether a lesson content has been completed based on content type
 * and viewing/interaction metrics.
 *
 * Completion Requirements:
 * - TEXT: Click + 12 seconds minimum viewing time
 * - VIDEO: 70% watch time minimum
 * - AUDIO: 65% listen time minimum
 * - IMAGE: Click + 5 seconds minimum viewing time
 * - PDF: 70% pages viewed + 2.5 seconds per page
 * - MINIGAME: Complete the game
 */
class CompletionValidator {
    companion object {
        private const val TAG = "CompletionValidator"

        // Minimum viewing times (milliseconds)
        private const val TEXT_MIN_VIEW_TIME = 12000L      // 12 seconds
        private const val IMAGE_MIN_VIEW_TIME = 5000L      // 5 seconds
        private const val PDF_MIN_TIME_PER_PAGE = 2500L    // 2.5 seconds per page

        // Minimum completion percentages
        private const val VIDEO_MIN_WATCH_PERCENT = 70     // 70%
        private const val AUDIO_MIN_LISTEN_PERCENT = 65    // 65%
        private const val PDF_MIN_PAGE_PERCENT = 70        // 70%
    }

    /**
     * Check if content is completed based on type and metrics
     */
    fun isContentCompleted(
        contentType: ContentType,
        viewTime: Long = 0L,
        watchPercent: Int = 0,
        listenPercent: Int = 0,
        pagesViewed: Int = 0,
        totalPages: Int = 0,
        isClicked: Boolean = false,
        isGameCompleted: Boolean = false
    ): Boolean {
        return when (contentType) {
            ContentType.TEXT -> {
                val completed = isClicked && viewTime >= TEXT_MIN_VIEW_TIME
                Log.d(TAG, "TEXT completion: clicked=$isClicked, viewTime=$viewTime ms, completed=$completed")
                completed
            }
            ContentType.VIDEO -> {
                val completed = watchPercent >= VIDEO_MIN_WATCH_PERCENT
                Log.d(TAG, "VIDEO completion: watched=$watchPercent%, completed=$completed")
                completed
            }
            ContentType.AUDIO -> {
                val completed = listenPercent >= AUDIO_MIN_LISTEN_PERCENT
                Log.d(TAG, "AUDIO completion: listened=$listenPercent%, completed=$completed")
                completed
            }
            ContentType.IMAGE -> {
                val completed = isClicked && viewTime >= IMAGE_MIN_VIEW_TIME
                Log.d(TAG, "IMAGE completion: clicked=$isClicked, viewTime=$viewTime ms, completed=$completed")
                completed
            }
            ContentType.PDF -> {
                val pagePercent = if (totalPages > 0) (pagesViewed * 100) / totalPages else 0
                val minTimeRequired = totalPages * PDF_MIN_TIME_PER_PAGE
                val completed = pagePercent >= PDF_MIN_PAGE_PERCENT && viewTime >= minTimeRequired
                Log.d(TAG, "PDF completion: pages=$pagePercent%, viewTime=$viewTime ms (need $minTimeRequired), completed=$completed")
                completed
            }
            ContentType.MINIGAME -> {
                Log.d(TAG, "MINIGAME completion: completed=$isGameCompleted")
                isGameCompleted
            }
        }
    }

    /**
     * Get completion percentage (0-100) for a content
     */
    fun getCompletionPercentage(
        contentType: ContentType,
        viewTime: Long = 0L,
        watchPercent: Int = 0,
        listenPercent: Int = 0,
        pagesViewed: Int = 0,
        totalPages: Int = 0,
        isClicked: Boolean = false,
        isGameCompleted: Boolean = false
    ): Int {
        return when (contentType) {
            ContentType.TEXT -> {
                if (!isClicked) return 0
                val timePercent = ((viewTime * 100) / TEXT_MIN_VIEW_TIME).toInt()
                timePercent.coerceIn(0, 100)
            }
            ContentType.VIDEO -> watchPercent.coerceIn(0, 100)
            ContentType.AUDIO -> listenPercent.coerceIn(0, 100)
            ContentType.IMAGE -> {
                if (!isClicked) return 0
                val timePercent = ((viewTime * 100) / IMAGE_MIN_VIEW_TIME).toInt()
                timePercent.coerceIn(0, 100)
            }
            ContentType.PDF -> {
                val pagePercent = if (totalPages > 0) (pagesViewed * 100) / totalPages else 0
                val minTimeRequired = totalPages * PDF_MIN_TIME_PER_PAGE
                val timePercent = if (minTimeRequired > 0) ((viewTime * 100) / minTimeRequired).toInt() else 0
                ((pagePercent + timePercent) / 2).coerceIn(0, 100)
            }
            ContentType.MINIGAME -> if (isGameCompleted) 100 else 0
        }
    }

    /**
     * Get detailed completion reason/message
     */
    fun getCompletionReason(
        contentType: ContentType,
        viewTime: Long = 0L,
        watchPercent: Int = 0,
        listenPercent: Int = 0,
        pagesViewed: Int = 0,
        totalPages: Int = 0,
        isClicked: Boolean = false,
        isGameCompleted: Boolean = false
    ): String {
        return when (contentType) {
            ContentType.TEXT -> {
                when {
                    !isClicked -> "Vui lòng click vào nội dung để bắt đầu"
                    viewTime < TEXT_MIN_VIEW_TIME -> "Vui lòng đọc ít nhất 12 giây (đã: ${viewTime / 1000}s)"
                    else -> "Hoàn thành ✓"
                }
            }
            ContentType.VIDEO -> {
                when {
                    watchPercent < VIDEO_MIN_WATCH_PERCENT -> "Vui lòng xem ít nhất 70% video (đã: $watchPercent%)"
                    else -> "Hoàn thành ✓"
                }
            }
            ContentType.AUDIO -> {
                when {
                    listenPercent < AUDIO_MIN_LISTEN_PERCENT -> "Vui lòng nghe ít nhất 65% âm thanh (đã: $listenPercent%)"
                    else -> "Hoàn thành ✓"
                }
            }
            ContentType.IMAGE -> {
                when {
                    !isClicked -> "Vui lòng click vào hình ảnh để xem"
                    viewTime < IMAGE_MIN_VIEW_TIME -> "Vui lòng xem ít nhất 5 giây (đã: ${viewTime / 1000}s)"
                    else -> "Hoàn thành ✓"
                }
            }
            ContentType.PDF -> {
                val pagePercent = if (totalPages > 0) (pagesViewed * 100) / totalPages else 0
                val minTimeRequired = totalPages * PDF_MIN_TIME_PER_PAGE
                when {
                    pagePercent < PDF_MIN_PAGE_PERCENT -> "Vui lòng xem ít nhất 70% trang (đã: $pagePercent%)"
                    viewTime < minTimeRequired -> "Vui lòng xem đủ thời gian (đã: ${viewTime / 1000}s, cần: ${minTimeRequired / 1000}s)"
                    else -> "Hoàn thành ✓"
                }
            }
            ContentType.MINIGAME -> {
                when {
                    !isGameCompleted -> "Vui lòng hoàn thành trò chơi"
                    else -> "Hoàn thành ✓"
                }
            }
        }
    }
}
