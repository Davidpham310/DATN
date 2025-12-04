package com.example.datn.presentation.student.lessons.managers

import android.util.Log

/**
 * Quản lý ghi nhận tương tác của học sinh
 * Theo dõi thời gian tương tác cuối cùng, loại tương tác, và phát hiện hành vi bất thường
 * Cơ chế: Nếu vượt quá X giây không tương tác → cảnh báo → nếu tiếp tục → đánh dấu "bỏ học giữa chừng"
 */
class InteractionManager {
    companion object {
        private const val TAG = "InteractionManager"
    }

    private var lastInteractionTime: Long = System.currentTimeMillis()
    private var interactionCount: Int = 0  // Số lần tương tác
    private var lastInteractionType: String = "NONE"  // Loại tương tác cuối
    private var consecutiveInactivityWarnings: Int = 0  // Số lần cảnh báo liên tiếp

    /**
     * Ghi nhận tương tác từ học sinh
     * @param interactionType Loại tương tác (CLICK, SCROLL, SWIPE, TAP, LONG_PRESS, etc.)
     */
    fun recordInteraction(interactionType: String = "CLICK") {
        lastInteractionTime = System.currentTimeMillis()
        interactionCount++
        lastInteractionType = interactionType
        // KHÔNG reset consecutiveInactivityWarnings ở đây
        // Chỉ reset khi người dùng click "Tiếp tục học" (continueLesson)
        Log.d(TAG, "Interaction recorded: type=$interactionType, count=$interactionCount, time=$lastInteractionTime")
    }

    /**
     * Lấy thời gian kể từ tương tác cuối cùng (ms)
     */
    fun getTimeSinceLastInteraction(): Long {
        return System.currentTimeMillis() - lastInteractionTime
    }

    /**
     * Kiểm tra xem học sinh có hoạt động không (dựa trên ngưỡng)
     */
    fun isUserActive(thresholdMs: Long): Boolean {
        return getTimeSinceLastInteraction() < thresholdMs
    }

    /**
     * Lấy loại tương tác cuối cùng
     */
    fun getLastInteractionType(): String = lastInteractionType

    /**
     * Lấy số lần tương tác
     */
    fun getInteractionCount(): Int = interactionCount

    /**
     * Ghi nhận cảnh báo không tương tác
     */
    fun recordInactivityWarning() {
        consecutiveInactivityWarnings++
        Log.w(TAG, "Inactivity warning recorded: count=$consecutiveInactivityWarnings")
    }

    /**
     * Lấy số lần cảnh báo không tương tác liên tiếp
     */
    fun getConsecutiveInactivityWarnings(): Int = consecutiveInactivityWarnings

    /**
     * Kiểm tra xem học sinh có bỏ học giữa chừng không
     * (3 lần cảnh báo liên tiếp mà không tương tác)
     */
    fun isAbandonedLesson(): Boolean {
        return consecutiveInactivityWarnings >= 3
    }

    /**
     * Kiểm tra hành vi bất thường
     * - Không tương tác quá lâu
     * - Quá ít tương tác (< 1 tương tác mỗi phút)
     */
    fun isSuspiciousBehavior(sessionDurationSeconds: Long): Boolean {
        val interactionsPerMinute = if (sessionDurationSeconds > 0) {
            (interactionCount * 60) / sessionDurationSeconds.toInt()
        } else {
            0
        }
        
        val isSuspicious = interactionsPerMinute < 1 && sessionDurationSeconds > 300  // < 1 tương tác/phút trong 5 phút
        
        if (isSuspicious) {
            Log.w(TAG, "Suspicious behavior: Only $interactionCount interactions in ${sessionDurationSeconds}s (${interactionsPerMinute}/min)")
        }
        
        return isSuspicious
    }

    /**
     * Reset thời gian tương tác
     */
    fun resetInteractionTime() {
        lastInteractionTime = System.currentTimeMillis()
        consecutiveInactivityWarnings = 0
        Log.d(TAG, "Interaction time reset")
    }

    /**
     * Lấy thời gian tương tác cuối cùng
     */
    fun getLastInteractionTime(): Long = lastInteractionTime

    /**
     * Reset tất cả dữ liệu (khi kết thúc bài học)
     */
    fun resetAll() {
        lastInteractionTime = System.currentTimeMillis()
        interactionCount = 0
        lastInteractionType = "NONE"
        consecutiveInactivityWarnings = 0
        Log.d(TAG, "All interaction data reset")
    }
}
