package com.example.datn.presentation.student.lessons.managers

import android.util.Log

/**
 * Quản lý phiên học tập
 * Theo dõi thời gian bắt đầu và kết thúc phiên học
 */
class SessionTimer {
    companion object {
        private const val TAG = "SessionTimer"
    }

    private var sessionStartTime: Long = 0L
    private var sessionEndTime: Long = 0L

    /**
     * Bắt đầu phiên học
     */
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        sessionEndTime = 0L
        Log.d(TAG, "Session started at $sessionStartTime")
    }

    /**
     * Kết thúc phiên học
     */
    fun stopSession() {
        sessionEndTime = System.currentTimeMillis()
        Log.d(TAG, "Session ended at $sessionEndTime")
    }

    /**
     * Lấy thời gian phiên học (giây)
     */
    fun getSessionDuration(): Long {
        val endTime = if (sessionEndTime > 0) sessionEndTime else System.currentTimeMillis()
        return (endTime - sessionStartTime) / 1000
    }

    /**
     * Lấy thời gian bắt đầu phiên
     */
    fun getSessionStartTime(): Long = sessionStartTime

    /**
     * Lấy thời gian kết thúc phiên
     */
    fun getSessionEndTime(): Long = sessionEndTime

    /**
     * Kiểm tra xem phiên có đang chạy không
     */
    fun isSessionActive(): Boolean = sessionStartTime > 0 && sessionEndTime == 0L
}