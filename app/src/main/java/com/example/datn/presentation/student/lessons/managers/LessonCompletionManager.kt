package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import com.example.datn.domain.models.LessonContent

/**
 * Manages overall lesson completion status
 * Enforces strict sequential completion requirement
 */
class LessonCompletionManager {
    companion object {
        private const val TAG = "LessonCompletionManager"
    }

    private val contentTracker = ContentCompletionTracker()

    // Track completed contents
    private val completedContents = mutableSetOf<String>()
    private var totalContents = 0

    /**
     * Initialize with lesson contents
     */
    fun initialize(contents: List<LessonContent>) {
        Log.d(TAG, "initialize() with ${contents.size} contents")
        totalContents = contents.size
        completedContents.clear()
    }

    /**
     * Mark content as completed
     */
    fun markContentCompleted(contentId: String) {
        Log.d(TAG, "markContentCompleted() contentId=$contentId")
        completedContents.add(contentId)
    }

    /**
     * Check if a specific content is completed
     */
    fun isContentCompleted(contentId: String): Boolean {
        return contentId in completedContents
    }

    /**
     * Get number of completed contents
     */
    fun getCompletedCount(): Int {
        return completedContents.size
    }

    /**
     * Get total contents
     */
    fun getTotalCount(): Int {
        return totalContents
    }

    /**
     * Get overall lesson completion percentage
     */
    fun getLessonCompletionPercentage(): Int {
        if (totalContents == 0) return 0
        return (completedContents.size * 100) / totalContents
    }

    /**
     * Check if lesson is fully completed (100%)
     */
    fun isLessonFullyCompleted(): Boolean {
        return completedContents.size == totalContents && totalContents > 0
    }

    /**
     * Check if student can access next content
     * Enforces strict sequential completion
     */
    fun canAccessContent(
        contents: List<LessonContent>,
        targetIndex: Int
    ): Boolean {
        // First content is always accessible
        if (targetIndex == 0) {
            Log.d(TAG, "canAccessContent() index=$targetIndex - FIRST CONTENT, allowed")
            return true
        }

        // For other contents, check if all previous contents are completed
        for (index in 0 until targetIndex) {
            val content = contents.getOrNull(index) ?: return false
            if (!isContentCompleted(content.id)) {
                Log.d(TAG, "canAccessContent() index=$targetIndex - BLOCKED (previous content ${index} not completed)")
                return false
            }
        }

        Log.d(TAG, "canAccessContent() index=$targetIndex - ALLOWED (all previous contents completed)")
        return true
    }

    /**
     * Get max accessible content index
     * Returns the highest index the student can access
     */
    fun getMaxAccessibleIndex(contents: List<LessonContent>): Int {
        if (contents.isEmpty()) return -1

        // First content is always accessible
        if (completedContents.isEmpty()) {
            Log.d(TAG, "getMaxAccessibleIndex() - no completed contents, returning 0")
            return 0
        }

        // Find the last completed content
        var maxAccessibleIndex = 0
        for (index in contents.indices) {
            val content = contents[index]
            if (isContentCompleted(content.id)) {
                maxAccessibleIndex = index
            } else {
                // Stop at first incomplete content
                break
            }
        }

        // Allow access to next content (if exists)
        val result = (maxAccessibleIndex + 1).coerceAtMost(contents.lastIndex)
        Log.d(TAG, "getMaxAccessibleIndex() - returning $result (completed up to index $maxAccessibleIndex)")
        return result
    }

    /**
     * Get list of completed content IDs
     */
    fun getCompletedContentIds(): Set<String> {
        return completedContents.toSet()
    }

    /**
     * Get list of incomplete content IDs
     */
    fun getIncompleteContentIds(allContents: List<LessonContent>): List<String> {
        return allContents
            .filter { it.id !in completedContents }
            .map { it.id }
    }

    /**
     * Get next incomplete content
     */
    fun getNextIncompleteContent(allContents: List<LessonContent>): LessonContent? {
        return allContents.firstOrNull { it.id !in completedContents }
    }

    /**
     * Get completion summary
     */
    fun getCompletionSummary(allContents: List<LessonContent>): String {
        val completed = completedContents.size
        val total = allContents.size
        val percentage = getLessonCompletionPercentage()

        return "Hoàn thành: $completed/$total ($percentage%)"
    }

    /**
     * Get detailed completion report
     */
    fun getDetailedReport(allContents: List<LessonContent>): String {
        val sb = StringBuilder()
        sb.append("=== Báo Cáo Hoàn Thành Bài Học ===\n")
        sb.append("Tổng nội dung: $totalContents\n")
        sb.append("Đã hoàn thành: ${completedContents.size}\n")
        sb.append("Hoàn thành: ${getLessonCompletionPercentage()}%\n")
        sb.append("Trạng thái: ${if (isLessonFullyCompleted()) "✓ HOÀN THÀNH" else "⏳ CHƯA HOÀN THÀNH"}\n")
        sb.append("\nChi tiết:\n")

        allContents.forEachIndexed { index, content ->
            val status = if (isContentCompleted(content.id)) "✓" else "✗"
            sb.append("$status ${index + 1}. ${content.title} (${content.contentType})\n")
        }

        return sb.toString()
    }

    /**
     * Reset completion tracker
     */
    fun reset() {
        Log.d(TAG, "reset()")
        completedContents.clear()
        contentTracker.reset()
    }
}
