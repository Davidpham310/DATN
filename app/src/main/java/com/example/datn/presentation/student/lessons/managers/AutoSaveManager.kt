package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Quản lý tự động lưu tiến độ học tập
 * Lưu tiến độ định kỳ theo khoảng thời gian được chỉ định
 */
class AutoSaveManager(
    private val scope: CoroutineScope,
    private val onAutoSave: () -> Unit
) {
    companion object {
        private const val TAG = "AutoSaveManager"
        private const val AUTO_SAVE_INTERVAL = 10000L  // 10 seconds
    }

    private var autoSaveJob: Job? = null

    /**
     * Bắt đầu tự động lưu tiến độ
     */
    fun startAutoSave() {
        autoSaveJob?.cancel()
        Log.d(TAG, "Starting auto-save with interval=${AUTO_SAVE_INTERVAL}ms")

        autoSaveJob = scope.launch {
            while (isActive) {
                delay(AUTO_SAVE_INTERVAL)
                Log.d(TAG, "Auto-save tick -> calling onAutoSave()")
                onAutoSave()
            }
        }
    }

    /**
     * Dừng tự động lưu tiến độ
     */
    fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
        Log.d(TAG, "Auto-save stopped")
    }

    /**
     * Kiểm tra xem auto-save có đang chạy không
     */
    fun isAutoSaveRunning(): Boolean = autoSaveJob?.isActive == true
}
