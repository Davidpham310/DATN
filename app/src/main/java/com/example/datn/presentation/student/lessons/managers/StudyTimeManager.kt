package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quản lý thời gian học tập
 *
 * Trách nhiệm:
 * - Theo dõi thời gian học tập
 * - Cộng dồn thời gian khi chuyển nội dung
 * - Cung cấp thông tin thời gian học
 * - Không reset thời gian khi state thay đổi
 * - Tạm dừng khi app vào background
 * - Loại trừ thời gian background khỏi tổng thời gian học
 */
class StudyTimeManager @Inject constructor(
    private val coroutineScope: CoroutineScope
) {

    private val TAG = "StudyTimeManager"

    // Thời gian học tập tích lũy (giây)
    private val _totalTimeSpent = MutableStateFlow(0L)
    val totalTimeSpent: StateFlow<Long> = _totalTimeSpent.asStateFlow()

    // Thời gian bắt đầu phiên học
    private val _sessionStartTime = MutableStateFlow(System.currentTimeMillis())
    val sessionStartTime: StateFlow<Long> = _sessionStartTime.asStateFlow()

    // Thời gian bắt đầu xem nội dung hiện tại
    private var contentStartTime = 0L

    // Job để cập nhật thời gian
    private var updateTimeJob: Job? = null

    private val _backgroundTimeTotal = MutableStateFlow(0L)
    val backgroundTimeTotal: StateFlow<Long> = _backgroundTimeTotal.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _pauseReason = MutableStateFlow<PauseReason?>(null)
    val pauseReason: StateFlow<PauseReason?> = _pauseReason.asStateFlow()

    private var pauseStartTime = 0L
    private var currentPauseReason: PauseReason? = null

    /**
     * Lý do tạm dừng
     */
    enum class PauseReason {
        USER_ACTION,        // User tạm dừng
        APP_BACKGROUND,     // App vào background
        SCREEN_OFF,         // Màn hình tắt
        LOW_BATTERY         // Pin yếu
    }

    /**
     * Bắt đầu phiên học
     */
    fun startSession() {
        _sessionStartTime.value = System.currentTimeMillis()
        contentStartTime = System.currentTimeMillis()
        _backgroundTimeTotal.value = 0L
        _isPaused.value = false
        _pauseReason.value = null

        Log.d(TAG, "🕐 Session started at: ${_sessionStartTime.value}")

        // Bắt đầu cập nhật thời gian mỗi giây
        startTimeTracking()
    }

    /**
     * Bắt đầu xem nội dung mới
     */
    fun startContentView() {
        if (_isPaused.value) {
            Log.d(TAG, "⏸️ Cannot start content view - manager is paused")
            return
        }

        contentStartTime = System.currentTimeMillis()
        Log.d(TAG, "📖 Content view started at: $contentStartTime")
    }

    /**
     * Kết thúc xem nội dung (cộng dồn thời gian)
     */
    fun endContentView() {
        if (_isPaused.value) {
            Log.d(TAG, "⏸️ Content view already ended due to pause")
            return
        }

        val currentTime = System.currentTimeMillis()
        val elapsedMs = currentTime - contentStartTime
        val elapsedSeconds = elapsedMs / 1000

        if (elapsedSeconds > 0) {
            _totalTimeSpent.value += elapsedSeconds
            Log.d(TAG, "⏱️ Content view ended")
            Log.d(TAG, "   - Elapsed time: ${elapsedSeconds}s")
            Log.d(TAG, "   - Total time spent: ${_totalTimeSpent.value}s")
        }
    }

    /**
     * Tạm dừng theo dõi thời gian
     */
    fun pause(reason: PauseReason = PauseReason.USER_ACTION) {
        if (_isPaused.value) {
            Log.d(TAG, "⏸️ Already paused")
            return
        }

        // Lưu thời gian hiện tại trước khi pause
        endContentView()
        stopTimeTracking()

        pauseStartTime = System.currentTimeMillis()
        currentPauseReason = reason
        _isPaused.value = true
        _pauseReason.value = reason

        Log.d(TAG, "⏸️ Time tracking paused")
        Log.d(TAG, "   - Reason: $reason")
        Log.d(TAG, "   - Total time so far: ${_totalTimeSpent.value}s")
    }

    /**
     * Tạm dừng do app vào background
     */
    fun pauseForBackground() {
        pause(PauseReason.APP_BACKGROUND)
        Log.d(TAG, "📱 Time tracking paused for background")
    }

    /**
     * Tạm dừng do màn hình tắt
     */
    fun pauseForScreenOff() {
        pause(PauseReason.SCREEN_OFF)
        Log.d(TAG, "🔴 Time tracking paused for screen off")
    }

    /**
     * Tiếp tục theo dõi thời gian
     */
    fun resume() {
        if (!_isPaused.value) {
            Log.d(TAG, "▶️ Already running")
            return
        }

        val pausedDuration = System.currentTimeMillis() - pauseStartTime

        // Cộng dồn background time nếu pause do background/screen off
        if (currentPauseReason == PauseReason.APP_BACKGROUND ||
            currentPauseReason == PauseReason.SCREEN_OFF) {
            _backgroundTimeTotal.value += pausedDuration
            Log.d(TAG, "📱 Background time added: ${pausedDuration / 1000}s")
            Log.d(TAG, "   - Total background time: ${_backgroundTimeTotal.value / 1000}s")
        }

        pauseStartTime = 0L
        currentPauseReason = null
        _isPaused.value = false
        _pauseReason.value = null

        // Bắt đầu lại tracking
        contentStartTime = System.currentTimeMillis()
        startTimeTracking()

        Log.d(TAG, "▶️ Time tracking resumed")
    }

    /**
     * Tiếp tục từ background
     * @return Thời gian đã ở background (ms)
     */
    fun resumeFromBackground(): Long {
        val pausedDuration = if (pauseStartTime > 0 && currentPauseReason == PauseReason.APP_BACKGROUND) {
            System.currentTimeMillis() - pauseStartTime
        } else 0L

        Log.d(TAG, "📱 Resuming from background (paused for ${pausedDuration / 1000}s)")
        resume()
        return pausedDuration
    }

    /**
     * Tiếp tục từ màn hình tắt
     * @return Thời gian màn hình đã tắt (ms)
     */
    fun resumeFromScreenOff(): Long {
        val pausedDuration = if (pauseStartTime > 0 && currentPauseReason == PauseReason.SCREEN_OFF) {
            System.currentTimeMillis() - pauseStartTime
        } else 0L

        Log.d(TAG, "🟢 Resuming from screen off (paused for ${pausedDuration / 1000}s)")
        resume()
        return pausedDuration
    }

    /**
     * Cộng dồn thời gian khi chuyển nội dung
     */
    fun addTimeOnContentChange() {
        if (_isPaused.value) {
            Log.d(TAG, "⏸️ Content change ignored - manager is paused")
            return
        }

        endContentView()
        startContentView()
    }

    /**
     * Kết thúc phiên học (cộng dồn thời gian cuối cùng)
     */
    fun endSession() {
        if (!_isPaused.value) {
            endContentView()
        }
        stopTimeTracking()

        Log.d(TAG, "🏁 Session ended")
        Log.d(TAG, "   - Total active time: ${_totalTimeSpent.value}s")
        Log.d(TAG, "   - Total background time: ${_backgroundTimeTotal.value / 1000}s")
    }

    /**
     * Lấy thời gian học tập hiện tại (chỉ thời gian active, không bao gồm background)
     */
    fun getTotalTimeSpent(): Long {
        return _totalTimeSpent.value
    }

    /**
     * Lấy thời gian background tổng cộng
     */
    fun getTotalBackgroundTime(): Long {
        return _backgroundTimeTotal.value / 1000
    }

    /**
     * Lấy thời gian bắt đầu phiên
     */
    fun getSessionStartTime(): Long {
        return _sessionStartTime.value
    }

    /**
     * Lấy thời gian đã trôi qua từ khi bắt đầu phiên (bao gồm cả pause)
     */
    fun getElapsedTimeInSession(): Long {
        val currentTime = System.currentTimeMillis()
        return (currentTime - _sessionStartTime.value) / 1000
    }

    /**
     * Lấy thời gian học thực tế (loại trừ background time)
     */
    fun getActiveStudyTime(): Long {
        return _totalTimeSpent.value
    }

    /**
     * Reset thời gian (khi bắt đầu bài học mới)
     */
    fun reset() {
        stopTimeTracking()
        _totalTimeSpent.value = 0L
        _sessionStartTime.value = System.currentTimeMillis()
        contentStartTime = System.currentTimeMillis()
        _backgroundTimeTotal.value = 0L
        _isPaused.value = false
        _pauseReason.value = null
        pauseStartTime = 0L
        currentPauseReason = null

        Log.d(TAG, "🔄 Study time reset")
    }

    /**
     * Reset thời gian tích lũy sau khi save (để tính thời gian thêm lần tiếp theo)
     */
    fun resetAccumulatedTime() {
        _totalTimeSpent.value = 0L
        contentStartTime = System.currentTimeMillis()

        Log.d(TAG, "🔄 Accumulated time reset after save")
    }

    /**
     * Bắt đầu theo dõi thời gian
     */
    private fun startTimeTracking() {
        stopTimeTracking()

        updateTimeJob = coroutineScope.launch {
            while (true) {
                delay(1000) // Cập nhật mỗi giây

                if (!_isPaused.value) {
                    // Cộng dồn thời gian
                    _totalTimeSpent.value += 1
                }
            }
        }

        Log.d(TAG, "⏱️ Time tracking started")
    }

    /**
     * Dừng theo dõi thời gian
     */
    private fun stopTimeTracking() {
        updateTimeJob?.cancel()
        updateTimeJob = null

        Log.d(TAG, "⏱️ Time tracking stopped")
    }

    /**
     * Lấy thông tin thời gian học
     */
    fun getStudyTimeInfo(): String {
        val totalSeconds = _totalTimeSpent.value
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val backgroundSeconds = _backgroundTimeTotal.value / 1000

        return buildString {
            append("📊 Thời gian học:\n")
            if (hours > 0) append("   - Giờ: $hours\n")
            if (minutes > 0) append("   - Phút: $minutes\n")
            append("   - Giây: $seconds\n")
            append("   - Tổng active: ${totalSeconds}s\n")
            append("   - Tổng background: ${backgroundSeconds}s")
        }
    }

    /**
     * Kiểm tra trạng thái pause
     */
    fun isPaused(): Boolean = _isPaused.value
}
