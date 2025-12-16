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
 * Quản lý phát hiện không hoạt động
 *
 * Trách nhiệm:
 * - Theo dõi thời gian không hoạt động
 * - Phát hành cảnh báo sau 60 giây
 * - Tự động thoát sau 3 cảnh báo
 * - Reset timer khi có tương tác
 * - Reset warning count khi có tương tác thực sự (CLICK, SCROLL, ...)
 * - Tích hợp với AppLifecycleManager để phát hiện app background/screen off
 * - Tạm dừng khi app vào background
 */
class InactivityManager @Inject constructor(private val coroutineScope: CoroutineScope) {

    private val TAG = "InactivityManager"

    private var inactivityJob: Job? = null
    private var appLifecycleManager: AppLifecycleManager? = null

    private val _warningCount = MutableStateFlow(0)
    val warningCount: StateFlow<Int> = _warningCount.asStateFlow()

    private val _shouldExit = MutableStateFlow(false)
    val shouldExit: StateFlow<Boolean> = _shouldExit.asStateFlow()

    private val _isInactivityWarningVisible = MutableStateFlow(false)
    val isInactivityWarningVisible: StateFlow<Boolean> = _isInactivityWarningVisible.asStateFlow()

    private val _exitReason = MutableStateFlow("")
    val exitReason: StateFlow<String> = _exitReason.asStateFlow()

    private val _inactivityState = MutableStateFlow(InactivityState.ACTIVE)
    val inactivityState: StateFlow<InactivityState> = _inactivityState.asStateFlow()

    private val _lastInteractionTime = MutableStateFlow(System.currentTimeMillis())
    val lastInteractionTime: StateFlow<Long> = _lastInteractionTime.asStateFlow()

    private val _inactivityDuration = MutableStateFlow(0L)
    val inactivityDuration: StateFlow<Long> = _inactivityDuration.asStateFlow()

    private val _isPausedForBackground = MutableStateFlow(false)
    val isPausedForBackground: StateFlow<Boolean> = _isPausedForBackground.asStateFlow()

    private val _lastWarningResetTime = MutableStateFlow(0L)
    val lastWarningResetTime: StateFlow<Long> = _lastWarningResetTime.asStateFlow()

    private val _totalWarningsReset = MutableStateFlow(0)
    val totalWarningsReset: StateFlow<Int> = _totalWarningsReset.asStateFlow()

    // Danh sách các loại tương tác được phép reset warning
    val allowedResetInteractionTypes: Set<String> by lazy {
        LearningProgressConfig.RESET_WARNING_INTERACTION_TYPES
            .split(",")
            .map { it.trim().uppercase() }
            .toSet()
    }

    // Callbacks
    private var onWarningTriggered: ((Int) -> Unit)? = null
    private var onForceExit: ((String) -> Unit)? = null
    private var onWarningReset: ((Int, String) -> Unit)? = null  // (newCount, interactionType)

    /**
     * Trạng thái không hoạt động
     */
    enum class InactivityState {
        ACTIVE,             // Đang hoạt động bình thường
        MONITORING,         // Đang theo dõi (chưa đến ngưỡng)
        WARNING,            // Hiển thị cảnh báo
        PAUSED_BACKGROUND,  // Tạm dừng do app background
        FORCE_EXIT          // Buộc thoát
    }

    /**
     * Đăng ký callbacks
     */
    fun setCallbacks(
        onWarningTriggered: ((Int) -> Unit)? = null,
        onForceExit: ((String) -> Unit)? = null,
        onWarningReset: ((Int, String) -> Unit)? = null
    ) {
        this.onWarningTriggered = onWarningTriggered
        this.onForceExit = onForceExit
        this.onWarningReset = onWarningReset
    }

    /**
     * Bắt đầu theo dõi không hoạt động
     */
    fun startInactivityTracking() {
        Log.d(TAG, "🚀 startInactivityTracking() - Starting inactivity monitoring (${LearningProgressConfig.INACTIVITY_WARNING_TIMEOUT_SECONDS}s threshold)")
        _inactivityState.value = InactivityState.MONITORING
        _isInactivityWarningVisible.value = false
        _lastInteractionTime.value = System.currentTimeMillis()
        resetInactivityTimer()
    }

