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
 * Quản lý phiên học
 *
 * Trách nhiệm:
 * - Theo dõi thời gian phiên học
 * - Quản lý tạm dừng/tiếp tục
 * - Tính tổng thời gian học
 * - Xử lý khi app vào background
 * - Hỗ trợ khôi phục phiên học
 */
class SessionManager @Inject constructor(private val coroutineScope: CoroutineScope) {

    private val TAG = "SessionManager"

    private var sessionStartTime = 0L
    private var pauseStartTime = 0L
    private var totalPausedTime = 0L
    private var timerJob: Job? = null

    private val _sessionElapsedTime = MutableStateFlow(0L)
    val sessionElapsedTime: StateFlow<Long> = _sessionElapsedTime.asStateFlow()

    private val _isSessionPaused = MutableStateFlow(false)
    val isSessionPaused: StateFlow<Boolean> = _isSessionPaused.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _pauseReason = MutableStateFlow<PauseReason?>(null)
    val pauseReason: StateFlow<PauseReason?> = _pauseReason.asStateFlow()

    private val _backgroundPauseTime = MutableStateFlow(0L)
    val backgroundPauseTime: StateFlow<Long> = _backgroundPauseTime.asStateFlow()

    // Session data cho recovery
    private var sessionData: SessionData? = null

    /**
     * Trạng thái phiên học
     */
    enum class SessionState {
        IDLE,               // Chưa bắt đầu
        ACTIVE,             // Đang hoạt động
        PAUSED_BY_USER,     // Tạm dừng bởi user
        PAUSED_BY_BACKGROUND, // Tạm dừng do app vào background
        PAUSED_BY_SCREEN_OFF, // Tạm dừng do màn hình tắt
        PAUSED_BY_LOW_BATTERY, // Tạm dừng do pin yếu
        ENDED               // Đã kết thúc
    }

    /**
     * Lý do tạm dừng
     */
    enum class PauseReason {
        USER_ACTION,        // User tạm dừng
        APP_BACKGROUND,     // App vào background
        SCREEN_OFF,         // Màn hình tắt
        LOW_BATTERY,        // Pin yếu
        INACTIVITY         // Không hoạt động
    }

    /**
     * Dữ liệu phiên học cho recovery
     */
    data class SessionData(
        val sessionId: String,
        val startTime: Long,
        val elapsedTime: Long,
        val totalPausedTime: Long,
        val lastActiveTime: Long,
        val currentContentIndex: Int,
        val lessonId: String
    )

    /**
     * Bắt đầu phiên học
     */
    fun startSession(lessonId: String = "", contentIndex: Int = 0) {
        sessionStartTime = System.currentTimeMillis()
        pauseStartTime = 0L
        totalPausedTime = 0L
        _sessionElapsedTime.value = 0L
        _isSessionPaused.value = false
        _sessionState.value = SessionState.ACTIVE
        _pauseReason.value = null
        _backgroundPauseTime.value = 0L

        // Tạo session data
        sessionData = SessionData(
            sessionId = "session_${System.currentTimeMillis()}",
            startTime = sessionStartTime,
            elapsedTime = 0L,
            totalPausedTime = 0L,
            lastActiveTime = sessionStartTime,
            currentContentIndex = contentIndex,
            lessonId = lessonId
        )

        Log.d(TAG, "🎬 startSession() - Session started")
        Log.d(TAG, "   - Session ID: ${sessionData?.sessionId}")
        Log.d(TAG, "   - Lesson ID: $lessonId")
        Log.d(TAG, "   - Start time: ${java.text.SimpleDateFormat("HH:mm:ss").format(sessionStartTime)}")

        startTimer()
    }

    /**
     * Tạm dừng phiên học
     */
    fun pauseSession(reason: PauseReason = PauseReason.USER_ACTION) {
        if (!_isSessionPaused.value && _sessionState.value == SessionState.ACTIVE) {
            pauseStartTime = System.currentTimeMillis()
            _isSessionPaused.value = true
            _pauseReason.value = reason

            // Cập nhật session state dựa trên lý do
            _sessionState.value = when (reason) {
                PauseReason.USER_ACTION -> SessionState.PAUSED_BY_USER
                PauseReason.APP_BACKGROUND -> SessionState.PAUSED_BY_BACKGROUND
                PauseReason.SCREEN_OFF -> SessionState.PAUSED_BY_SCREEN_OFF
                PauseReason.LOW_BATTERY -> SessionState.PAUSED_BY_LOW_BATTERY
                PauseReason.INACTIVITY -> SessionState.PAUSED_BY_USER
            }

            timerJob?.cancel()

            Log.d(TAG, "⏸️ pauseSession() - Session paused")
            Log.d(TAG, "   - Reason: $reason")
            Log.d(TAG, "   - Elapsed time: ${_sessionElapsedTime.value}s")
        }
    }

    /**
     * Tạm dừng do app vào background
     */
    fun pauseForBackground() {
        pauseSession(PauseReason.APP_BACKGROUND)
        Log.d(TAG, "📱 Session paused for background")
    }

    /**
     * Tạm dừng do màn hình tắt
     */
    fun pauseForScreenOff() {
        pauseSession(PauseReason.SCREEN_OFF)
        Log.d(TAG, "🔴 Session paused for screen off")
    }

    /**
     * Tạm dừng do pin yếu
     */
    fun pauseForLowBattery() {
        pauseSession(PauseReason.LOW_BATTERY)
        Log.d(TAG, "🔋 Session paused for low battery")
    }

