package com.example.datn.presentation.student.lessons.managers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Quản lý vòng đời ứng dụng
 *
 * Trách nhiệm:
 * - Phát hiện app chạy nền
 * - Phát hiện màn hình tắt
 * - Phát hiện pin yếu và sắp tắt nguồn
 * - Tự động lưu tiến độ trước khi thoát
 * - Tự động thoát khi app ở nền hoặc màn hình tắt
 * - Ghi nhận thời gian ở nền
 * - Hỗ trợ khôi phục phiên học
 */
class AppLifecycleManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val _isAppInForeground = MutableStateFlow(true)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    private val _isScreenOn = MutableStateFlow(true)
    val isScreenOn: StateFlow<Boolean> = _isScreenOn.asStateFlow()

    private val _shouldForceExit = MutableStateFlow(false)
    val shouldForceExit: StateFlow<Boolean> = _shouldForceExit.asStateFlow()

    private val _backgroundTimeMs = MutableStateFlow(0L)
    val backgroundTimeMs: StateFlow<Long> = _backgroundTimeMs.asStateFlow()

    private val _lifecycleState = MutableStateFlow(LifecycleState.ACTIVE)
    val lifecycleState: StateFlow<LifecycleState> = _lifecycleState.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _exitReason = MutableStateFlow<ExitReason?>(null)
    val exitReason: StateFlow<ExitReason?> = _exitReason.asStateFlow()

    private val _shouldShowBackgroundWarning = MutableStateFlow(false)
    val shouldShowBackgroundWarning: StateFlow<Boolean> = _shouldShowBackgroundWarning.asStateFlow()

    private var backgroundStartTime = 0L
    private var screenOffStartTime = 0L
    private var screenOffReceiver: BroadcastReceiver? = null
    private var screenOnReceiver: BroadcastReceiver? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var powerConnectionReceiver: BroadcastReceiver? = null
    private var shutdownReceiver: BroadcastReceiver? = null

    private var backgroundMonitorJob: Job? = null
    private var screenOffMonitorJob: Job? = null

    private var onBackgroundEntered: (() -> Unit)? = null
    private var onBackgroundExited: (() -> Unit)? = null
    private var onScreenOff: (() -> Unit)? = null
    private var onScreenOn: (() -> Unit)? = null
    private var onEmergencySaveRequired: (() -> Unit)? = null
    private var onForceExitRequired: ((ExitReason) -> Unit)? = null

    private val TAG = "AppLifecycleManager"

    /**
     * Trạng thái vòng đời của app
     */
    enum class LifecycleState {
        ACTIVE,           // App đang hoạt động bình thường
        BACKGROUND,       // App ở nền
        BACKGROUND_WARNING, // App ở nền quá lâu - hiển thị cảnh báo
        SCREEN_OFF,       // Màn hình tắt
        LOW_BATTERY,      // Pin yếu
        SHUTTING_DOWN,    // Đang tắt nguồn
        FORCE_EXIT        // Buộc phải thoát
    }

    /**
     * Lý do buộc thoát
     */
    enum class ExitReason {
        BACKGROUND_TIMEOUT,    // Ở nền quá lâu
        SCREEN_OFF,           // Màn hình tắt
        LOW_BATTERY,          // Pin yếu
        DEVICE_SHUTDOWN,      // Thiết bị tắt nguồn
        USER_FORCE_STOP,      // User force stop app
        INACTIVITY            // Không hoạt động quá lâu
    }

    /**
     * Đăng ký callbacks
     */
    fun setCallbacks(
        onBackgroundEntered: (() -> Unit)? = null,
        onBackgroundExited: (() -> Unit)? = null,
        onScreenOff: (() -> Unit)? = null,
        onScreenOn: (() -> Unit)? = null,
        onEmergencySaveRequired: (() -> Unit)? = null,
        onForceExitRequired: ((ExitReason) -> Unit)? = null
    ) {
        this.onBackgroundEntered = onBackgroundEntered
        this.onBackgroundExited = onBackgroundExited
        this.onScreenOff = onScreenOff
        this.onScreenOn = onScreenOn
        this.onEmergencySaveRequired = onEmergencySaveRequired
        this.onForceExitRequired = onForceExitRequired

        Log.d(TAG, "✅ Callbacks registered")
    }

    /**
     * Bắt đầu theo dõi vòng đời ứng dụng
     */
    fun startMonitoring(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
        registerScreenStateReceivers()
        registerBatteryReceivers()
        registerShutdownReceiver()

        // Kiểm tra trạng thái pin ban đầu
        checkInitialBatteryState()

        Log.d(TAG, "🚀 Started monitoring lifecycle, screen, battery, and shutdown events")
    }

    /**
     * Dừng theo dõi vòng đời ứng dụng
     */
    fun stopMonitoring() {
        unregisterScreenStateReceivers()
        unregisterBatteryReceivers()
        unregisterShutdownReceiver()
        cancelMonitorJobs()

        Log.d(TAG, "⏹️ Stopped monitoring all events")
    }

    /**
     * Gọi khi app đi vào foreground
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        _isAppInForeground.value = true
        cancelMonitorJobs()

        // Tính thời gian ở nền
        if (backgroundStartTime > 0) {
            val timeInBackground = System.currentTimeMillis() - backgroundStartTime
            _backgroundTimeMs.value = timeInBackground

            Log.d(TAG, "⏱️ App resumed. Time in background: ${timeInBackground}ms")

            // Gọi callback
            onBackgroundExited?.invoke()

            // Kiểm tra xem có ở nền quá lâu không
            if (timeInBackground > LearningProgressConfig.APP_BACKGROUND_TIMEOUT_MS) {
                Log.w(TAG, "⚠️ App was in background too long (${timeInBackground}ms > ${LearningProgressConfig.APP_BACKGROUND_TIMEOUT_MS}ms)")
                triggerForceExit(ExitReason.BACKGROUND_TIMEOUT)
            } else {
                // Reset về trạng thái active
                _lifecycleState.value = LifecycleState.ACTIVE
                _shouldShowBackgroundWarning.value = false
            }
        }

        backgroundStartTime = 0L
    }

    /**
     * Gọi khi app đi vào background
     */
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        _isAppInForeground.value = false
        backgroundStartTime = System.currentTimeMillis()
        _lifecycleState.value = LifecycleState.BACKGROUND

        Log.d(TAG, "📱 App paused. Starting background monitoring...")

        // Gọi callback để lưu tiến độ
        if (LearningProgressConfig.SAVE_PROGRESS_ON_BACKGROUND) {
            onBackgroundEntered?.invoke()
        }

        // Bắt đầu theo dõi thời gian ở background
        startBackgroundMonitoring()
    }

    /**
     * Gọi khi app bị destroy
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.w(TAG, "💀 App destroyed - triggering emergency save")

        // Lưu khẩn cấp trước khi destroy
        onEmergencySaveRequired?.invoke()
    }

    /**
     * Kiểm tra xem app có ở nền không
     */
    fun isAppInBackground(): Boolean = !_isAppInForeground.value

    /**
     * Kiểm tra xem màn hình có tắt không
     */
    fun isScreenOff(): Boolean = !_isScreenOn.value

    /**
     * Lấy thời gian ở nền (ms)
     */
    fun getBackgroundTime(): Long = _backgroundTimeMs.value

    /**
     * Lấy trạng thái vòng đời hiện tại
     */
    fun getCurrentLifecycleState(): LifecycleState = _lifecycleState.value

    /**
     * Kiểm tra pin có yếu không
     */
    fun isLowBattery(): Boolean =
        _batteryLevel.value <= LearningProgressConfig.LOW_BATTERY_THRESHOLD && !_isCharging.value

    /**
     * Reset trạng thái
     */
    fun reset() {
        _isAppInForeground.value = true
        _isScreenOn.value = true
        _shouldForceExit.value = false
        _backgroundTimeMs.value = 0L
        _lifecycleState.value = LifecycleState.ACTIVE
        _exitReason.value = null
        _shouldShowBackgroundWarning.value = false
        backgroundStartTime = 0L
        screenOffStartTime = 0L
        cancelMonitorJobs()

        Log.d(TAG, "🔄 Reset complete")
    }

    /**
     * Xác nhận đã xử lý force exit
     */
    fun acknowledgeForceExit() {
        _shouldForceExit.value = false
        _exitReason.value = null
    }

    /**
     * Bắt đầu theo dõi background
     */
    private fun startBackgroundMonitoring() {
        backgroundMonitorJob?.cancel()
        backgroundMonitorJob = coroutineScope.launch {
            // Chờ grace period
            delay(LearningProgressConfig.BACKGROUND_GRACE_PERIOD_MS)

            // Kiểm tra vẫn ở background
            if (!_isAppInForeground.value) {
                Log.d(TAG, "📱 Still in background after grace period")

                // Chờ đến warning threshold
                delay(LearningProgressConfig.BACKGROUND_WARNING_THRESHOLD_MS - LearningProgressConfig.BACKGROUND_GRACE_PERIOD_MS)

                if (!_isAppInForeground.value) {
                    Log.w(TAG, "⚠️ Background warning threshold reached")
                    _lifecycleState.value = LifecycleState.BACKGROUND_WARNING
                    _shouldShowBackgroundWarning.value = true

                    // Chờ đến timeout
                    delay(LearningProgressConfig.APP_BACKGROUND_TIMEOUT_MS - LearningProgressConfig.BACKGROUND_WARNING_THRESHOLD_MS)

                    if (!_isAppInForeground.value) {
                        Log.e(TAG, "❌ Background timeout reached - force exit")
                        triggerForceExit(ExitReason.BACKGROUND_TIMEOUT)
                    }
                }
            }
        }
    }

    /**
     * Bắt đầu theo dõi screen off
     */
    private fun startScreenOffMonitoring() {
        screenOffMonitorJob?.cancel()
        screenOffMonitorJob = coroutineScope.launch {
            // Chờ grace period
            delay(LearningProgressConfig.SCREEN_OFF_GRACE_PERIOD_MS)

            // Kiểm tra màn hình vẫn tắt
            if (!_isScreenOn.value) {
                Log.w(TAG, "🔴 Screen still off after grace period")

                if (LearningProgressConfig.SCREEN_OFF_AUTO_EXIT) {
                    triggerForceExit(ExitReason.SCREEN_OFF)
                }
            }
        }
    }

    /**
     * Hủy các monitor jobs
     */
    private fun cancelMonitorJobs() {
        backgroundMonitorJob?.cancel()
        backgroundMonitorJob = null
        screenOffMonitorJob?.cancel()
        screenOffMonitorJob = null
    }

    /**
     * Trigger force exit
     */
    private fun triggerForceExit(reason: ExitReason) {
        _lifecycleState.value = LifecycleState.FORCE_EXIT
        _exitReason.value = reason
        _shouldForceExit.value = true

        Log.e(TAG, "🚨 Force exit triggered: $reason")

        // Gọi emergency save trước
        onEmergencySaveRequired?.invoke()

        // Thông báo force exit
        onForceExitRequired?.invoke(reason)
    }

    /**
     * Đăng ký nhận sự kiện màn hình tắt/bật
     */
    private fun registerScreenStateReceivers() {
        try {
            // Nhận sự kiện màn hình tắt
            screenOffReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    _isScreenOn.value = false
                    screenOffStartTime = System.currentTimeMillis()
                    _lifecycleState.value = LifecycleState.SCREEN_OFF

                    Log.w(TAG, "🔴 Screen turned OFF")

                    // Gọi callback để lưu tiến độ
                    if (LearningProgressConfig.SAVE_PROGRESS_ON_SCREEN_OFF) {
                        onScreenOff?.invoke()
                    }

                    // Bắt đầu theo dõi screen off
                    startScreenOffMonitoring()
                }
            }

            // Nhận sự kiện màn hình bật
            screenOnReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    _isScreenOn.value = true
                    screenOffMonitorJob?.cancel()

                    val screenOffDuration = if (screenOffStartTime > 0) {
                        System.currentTimeMillis() - screenOffStartTime
                    } else 0L

                    Log.d(TAG, "🟢 Screen turned ON (was off for ${screenOffDuration}ms)")

                    // Reset về trạng thái trước đó
                    if (_isAppInForeground.value) {
                        _lifecycleState.value = LifecycleState.ACTIVE
                    } else {
                        _lifecycleState.value = LifecycleState.BACKGROUND
                    }

                    screenOffStartTime = 0L
                    onScreenOn?.invoke()
                }
            }

            val screenOffFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            val screenOnFilter = IntentFilter(Intent.ACTION_SCREEN_ON)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(screenOffReceiver, screenOffFilter, Context.RECEIVER_EXPORTED)
                context.registerReceiver(screenOnReceiver, screenOnFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(screenOffReceiver, screenOffFilter)
                context.registerReceiver(screenOnReceiver, screenOnFilter)
            }

            Log.d(TAG, "✅ Screen state receivers registered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registering screen receivers: ${e.message}", e)
        }
    }

    /**
     * Hủy đăng ký nhận sự kiện màn hình
     */
    private fun unregisterScreenStateReceivers() {
        try {
            screenOffReceiver?.let { context.unregisterReceiver(it) }
            screenOnReceiver?.let { context.unregisterReceiver(it) }
            Log.d(TAG, "✅ Screen state receivers unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unregistering screen receivers: ${e.message}", e)
        }
    }

    /**
     * Đăng ký nhận sự kiện pin
     */
    private fun registerBatteryReceivers() {
        try {
            // Theo dõi thay đổi mức pin
            batteryReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

                    if (level >= 0 && scale > 0) {
                        val batteryPct = (level * 100) / scale
                        _batteryLevel.value = batteryPct

                        // Kiểm tra pin yếu
                        if (batteryPct <= LearningProgressConfig.LOW_BATTERY_THRESHOLD && !_isCharging.value) {
                            Log.w(TAG, "🔋 Low battery: $batteryPct%")
                            _lifecycleState.value = LifecycleState.LOW_BATTERY

                            if (LearningProgressConfig.AUTO_EXIT_ON_POWER_OFF) {
                                triggerForceExit(ExitReason.LOW_BATTERY)
                            }
                        }
                    }
                }
            }

            // Theo dõi kết nối sạc
            powerConnectionReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_POWER_CONNECTED -> {
                            _isCharging.value = true
                            Log.d(TAG, "🔌 Power connected")

                            // Reset trạng thái nếu đang low battery
                            if (_lifecycleState.value == LifecycleState.LOW_BATTERY) {
                                _lifecycleState.value = if (_isAppInForeground.value) {
                                    LifecycleState.ACTIVE
                                } else {
                                    LifecycleState.BACKGROUND
                                }
                            }
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            _isCharging.value = false
                            Log.d(TAG, "🔌 Power disconnected")
                        }
                    }
                }
            }

            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val powerFilter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_EXPORTED)
                context.registerReceiver(powerConnectionReceiver, powerFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, batteryFilter)
                context.registerReceiver(powerConnectionReceiver, powerFilter)
            }

            Log.d(TAG, "✅ Battery receivers registered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registering battery receivers: ${e.message}", e)
        }
    }

    /**
     * Hủy đăng ký nhận sự kiện pin
     */
    private fun unregisterBatteryReceivers() {
        try {
            batteryReceiver?.let { context.unregisterReceiver(it) }
            powerConnectionReceiver?.let { context.unregisterReceiver(it) }
            Log.d(TAG, "✅ Battery receivers unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unregistering battery receivers: ${e.message}", e)
        }
    }

    /**
     * Đăng ký nhận sự kiện tắt nguồn
     */
    private fun registerShutdownReceiver() {
        try {
            shutdownReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_SHUTDOWN -> {
                            Log.e(TAG, "⚡ Device shutting down!")
                            _lifecycleState.value = LifecycleState.SHUTTING_DOWN

                            // Lưu khẩn cấp
                            onEmergencySaveRequired?.invoke()
                            triggerForceExit(ExitReason.DEVICE_SHUTDOWN)
                        }
                        Intent.ACTION_REBOOT -> {
                            Log.e(TAG, "🔄 Device rebooting!")
                            _lifecycleState.value = LifecycleState.SHUTTING_DOWN

                            // Lưu khẩn cấp
                            onEmergencySaveRequired?.invoke()
                            triggerForceExit(ExitReason.DEVICE_SHUTDOWN)
                        }
                    }
                }
            }

            val shutdownFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SHUTDOWN)
                addAction(Intent.ACTION_REBOOT)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(shutdownReceiver, shutdownFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(shutdownReceiver, shutdownFilter)
            }

            Log.d(TAG, "✅ Shutdown receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registering shutdown receiver: ${e.message}", e)
        }
    }

    /**
     * Hủy đăng ký nhận sự kiện tắt nguồn
     */
    private fun unregisterShutdownReceiver() {
        try {
            shutdownReceiver?.let { context.unregisterReceiver(it) }
            Log.d(TAG, "✅ Shutdown receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unregistering shutdown receiver: ${e.message}", e)
        }
    }

    /**
     * Kiểm tra trạng thái pin ban đầu
     */
    private fun checkInitialBatteryState() {
        try {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            if (level >= 0 && scale > 0) {
                _batteryLevel.value = (level * 100) / scale
            }

            _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            Log.d(TAG, "🔋 Initial battery: ${_batteryLevel.value}%, charging: ${_isCharging.value}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking initial battery state: ${e.message}", e)
        }
    }
}
