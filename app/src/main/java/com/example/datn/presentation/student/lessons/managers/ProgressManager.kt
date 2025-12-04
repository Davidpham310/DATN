package com.example.datn.presentation.student.lessons.managers

import android.util.Log

/**
 * Quản lý tiến độ học tập
 * Tính toán và cập nhật phần trăm tiến độ dựa trên nội dung đã xem
 */
class ProgressManager {
    companion object {
        private const val TAG = "ProgressManager"
    }

    /**
     * Tính toán phần trăm tiến độ
     * @param totalContents Tổng số nội dung
     * @param viewedCount Số nội dung đã xem
     * @return Phần trăm tiến độ (0-100)
     */
    fun calculateProgressPercentage(totalContents: Int, viewedCount: Int): Int {
        if (totalContents <= 0) return 0
        val percentage = (viewedCount * 100) / totalContents
        return percentage.coerceIn(0, 100)
    }

    /**
     * Tính toán số nội dung cần xem để đạt phần trăm mục tiêu
     * @param totalContents Tổng số nội dung
     * @param targetPercentage Phần trăm mục tiêu
     * @return Số nội dung cần xem
     */
    fun calculateRequiredViewedCount(totalContents: Int, targetPercentage: Int): Int {
        if (totalContents <= 0) return 0
        val required = (targetPercentage * totalContents) / 100
        return required.coerceIn(0, totalContents)
    }

    /**
     * Kiểm tra xem tiến độ có hoàn thành không
     * @param progressPercentage Phần trăm tiến độ hiện tại
     * @param requiredPercentage Phần trăm yêu cầu (mặc định 100%)
     * @return True nếu hoàn thành
     */
    fun isProgressComplete(progressPercentage: Int, requiredPercentage: Int = 100): Boolean {
        return progressPercentage >= requiredPercentage
    }

    /**
     * Lấy trạng thái tiến độ dưới dạng text
     */
    fun getProgressStatus(progressPercentage: Int): String {
        return when {
            progressPercentage >= 100 -> "Hoàn thành"
            progressPercentage >= 75 -> "Gần hoàn thành"
            progressPercentage >= 50 -> "Đã học nửa"
            progressPercentage > 0 -> "Đang học"
            else -> "Chưa bắt đầu"
        }
    }

    /**
     * Log tiến độ
     */
    fun logProgress(currentIndex: Int, totalContents: Int, progressPercentage: Int) {
        Log.d(
            TAG,
            "Progress: $currentIndex/$totalContents ($progressPercentage%)"
        )
    }
}