    /**
     * Tiếp tục phiên học
     */
    fun resumeSession(): Boolean {
        if (_isSessionPaused.value) {
            val pausedDuration = System.currentTimeMillis() - pauseStartTime
            totalPausedTime += pausedDuration

            // Cập nhật background pause time nếu pause do background
            if (_pauseReason.value == PauseReason.APP_BACKGROUND ||
                _pauseReason.value == PauseReason.SCREEN_OFF) {
                _backgroundPauseTime.value += pausedDuration
            }

            pauseStartTime = 0L
            _isSessionPaused.value = false
            _sessionState.value = SessionState.ACTIVE

            val previousReason = _pauseReason.value
            _pauseReason.value = null

            Log.d(TAG, "▶️ resumeSession() - Session resumed")
            Log.d(TAG, "   - Previous pause reason: $previousReason")
            Log.d(TAG, "   - Paused for: ${pausedDuration / 1000}s")
            Log.d(TAG, "   - Total paused time: ${totalPausedTime / 1000}s")

            startTimer()
            return true
        }
        return false
    }

    /**
     * Kết thúc phiên học
     */
    fun endSession(): Long {
        timerJob?.cancel()
        timerJob = null

        val totalTime = if (_isSessionPaused.value) {
            totalPausedTime += System.currentTimeMillis() - pauseStartTime
            (System.currentTimeMillis() - sessionStartTime - totalPausedTime) / 1000
        } else {
            (System.currentTimeMillis() - sessionStartTime - totalPausedTime) / 1000
        }

        _sessionState.value = SessionState.ENDED

        Log.d(TAG, "🏁 endSession() - Session ended")
        Log.d(TAG, "   - Total active time: ${totalTime}s (${totalTime / 60}m ${totalTime % 60}s)")
        Log.d(TAG, "   - Total paused time: ${totalPausedTime / 1000}s")
        Log.d(TAG, "   - Background pause time: ${_backgroundPauseTime.value / 1000}s")

        return totalTime
    }

    /**
     * Lấy thời gian phiên học hiện tại (giây)
     */
    fun getElapsedTime(): Long = _sessionElapsedTime.value

    /**
     * Kiểm tra xem phiên học có đang tạm dừng không
     */
    fun isSessionPaused(): Boolean = _isSessionPaused.value

    /**
     * Lấy trạng thái phiên học
     */
    fun getSessionState(): SessionState = _sessionState.value

    /**
     * Lấy lý do tạm dừng
     */
    fun getPauseReason(): PauseReason? = _pauseReason.value

    /**
     * Lấy session data cho recovery
     */
    fun getSessionDataForRecovery(): SessionData? {
        return sessionData?.copy(
            elapsedTime = _sessionElapsedTime.value,
            totalPausedTime = totalPausedTime,
            lastActiveTime = System.currentTimeMillis()
        )
    }

    /**
     * Khôi phục phiên học từ data
     */
    fun recoverSession(data: SessionData): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceLastActive = now - data.lastActiveTime

        // Kiểm tra timeout
        if (timeSinceLastActive > LearningProgressConfig.SESSION_RECOVERY_TIMEOUT_MS) {
            Log.w(TAG, "⚠️ Session recovery timeout - cannot recover")
            return false
        }

        sessionStartTime = data.startTime
        totalPausedTime = data.totalPausedTime + timeSinceLastActive
        _sessionElapsedTime.value = data.elapsedTime
        sessionData = data.copy(lastActiveTime = now)
        _sessionState.value = SessionState.ACTIVE
        _isSessionPaused.value = false

        Log.d(TAG, "✅ Session recovered")
        Log.d(TAG, "   - Original start: ${java.text.SimpleDateFormat("HH:mm:ss").format(data.startTime)}")
        Log.d(TAG, "   - Time since last active: ${timeSinceLastActive / 1000}s")
        Log.d(TAG, "   - Recovered elapsed time: ${data.elapsedTime}s")

        startTimer()
        return true
    }

    /**
     * Reset phiên học
     */
    fun reset() {
        timerJob?.cancel()
        timerJob = null
        sessionStartTime = 0L
        pauseStartTime = 0L
        totalPausedTime = 0L
        _sessionElapsedTime.value = 0L
        _isSessionPaused.value = false
        _sessionState.value = SessionState.IDLE
        _pauseReason.value = null
        _backgroundPauseTime.value = 0L
        sessionData = null

        Log.d(TAG, "🔄 reset() - Session manager reset")
    }

    /**
     * Cập nhật content index cho recovery
     */
    fun updateContentIndex(index: Int) {
        sessionData = sessionData?.copy(currentContentIndex = index)
    }

    private fun startTimer() {
        timerJob?.cancel()
        Log.d(TAG, "⏱️ startTimer() - Timer started")

        timerJob = coroutineScope.launch {
            while (true) {
                delay(1000)  // Cập nhật mỗi giây

                val currentTime = System.currentTimeMillis()
                val elapsedTime = (currentTime - sessionStartTime - totalPausedTime) / 1000
                _sessionElapsedTime.value = elapsedTime

                // Cập nhật session data
                sessionData = sessionData?.copy(
                    elapsedTime = elapsedTime,
                    lastActiveTime = currentTime
                )

                // Log every 60 seconds
                if (elapsedTime % 60 == 0L && elapsedTime > 0) {
                    Log.d(TAG, "⏰ Session elapsed: ${elapsedTime / 60}m ${elapsedTime % 60}s")
                }
            }
        }
    }
}
