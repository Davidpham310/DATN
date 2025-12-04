package com.example.datn.presentation.student.lessons

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.models.StudentLessonProgress

data class StudentLessonViewState(
    val lesson: Lesson? = null,
    val lessonContents: List<LessonContent> = emptyList(),
    val contentUrls: Map<String, String> = emptyMap(),
    val progress: StudentLessonProgress? = null,
    val lessonId: String? = null,
    val studentId: String? = null,
    val currentContentIndex: Int = 0,
    val viewedContentIds: Set<String> = emptySet(),
    val sessionStartTime: Long = 0L,
    val showProgressDialog: Boolean = false,

    // ========== Interaction Tracking Fields ==========
    val lastInteractionTime: Long = 0L,
    val inactivityWarningCount: Int = 0,
    val showInactivityWarning: Boolean = false,
    val shouldAutoExitLesson: Boolean = false,

    // ========== NEW: Media Tracking Fields ==========
    val isMediaPlaying: Boolean = false,
    val currentMediaType: ContentType? = null,
    val mediaDuration: Long = 0L,
    val mediaPosition: Long = 0L,

    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState {

    val currentContent: LessonContent?
        get() = lessonContents.getOrNull(currentContentIndex)

    val canGoPrevious: Boolean
        get() = currentContentIndex > 0

    val canGoNext: Boolean
        get() = currentContentIndex < lessonContents.size - 1

    val progressPercentage: Int
        get() {
            if (lessonContents.isEmpty()) return 0
            
            // Mỗi nội dung hoàn thành 100% = (100 / totalContents) %
            val percentPerContent = 100 / lessonContents.size
            
            // Tính tiến độ từ nội dung đã hoàn thành 100%
            val completedCount = viewedContentIds.size
            var totalProgress = completedCount * percentPerContent
            
            // Chỉ tính partial progress cho VIDEO/AUDIO (có mediaDuration)
            // TEXT/IMAGE/PDF phải được mark as viewed 100% mới tính
            val currentContent = currentContent
            if (currentContent != null && currentContent.id !in viewedContentIds) {
                if (currentContent.contentType == ContentType.VIDEO || currentContent.contentType == ContentType.AUDIO) {
                    // Tính phần trăm xem của video/audio
                    val currentContentProgress = if (mediaDuration > 0) {
                        ((mediaPosition * 100) / mediaDuration).toInt()
                    } else 0
                    
                    // Thêm phần trăm của nội dung hiện tại vào tổng tiến độ
                    // (phần trăm xem / 100) * (100 / totalContents)
                    val partialProgress = (currentContentProgress * percentPerContent) / 100
                    totalProgress += partialProgress
                }
                // TEXT/IMAGE/PDF không tính partial progress, phải hoàn thành 100% mới được tính
            }
            
            return totalProgress.coerceIn(0, 100)
        }

    val isLessonCompleted: Boolean
        get() = progress?.isCompleted == true

    // ========== Interaction Tracking Utilities ==========

    fun isUserActive(thresholdMs: Long = 60000L): Boolean {
        val timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime
        return timeSinceLastInteraction < thresholdMs
    }

    fun getTimeSinceLastInteraction(): Long {
        return System.currentTimeMillis() - lastInteractionTime
    }

    fun hasMaxWarningsReached(maxWarnings: Int = 3): Boolean {
        return inactivityWarningCount >= maxWarnings
    }

    // ========== NEW: Media Tracking Utilities ==========

    /**
     * Check if current content is media (video/audio)
     */
    val isCurrentContentMedia: Boolean
        get() = currentContent?.let {
            it.contentType == ContentType.VIDEO || it.contentType == ContentType.AUDIO
        } ?: false

    /**
     * Get media watch percentage
     */
    val mediaWatchPercentage: Int
        get() = if (mediaDuration > 0) {
            ((mediaPosition * 100) / mediaDuration).toInt()
        } else 0

    /**
     * Check if media has been watched enough (70%)
     */
    val hasWatchedEnoughMedia: Boolean
        get() = mediaWatchPercentage >= 70

    /**
     * Get formatted media position (MM:SS / MM:SS)
     */
    val formattedMediaPosition: String
        get() {
            val currentMin = (mediaPosition / 1000) / 60
            val currentSec = (mediaPosition / 1000) % 60
            val totalMin = (mediaDuration / 1000) / 60
            val totalSec = (mediaDuration / 1000) % 60
            return String.format("%02d:%02d / %02d:%02d", currentMin, currentSec, totalMin, totalSec)
        }
}