    /**
     * Ghi nhận tương tác (click, scroll, ...)
     * Reset timer không hoạt động
     * Reset warning count nếu là tương tác được phép
     *
     * @param interactionType Loại tương tác (CLICK, SCROLL, SWIPE, TAP, etc.)
     * @return true nếu warning count được reset
     */
    fun recordInteraction(interactionType: String = "UNKNOWN"): Boolean {
        // Không ghi nhận tương tác nếu đang paused
        if (_isPausedForBackground.value) {
            Log.d(TAG, "⏸️ Interaction ignored - manager is paused for background")
            return false
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastInteraction = currentTime - _lastInteractionTime.value

        // Kiểm tra khoảng cách tối thiểu giữa các tương tác (tránh spam)
        val isThrottled = timeSinceLastInteraction < LearningProgressConfig.MIN_INTERACTION_INTERVAL_MS
        if (isThrottled) {
            Log.d(TAG, "⏳ Interaction throttled (log only) - too fast (${timeSinceLastInteraction}ms < ${LearningProgressConfig.MIN_INTERACTION_INTERVAL_MS}ms)")
        }

        Log.d(TAG, "════════════════════════════════════════════════════════════════")
        Log.d(TAG, "👆 recordInteraction(type=$interactionType) - User interaction detected")
        Log.d(TAG, "   - Interaction Type: $interactionType")
        Log.d(TAG, "   - Current inactivity duration: ${_inactivityDuration.value}ms")
        Log.d(TAG, "   - Current warning count: ${_warningCount.value}")
        Log.d(TAG, "════════════════════════════════════════════════════════════════")

        _lastInteractionTime.value = currentTime
        _inactivityDuration.value = 0L
        _inactivityState.value = InactivityState.MONITORING
        _isInactivityWarningVisible.value = false

        if (_shouldExit.value) {
            Log.w(TAG, "🟢 Clearing shouldExit flag due to user interaction: $interactionType")
        }
        _shouldExit.value = false
        _exitReason.value = ""

        var warningWasReset = false
        if (shouldResetWarningOnInteraction(interactionType)) {
            warningWasReset = resetWarningCount(interactionType)
        }

        // Reset countdown timer từ 0 giây
        resetInactivityTimer()
        Log.d(TAG, "   ✅ Countdown timer reset to 0s")
        Log.d(TAG, "   ✅ Starting new ${LearningProgressConfig.INACTIVITY_WARNING_TIMEOUT_SECONDS}s countdown")
        Log.d(TAG, "════════════════════════════════════════════════════════════════")
        
        return warningWasReset
    }

    /**
     * Kiểm tra xem tương tác có nên reset warning count không
     */
    private fun shouldResetWarningOnInteraction(interactionType: String): Boolean {
        // Không reset nếu tính năng bị tắt
        if (!LearningProgressConfig.RESET_WARNING_ON_INTERACTION) {
            return false
        }

        // Không reset nếu chưa có warning nào
        if (_warningCount.value == 0) {
            return false
        }

        // Nếu cấu hình cho phép reset với BẤT KỲ tương tác nào
        if (LearningProgressConfig.RESET_WARNING_ON_ANY_INTERACTION) {
            Log.d(TAG, "✅ Reset warning on ANY interaction: $interactionType")
            return true
        }

        // Kiểm tra loại tương tác có trong danh sách được phép không
        val normalizedType = interactionType.trim().uppercase()
        return normalizedType in allowedResetInteractionTypes
    }

    /**
     * Reset warning count khi có tương tác thực sự
     *
     * @param interactionType Loại tương tác đã thực hiện
     * @return true nếu warning count đã được thay đổi
     */
    private fun resetWarningCount(interactionType: String): Boolean {
        val previousCount = _warningCount.value

        if (previousCount == 0) {
            return false
        }

        val newCount = if (LearningProgressConfig.PARTIAL_RESET_WARNING) {
            // Giảm 1 warning
            (previousCount - 1).coerceAtLeast(0)
        } else {
            // Reset hoàn toàn về 0
            0
        }

        _warningCount.value = newCount
        _lastWarningResetTime.value = System.currentTimeMillis()
        _totalWarningsReset.value = _totalWarningsReset.value + 1

        Log.d(TAG, "✅ resetWarningCount() - Warning count reset: $previousCount → $newCount (interaction: $interactionType)")
        Log.d(TAG, "   - Total warnings reset in session: ${_totalWarningsReset.value}")

        // Gọi callback
        onWarningReset?.invoke(newCount, interactionType)

        return true
    }

    /**
     * Reset warning count thủ công (public API)
     * Sử dụng khi cần reset từ bên ngoài (vd: sau khi user xác nhận tiếp tục học)
     */
    fun forceResetWarningCount(reason: String = "MANUAL") {
        val previousCount = _warningCount.value
        _warningCount.value = 0
        _lastWarningResetTime.value = System.currentTimeMillis()

        if (_isPausedForBackground.value) {
            Log.w(TAG, "🟢 forceResetWarningCount() clearing pausedForBackground=true (reason: $reason)")
        }
        _isPausedForBackground.value = false

        _shouldExit.value = false
        _exitReason.value = ""
        _isInactivityWarningVisible.value = false
        _inactivityState.value = InactivityState.MONITORING
        _lastInteractionTime.value = System.currentTimeMillis()
        _inactivityDuration.value = 0L

        Log.d(TAG, "🔄 forceResetWarningCount() - Warning count force reset: $previousCount → 0 (reason: $reason)")
        Log.d(TAG, "   - Cleared flags: shouldExit=false, warningVisible=false, state=MONITORING")

        // Reset timer để bắt đầu đếm 60 giây mới
        resetInactivityTimer()

        onWarningReset?.invoke(0, reason)
    }

    fun dismissWarning(reason: String = "UI") {
        if (_isInactivityWarningVisible.value) {
            Log.d(TAG, "✅ dismissWarning() - Hiding warning dialog (reason: $reason)")
        }
        _isInactivityWarningVisible.value = false

        if (_inactivityState.value == InactivityState.WARNING) {
            _inactivityState.value = InactivityState.MONITORING
        }
    }

    /**
     * Dừng theo dõi không hoạt động
     */
    fun stopInactivityTracking() {
        Log.d(TAG, "⏹️ stopInactivityTracking() - Stopping inactivity monitoring")
        inactivityJob?.cancel()
        inactivityJob = null
        _inactivityState.value = InactivityState.ACTIVE
    }

    /**
     * Tạm dừng theo dõi khi app vào background
     */
    fun pauseForBackground() {
        Log.d(TAG, "📱 pauseForBackground() - Pausing inactivity tracking")
        inactivityJob?.cancel()
        _isPausedForBackground.value = true
        _inactivityState.value = InactivityState.PAUSED_BACKGROUND
    }

    /**
     * Tiếp tục theo dõi khi app trở lại foreground
     */
    fun resumeFromBackground() {
        Log.d(TAG, "📱 resumeFromBackground() - Resuming inactivity tracking")
        _isPausedForBackground.value = false
        _lastInteractionTime.value = System.currentTimeMillis()
        _inactivityDuration.value = 0L
        _inactivityState.value = InactivityState.MONITORING
        resetInactivityTimer()
    }

    /**
     * Lấy số lần cảnh báo hiện tại
     */
    fun getWarningCount(): Int = _warningCount.value

    /**
     * Lấy thời gian không hoạt động hiện tại (ms)
     */
    fun getInactivityDuration(): Long = _inactivityDuration.value

    /**
     * Kiểm tra xem có nên thoát không
     */
    fun shouldExitSession(): Boolean = _shouldExit.value

    /**
     * Tích hợp với AppLifecycleManager
     */
    fun setAppLifecycleManager(manager: AppLifecycleManager) {
        this.appLifecycleManager = manager

        // Theo dõi sự kiện app background
        coroutineScope.launch {
            manager.isAppInForeground.collect { isInForeground ->
                if (!isInForeground) {
                    Log.w(TAG, "📱 App went to background - pausing inactivity tracking")
                    pauseForBackground()
                } else if (_isPausedForBackground.value) {
                    Log.d(TAG, "📱 App returned to foreground - resuming inactivity tracking")
                    resumeFromBackground()
                }
            }
        }

        // Theo dõi sự kiện màn hình tắt
        coroutineScope.launch {
            manager.isScreenOn.collect { isScreenOn ->
                if (!isScreenOn && LearningProgressConfig.SCREEN_OFF_AUTO_EXIT) {
                    Log.w(TAG, "🔴 Screen turned OFF. Force exit.")
                    triggerForceExit("Màn hình đã tắt")
                }
            }
        }

        // Theo dõi sự kiện force exit từ lifecycle manager
        coroutineScope.launch {
            manager.shouldForceExit.collect { shouldExit ->
                if (shouldExit) {
                    val reason = manager.exitReason.value?.let { exitReason ->
                        when (exitReason) {
                            AppLifecycleManager.ExitReason.BACKGROUND_TIMEOUT -> "App ở nền quá lâu"
                            AppLifecycleManager.ExitReason.SCREEN_OFF -> "Màn hình đã tắt"
                            AppLifecycleManager.ExitReason.LOW_BATTERY -> "Pin yếu"
                            AppLifecycleManager.ExitReason.DEVICE_SHUTDOWN -> "Thiết bị đang tắt"
                            AppLifecycleManager.ExitReason.USER_FORCE_STOP -> "Ứng dụng bị dừng"
                            AppLifecycleManager.ExitReason.INACTIVITY -> "Không hoạt động quá lâu"
                        }
                    } ?: "Lỗi không xác định"

                    Log.w(TAG, "⚠️ Force exit triggered by lifecycle manager: $reason")
                    triggerForceExit(reason)
                }
            }
        }
    }

    /**
     * Trigger force exit
     */
    private fun triggerForceExit(reason: String) {
        _exitReason.value = reason
        _shouldExit.value = true
        _inactivityState.value = InactivityState.FORCE_EXIT
        onForceExit?.invoke(reason)
    }

    /**
     * Lấy lý do thoát
     */
    fun getExitReason(): String = _exitReason.value

    /**
     * Lấy thời gian không hoạt động (giây)
     */
    fun getInactivityDurationSeconds(): Long = _inactivityDuration.value / 1000

    /**
     * Lấy tổng số lần warning đã được reset trong session
     */
    fun getTotalWarningsReset(): Int = _totalWarningsReset.value

    /**
     * Kiểm tra loại tương tác có được phép reset warning không
     */
    fun isInteractionTypeAllowedForReset(interactionType: String): Boolean {
        return interactionType.trim().uppercase() in allowedResetInteractionTypes
    }

    /**
     * Reset lại trạng thái (khi chuyển sang bài học khác)
     */
    fun reset() {
        Log.d(TAG, "🔄 reset() - Resetting inactivity manager state")
        stopInactivityTracking()
        _warningCount.value = 0
        _shouldExit.value = false
        _isInactivityWarningVisible.value = false
        _exitReason.value = ""
        _inactivityState.value = InactivityState.ACTIVE
        _lastInteractionTime.value = System.currentTimeMillis()
        _inactivityDuration.value = 0L
        _isPausedForBackground.value = false
        _lastWarningResetTime.value = 0L
        _totalWarningsReset.value = 0
        Log.d(TAG, "   ✅ Reset complete")
    }

    private fun resetInactivityTimer() {
        // Cancel job cũ nếu có
        inactivityJob?.cancel()
        Log.d(TAG, "⏱️ resetInactivityTimer() - Cancelling old timer job")

        // Không bắt đầu timer nếu đang paused
        if (_isPausedForBackground.value) {
            Log.d(TAG, "   ⏸️ Timer not started - app is paused for background")
            return
        }

        Log.d(TAG, "⏱️ resetInactivityTimer() - Starting new ${LearningProgressConfig.INACTIVITY_WARNING_TIMEOUT_SECONDS}s countdown")
        
        // Reset inactivity duration về 0
        _inactivityDuration.value = 0L
        Log.d(TAG, "   ✅ Inactivity duration reset to 0ms")

        inactivityJob = coroutineScope.launch {
            // Cập nhật inactivity duration mỗi giây
            var elapsed = 0L
            while (elapsed < LearningProgressConfig.INACTIVITY_WARNING_TIMEOUT_SECONDS) {
                delay(1000)
                elapsed++
                _inactivityDuration.value = elapsed * 1000

                // Log mỗi 10 giây để theo dõi countdown
                if (elapsed % 10 == 0L || elapsed == 1L) {
                    Log.d(TAG, "⏰ Countdown: ${elapsed}s / ${LearningProgressConfig.INACTIVITY_WARNING_TIMEOUT_SECONDS}s")
                }
            }

            // Kiểm tra app có ở nền không
            if (appLifecycleManager?.isAppInForeground?.value == false) {
                Log.w(TAG, "📱 Inactivity detected while app in background - Force exit")
                triggerForceExit("Không hoạt động khi app ở nền")
                return@launch
            }

            // Kiểm tra màn hình có tắt không
            if (appLifecycleManager?.isScreenOn?.value == false) {
                Log.w(TAG, "🔴 Inactivity detected while screen is off - Force exit")
                triggerForceExit("Không hoạt động khi màn hình tắt")
                return@launch
            }

            // Tăng số lần cảnh báo
            val newWarningCount = _warningCount.value + 1
            _warningCount.value = newWarningCount
            _isInactivityWarningVisible.value = true
            _inactivityState.value = InactivityState.WARNING

            Log.w(TAG, "⚠️ INACTIVITY WARNING #$newWarningCount/${LearningProgressConfig.MAX_INACTIVITY_WARNINGS}")
            Log.w(TAG, "   - User has not interacted for ${LearningProgressConfig.INACTIVITY_WARNING_TIMEOUT_SECONDS}s")
            Log.w(TAG, "   - Showing warning dialog to user")
            Log.w(TAG, "   - Hint: User can reset warnings by interacting (${allowedResetInteractionTypes.joinToString()})")

            // Gọi callback
            onWarningTriggered?.invoke(newWarningCount)

            // Kiểm tra xem có nên thoát không
            if (newWarningCount >= LearningProgressConfig.MAX_INACTIVITY_WARNINGS) {
                Log.e(TAG, "❌ MAX WARNINGS REACHED - Auto-exiting lesson")
                triggerForceExit("Vượt quá số lần cảnh báo không hoạt động")
            } else {
                // Tiếp tục theo dõi nếu chưa đạt giới hạn
                Log.d(TAG, "   - Continuing to monitor inactivity...")
                _inactivityState.value = InactivityState.MONITORING
                resetInactivityTimer()
            }
        }
    }
}
