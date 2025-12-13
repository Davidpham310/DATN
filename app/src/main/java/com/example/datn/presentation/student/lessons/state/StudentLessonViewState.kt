package com.example.datn.presentation.student.lessons.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.presentation.student.lessons.managers.ContentCompletionStatus

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

    val totalWarningsResetInSession: Int = 0,  // Tổng số lần warning được reset trong session
    val lastWarningResetTime: Long = 0L,       // Thời điểm reset warning gần nhất
    val lastResetInteractionType: String = "", // Loại tương tác đã reset warning gần nhất

    // ========== Media Tracking Fields ==========
    val isMediaPlaying: Boolean = false,
    val currentMediaType: ContentType? = null,
    val mediaDuration: Long = 0L,
    val mediaPosition: Long = 0L,

    // ========== Content Completion Tracking Fields ==========
    val contentCompletionStatus: Map<String, ContentCompletionStatus> = emptyMap(),
    val currentContentViewStartTime: Long = 0L,  // Thời gian bắt đầu xem nội dung hiện tại
    val currentContentElapsedSeconds: Long = 0L, // Thời gian đã xem nội dung hiện tại (giây)
    val isFastForwardDetected: Boolean = false,  // Đã phát hiện tua nhanh
    val totalFastForwardCount: Int = 0,          // Tổng số lần tua nhanh
    val studySeriousnessScore: Int = 100,        // Điểm đánh giá mức độ nghiêm túc (0-100)

    val isAppInBackground: Boolean = false,      // App có đang ở background không
    val isScreenOff: Boolean = false,            // Màn hình có đang tắt không
    val backgroundDurationMs: Long = 0L,         // Thời gian ở background (ms)
    val lastBackgroundTime: Long = 0L,           // Thời điểm vào background gần nhất
    val totalBackgroundTimeMs: Long = 0L,        // Tổng thời gian ở background trong session

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

            val percentPerContent = 100 / lessonContents.size
            val completedCount = viewedContentIds.size
            var totalProgress = completedCount * percentPerContent

            val currentContent = currentContent
            if (currentContent != null && currentContent.id !in viewedContentIds) {
                if (currentContent.contentType == ContentType.VIDEO || currentContent.contentType == ContentType.AUDIO) {
                    val currentContentProgress = if (mediaDuration > 0) {
                        ((mediaPosition * 100) / mediaDuration).toInt()
                    } else 0

                    val partialProgress = (currentContentProgress * percentPerContent) / 100
                    totalProgress += partialProgress
                }
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

    /**
     * Kiểm tra xem user có đang tích cực tương tác không
     * Dựa trên việc warning có được reset nhiều lần không
     */
    val isUserActivelyEngaged: Boolean
        get() = totalWarningsResetInSession > 0 || inactivityWarningCount == 0

    /**
     * Mô tả trạng thái tương tác của user
     */
    val interactionStatusDescription: String
        get() = when {
            inactivityWarningCount == 0 && totalWarningsResetInSession == 0 -> "Đang học bình thường"
            inactivityWarningCount == 0 && totalWarningsResetInSession > 0 -> "Đã quay lại học tập ($totalWarningsResetInSession lần)"
            inactivityWarningCount == 1 -> "Cảnh báo 1: Hãy tương tác để tiếp tục"
            inactivityWarningCount == 2 -> "Cảnh báo 2: Sắp bị thoát tự động"
            inactivityWarningCount >= 3 -> "Cảnh báo tối đa: Sẽ thoát ngay"
            else -> "Không xác định"
        }

    // ========== Media Tracking Utilities ==========

    val isCurrentContentMedia: Boolean
        get() = currentContent?.let {
            it.contentType == ContentType.VIDEO || it.contentType == ContentType.AUDIO
        } ?: false

    val mediaWatchPercentage: Int
        get() = if (mediaDuration > 0) {
            ((mediaPosition * 100) / mediaDuration).toInt()
        } else 0

    val hasWatchedEnoughMedia: Boolean
        get() = mediaWatchPercentage >= 70

    val formattedMediaPosition: String
        get() {
            val currentMin = (mediaPosition / 1000) / 60
            val currentSec = (mediaPosition / 1000) % 60
            val totalMin = (mediaDuration / 1000) / 60
            val totalSec = (mediaDuration / 1000) % 60
            return String.format("%02d:%02d / %02d:%02d", currentMin, currentSec, totalMin, totalSec)
        }

    // ========== Study Seriousness Utilities ==========

    /**
     * Kiểm tra nội dung hiện tại có đủ điều kiện hoàn thành không
     */
    val isCurrentContentEligibleForCompletion: Boolean
        get() {
            val content = currentContent ?: return false
            val status = contentCompletionStatus[content.id]
            return status?.isCompleted == true
        }

    /**
     * Lấy tiến độ hoàn thành của nội dung hiện tại (0-100)
     */
    val currentContentProgress: Int
        get() {
            val content = currentContent ?: return 0
            return contentCompletionStatus[content.id]?.progress ?: 0
        }

    /**
     * Kiểm tra học sinh có học nghiêm túc không
     * - Điểm >= 80: Rất nghiêm túc
     * - Điểm 60-79: Nghiêm túc
     * - Điểm 40-59: Trung bình
     * - Điểm < 40: Không nghiêm túc
     */
    val studySeriousnessLevel: String
        get() = when {
            studySeriousnessScore >= 80 -> "Rất nghiêm túc"
            studySeriousnessScore >= 60 -> "Nghiêm túc"
            studySeriousnessScore >= 40 -> "Trung bình"
            else -> "Không nghiêm túc"
        }

    /**
     * Kiểm tra xem có tua nhanh video/audio không
     */
    val hasDetectedCheating: Boolean
        get() = isFastForwardDetected || totalFastForwardCount > 0

    /**
     * Tính tỷ lệ nội dung đã hoàn thành đúng quy tắc
     */
    val properCompletionRate: Int
        get() {
            if (lessonContents.isEmpty()) return 0
            val properlyCompleted = contentCompletionStatus.count { it.value.isCompleted && !it.value.isFastForwarded }
            return (properlyCompleted * 100) / lessonContents.size
        }

    /**
     * Tổng thời gian học thực tế (không tính thời gian tua)
     */
    val totalActualStudyTimeSeconds: Long
        get() = contentCompletionStatus.values.sumOf {
            if (!it.isFastForwarded) it.progress.toLong() else 0L
        }


    /**
     * Kiểm tra app có đang inactive không (background hoặc screen off)
     */
    val isAppInactive: Boolean
        get() = isAppInBackground || isScreenOff

    /**
     * Lấy thời gian học thực tế (trừ thời gian background)
     */
    val effectiveStudyTimeSeconds: Long
        get() {
            val totalSessionTime = if (sessionStartTime > 0) {
                (System.currentTimeMillis() - sessionStartTime) / 1000
            } else 0L
            val backgroundTimeSeconds = totalBackgroundTimeMs / 1000
            return (totalSessionTime - backgroundTimeSeconds).coerceAtLeast(0)
        }

    /**
     * Tỷ lệ thời gian học thực tế so với tổng thời gian session
     */
    val studyEfficiencyPercentage: Int
        get() {
            val totalSessionTime = if (sessionStartTime > 0) {
                System.currentTimeMillis() - sessionStartTime
            } else 0L
            if (totalSessionTime <= 0) return 100
            val activeTime = totalSessionTime - totalBackgroundTimeMs
            return ((activeTime * 100) / totalSessionTime).toInt().coerceIn(0, 100)
        }
}
