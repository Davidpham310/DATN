package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Quản lý phát hiện không tương tác của học sinh
 * Theo dõi và kích hoạt cảnh báo khi vượt quá ngưỡng thời gian
 */
class InactivityManager(
    private val scope: CoroutineScope,
    private val interactionManager: InteractionManager,
    private val onInactivityDetected: (warningCount: Int) -> Unit
) {
    companion object {
        private const val TAG = "InactivityManager"
        private const val INACTIVITY_THRESHOLD = 10000L  // 60 seconds
        private const val MAX_WARNINGS = 3
    }

    private var inactivityCheckJob: Job? = null
    private var warningCount: Int = 0

    /**
     * Bắt đầu kiểm tra không tương tác
     * Kiểm tra mỗi 1 giây để phát hiện chính xác khi vượt 60 giây
     */
    fun startInactivityCheck() {
        inactivityCheckJob?.cancel()
        // KHÔNG reset warningCount - nó sẽ tiếp tục đếm từ giá trị hiện tại
        Log.d(TAG, "Starting inactivity check (warningCount=$warningCount)")

        inactivityCheckJob = scope.launch {
            while (isActive) {
                delay(1000)  // Kiểm tra mỗi 1 giây

                // Nếu đã đạt MAX_WARNINGS, dừng kiểm tra
                if (warningCount >= MAX_WARNINGS) {
                    Log.d(TAG, "Already at max warnings, stopping check")
                    stopInactivityCheck()
                    break
                }

                val timeSinceLastInteraction = interactionManager.getTimeSinceLastInteraction()
                Log.d(TAG, "Inactivity check: timeSinceLastInteraction=${timeSinceLastInteraction}ms")

                if (timeSinceLastInteraction >= INACTIVITY_THRESHOLD) {
                    warningCount++
                    Log.d(TAG, "Inactivity detected! Warning count: $warningCount/$MAX_WARNINGS")
                    onInactivityDetected(warningCount)

                    if (warningCount >= MAX_WARNINGS) {
                        Log.d(TAG, "Max warnings reached, stopping check")
                        stopInactivityCheck()
                        break
                    } else {
                        // Dừng kiểm tra tạm thời để chờ người dùng phản hồi
                        // Sẽ restart khi continueLesson() được gọi
                        Log.d(TAG, "Pausing inactivity check, waiting for user response")
                        break
                    }
                }
            }
        }
    }

    /**
     * Dừng kiểm tra không tương tác
     */
    fun stopInactivityCheck() {
        inactivityCheckJob?.cancel()
        inactivityCheckJob = null
        Log.d(TAG, "Inactivity check stopped")
    }

    /**
     * Reset bộ đếm cảnh báo
     */
    fun resetWarningCount() {
        warningCount = 0
        Log.d(TAG, "Warning count reset to 0")
    }

    /**
     * Lấy số lần cảnh báo hiện tại
     */
    fun getWarningCount(): Int = warningCount

    /**
     * Kiểm tra xem đã đạt cảnh báo tối đa chưa
     */
    fun hasMaxWarningsReached(): Boolean = warningCount >= MAX_WARNINGS
}