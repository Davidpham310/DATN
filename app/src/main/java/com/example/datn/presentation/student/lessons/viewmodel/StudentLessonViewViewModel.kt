package com.example.datn.presentation.student.lessons.viewmodel

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.IProgressRepository
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.lesson.LessonUseCases
import com.example.datn.domain.usecase.progress.UpdateLessonProgressParams
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.student.lessons.managers.*
import com.example.datn.presentation.student.lessons.state.StudentLessonViewState
import com.example.datn.presentation.student.lessons.event.StudentLessonViewEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentLessonViewViewModel @Inject constructor(
    private val lessonUseCases: LessonUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    private val progressRepository: IProgressRepository,
    notificationManager: NotificationManager,
    private val inactivityManager: InactivityManager,
    private val autoSaveManager: ProgressAutoSaveManager,
    private val sessionManager: SessionManager,
    private val studyTimeManager: StudyTimeManager,
    private val mediaProgressManager: MediaProgressManager,
    private val contentCompletionManager: ContentCompletionManager,
    private val completionRulesManager: CompletionRulesManager,
    private val navigationControlManager: NavigationControlManager,
    private val appLifecycleManager: AppLifecycleManager
) : BaseViewModel<StudentLessonViewState, StudentLessonViewEvent>(
    StudentLessonViewState(),
    notificationManager
) {

    companion object {
        private const val TAG = "StudentLessonViewVM"
        private const val AUTO_EXIT_DELAY = 5000L
    }

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private var autoExitJob: Job? = null

    private var contentViewTimeJob: Job? = null

    private var pauseMediaPlayerCallback: (() -> Unit)? = null
    private var resumeMediaPlayerCallback: (() -> Unit)? = null

    init {
        // Lắng nghe trạng thái hoàn thành từ ContentCompletionManager
        viewModelScope.launch {
            contentCompletionManager.completionStatus.collect { statusMap ->
                Log.d(TAG, "📊 Completion status updated: ${statusMap.size} items")
                statusMap.forEach { (contentId, status) ->
                    Log.d(TAG, "   - $contentId: completed=${status.isCompleted}, progress=${status.progress}, fastForward=${status.isFastForwarded}")
                }

                // Cập nhật state với trạng thái hoàn thành mới
                setState {
                    // Tính điểm nghiêm túc dựa trên các tiêu chí
                    val fastForwardCount = statusMap.count { it.value.isFastForwarded }
                    val newSeriousnessScore = calculateSeriousnessScore(statusMap)

                    copy(
                        contentCompletionStatus = statusMap,
                        totalFastForwardCount = fastForwardCount,
                        isFastForwardDetected = fastForwardCount > 0,
                        studySeriousnessScore = newSeriousnessScore
                    )
                }
            }
        }

        // Lắng nghe cảnh báo không hoạt động từ InactivityManager
        viewModelScope.launch {
            inactivityManager.isInactivityWarningVisible.collect { isVisible ->
                if (isVisible) {
                    Log.d(TAG, "⚠️ Inactivity warning triggered from manager")
                    showInactivityWarning()
                }
            }
        }

        // Lắng nghe yêu cầu thoát từ InactivityManager
        viewModelScope.launch {
            inactivityManager.shouldExit.collect { shouldExit ->
                if (shouldExit) {
                    val reason = inactivityManager.getExitReason()
                    Log.d(TAG, "❌ Force exit triggered: $reason")
                    exitLessonWithoutSaving()
                }
            }
        }

        viewModelScope.launch {
            appLifecycleManager.lifecycleState.collect { lifecycleState ->
                Log.d(TAG, "📱 Lifecycle state changed: $lifecycleState")
                handleLifecycleStateChange(lifecycleState)
            }
        }

        viewModelScope.launch {
            appLifecycleManager.shouldForceExit.collect { shouldExit ->
                if (shouldExit) {
                    val reason = appLifecycleManager.exitReason.value
                    Log.e(TAG, "🚨 Force exit from AppLifecycleManager: $reason")
                    handleForceExit(reason)
                }
            }
        }

        // Theo dõi trạng thái hiển thị Dialog cảnh báo không hoạt động
        viewModelScope.launch {
            inactivityManager.isInactivityWarningVisible.collect { isVisible ->
                val currentShowWarning = state.value.showInactivityWarning
                
                // Chỉ cập nhật state nếu có thay đổi thực sự
                if (currentShowWarning != isVisible) {
                    Log.d(TAG, "⚠️ Inactivity warning visibility changed: $currentShowWarning → $isVisible")
                    setState { copy(showInactivityWarning = isVisible) }
                }
            }
        }

        // Theo dõi yêu cầu tự động thoát (sau 3 lần cảnh báo)
        viewModelScope.launch {
            inactivityManager.shouldExit.collect { shouldExit ->
                if (shouldExit) {
                    Log.e(TAG, "❌ Auto-exit requested due to max inactivity warnings")
                    exitLessonWithoutSaving()
                }
            }
        }
    }

    private fun handleLifecycleStateChange(lifecycleState: AppLifecycleManager.LifecycleState) {
        when (lifecycleState) {
            AppLifecycleManager.LifecycleState.BACKGROUND -> {
                Log.d(TAG, "📱 App entered background - pausing session and media")
                // Session và media sẽ được pause qua LifecycleListener callbacks
            }
            AppLifecycleManager.LifecycleState.SCREEN_OFF -> {
                Log.d(TAG, "🔴 Screen off - pausing session and media")
                // Session và media sẽ được pause qua LifecycleListener callbacks
            }
            AppLifecycleManager.LifecycleState.LOW_BATTERY -> {
                Log.w(TAG, "🔋 Low battery detected")
                showNotification("Pin yếu! Tiến độ đang được lưu tự động.", NotificationType.ERROR)
            }
            AppLifecycleManager.LifecycleState.ACTIVE -> {
                Log.d(TAG, "✅ App is active")
            }
            else -> {}
        }
    }

    private fun handleForceExit(reason: AppLifecycleManager.ExitReason?) {
        viewModelScope.launch {
            Log.e(TAG, "🚨 Handling force exit: $reason")

            // Lưu tiến độ khẩn cấp
            saveProgress()

            // Kết thúc session
            sessionManager.endSession()
            studyTimeManager.endSession()

            // Đánh dấu cần thoát
            setState { copy(shouldAutoExitLesson = true) }

            // Acknowledge force exit
            appLifecycleManager.acknowledgeForceExit()
        }
    }

    private fun onAppEnteredBackground() {
        Log.d(TAG, "📱 App entered background")

        // Pause session
        sessionManager.pauseForBackground()

        // Pause study time
        studyTimeManager.pauseForBackground()

        // Pause media
        mediaProgressManager.pauseForBackground()
        pauseMediaPlayerCallback?.invoke()

        // Pause content view time tracking
        contentViewTimeJob?.cancel()

        // Lưu tiến độ lần cuối trước khi "ngủ" (Checkpoint)
        // Nếu bạn muốn tắt hẳn việc lưu khi vào background (không khuyến khích vì có thể mất dữ liệu), hãy comment dòng này lại.
        viewModelScope.launch {
            saveProgress()
        }

        // Cập nhật state
        setState { copy(isAppInBackground = true) }
    }

    private fun onAppEnteredForeground(backgroundDurationMs: Long) {
        Log.d(TAG, "📱 App entered foreground (was in background for ${backgroundDurationMs}ms)")

        // Resume session
        sessionManager.resumeSession()

        // Resume study time
        studyTimeManager.resumeFromBackground()

        // Resume media tracking (không auto play)
        mediaProgressManager.resumeFromBackground()

        // Resume content view time tracking cho nội dung hiện tại
        val currentContent = state.value.currentContent
        if (currentContent != null) {
            startContentViewTimeTracking(currentContent.id, currentContent.contentType.name)
        }

        // [MỚI] Khởi động lại Auto Save khi người dùng quay lại
        autoSaveManager.startAutoSave()

        // Cập nhật state
        setState { copy(isAppInBackground = false) }

        // Kiểm tra nếu ở background quá lâu
        if (backgroundDurationMs > LearningProgressConfig.APP_BACKGROUND_TIMEOUT_MS) {
            Log.w(TAG, "⚠️ Was in background too long - showing warning")
            showNotification(
                "Bạn đã rời khỏi bài học quá lâu (${backgroundDurationMs / 1000}s)",
                NotificationType.ERROR
            )
        }
    }
    private fun onScreenOff() {
        Log.d(TAG, "🔴 Screen off")

        // [MỚI] Dừng ngay việc tự động lưu định kỳ
        autoSaveManager.stopAutoSave()

        // Pause session
        sessionManager.pauseForScreenOff()

        // Pause study time
        studyTimeManager.pauseForScreenOff()

        // Pause media
        mediaProgressManager.pauseForScreenOff()
        pauseMediaPlayerCallback?.invoke()

        // Pause content view time tracking
        contentViewTimeJob?.cancel()

        // Lưu tiến độ lần cuối (Checkpoint)
        viewModelScope.launch {
            saveProgress()
        }
    }

    private fun onScreenOn(offDurationMs: Long) {
        Log.d(TAG, "🟢 Screen on (was off for ${offDurationMs}ms)")

        // Resume session nếu app vẫn ở foreground
        if (appLifecycleManager.isAppInForeground.value) {
            sessionManager.resumeSession()
            studyTimeManager.resumeFromScreenOff()
            mediaProgressManager.resumeFromScreenOff()

            // [MỚI] Khởi động lại Auto Save
            autoSaveManager.startAutoSave()

            // Resume content view time tracking
            val currentContent = state.value.currentContent
            if (currentContent != null) {
                startContentViewTimeTracking(currentContent.id, currentContent.contentType.name)
            }
        }
    }

    private fun onLowBattery(batteryLevel: Int) {
        Log.w(TAG, "🔋 Low battery: $batteryLevel%")

        // Lưu tiến độ ngay lập tức
        viewModelScope.launch {
            saveProgress()
        }

        showNotification(
            "Pin yếu ($batteryLevel%)! Tiến độ đã được lưu tự động.",
            NotificationType.ERROR
        )
    }

    private fun onDeviceShuttingDown() {
        Log.e(TAG, "⚡ Device shutting down")

        // [MỚI] Dừng Auto Save để tránh xung đột khi tắt máy
        autoSaveManager.stopAutoSave()

        // Lưu tiến độ khẩn cấp một lần duy nhất
        viewModelScope.launch {
            saveProgress()
            sessionManager.endSession()
            studyTimeManager.endSession()
        }
    }

    fun setMediaPlayerCallbacks(
        onPause: (() -> Unit)? = null,
        onResume: (() -> Unit)? = null
    ) {
        pauseMediaPlayerCallback = onPause
        resumeMediaPlayerCallback = onResume
        Log.d(TAG, "✅ Media player callbacks registered")
    }

    fun startLifecycleMonitoring(lifecycle: Lifecycle) {
        // Thiết lập callbacks
        appLifecycleManager.setCallbacks(
            onBackgroundEntered = {
                Log.d(TAG, "📱 [Callback] Background entered")
                onAppEnteredBackground()
            },
            onBackgroundExited = {
                Log.d(TAG, "📱 [Callback] Background exited")
                onAppEnteredForeground(appLifecycleManager.getBackgroundTime())
            },
            onScreenOff = {
                Log.d(TAG, "🔴 [Callback] Screen off")
                onScreenOff()
            },
            onScreenOn = {
                Log.d(TAG, "🟢 [Callback] Screen on")
                onScreenOn(0L)  // Screen on duration not tracked separately
            },
            onEmergencySaveRequired = {
                Log.e(TAG, "🚨 [Callback] Emergency save required")
                viewModelScope.launch {
                    saveProgress()
                }
            },
            onForceExitRequired = { reason ->
                Log.e(TAG, "🚨 [Callback] Force exit required: $reason")
                handleForceExit(reason)
            }
        )

        // Bắt đầu monitoring
        appLifecycleManager.startMonitoring(lifecycle)

        Log.d(TAG, "🚀 Lifecycle monitoring started")
    }

    fun stopLifecycleMonitoring() {
        appLifecycleManager.stopMonitoring()
        Log.d(TAG, "⏹️ Lifecycle monitoring stopped")
    }

    private fun calculateSeriousnessScore(statusMap: Map<String, ContentCompletionStatus>): Int {
        if (statusMap.isEmpty()) return 100

        var score = 100

        // Trừ điểm nếu tua nhanh video/audio
        val fastForwardCount = statusMap.count { it.value.isFastForwarded }
        score -= fastForwardCount * 20  // Mỗi lần tua nhanh trừ 20 điểm

        // Trừ điểm nếu có cảnh báo không hoạt động
        score -= state.value.inactivityWarningCount * 10  // Mỗi cảnh báo trừ 10 điểm

        // Cộng điểm nếu hoàn thành nội dung đúng quy tắc
        val properlyCompleted = statusMap.count { it.value.isCompleted && !it.value.isFastForwarded }
        val totalContents = state.value.lessonContents.size
        if (totalContents > 0) {
            score += (properlyCompleted * 10) / totalContents
        }

        return score.coerceIn(0, 100)
    }

    override fun onEvent(event: StudentLessonViewEvent) {
        when (event) {
            is StudentLessonViewEvent.LoadLesson -> loadLesson(event.lessonId, event.initialContentId)
            StudentLessonViewEvent.NextContent -> navigateToNextContent()
            StudentLessonViewEvent.PreviousContent -> navigateToPreviousContent()
            is StudentLessonViewEvent.GoToContent -> navigateToContent(event.index)
            StudentLessonViewEvent.MarkCurrentAsViewed -> markCurrentContentAsViewed()
            StudentLessonViewEvent.ShowProgressDialog -> setState { copy(showProgressDialog = true) }
            StudentLessonViewEvent.DismissProgressDialog -> setState { copy(showProgressDialog = false) }
            StudentLessonViewEvent.SaveProgress -> saveProgress()
            is StudentLessonViewEvent.RecordInteraction -> recordInteraction(event.interactionType)
            StudentLessonViewEvent.ShowInactivityWarning -> showInactivityWarning()
            StudentLessonViewEvent.DismissInactivityWarning -> dismissInactivityWarning()
            StudentLessonViewEvent.ContinueLesson -> continueLesson()
            StudentLessonViewEvent.ExitLessonWithoutSaving -> exitLessonWithoutSaving()
            is StudentLessonViewEvent.OnMediaStateChanged -> onMediaStateChanged(event.isPlaying, event.contentType)
            is StudentLessonViewEvent.OnMediaProgress -> onMediaProgress(event.duration, event.position)
            is StudentLessonViewEvent.OnMediaSeek -> onMediaSeek(event.fromPosition, event.toPosition)
            StudentLessonViewEvent.ValidateVideoProgress -> validateVideoProgress()

            is StudentLessonViewEvent.StartContentView -> startContentView(event.contentId, event.contentType)
            is StudentLessonViewEvent.UpdateContentViewTime -> updateContentViewTime(event.contentId, event.elapsedSeconds, event.contentType)
            is StudentLessonViewEvent.UpdatePdfScrollProgress -> updatePdfScrollProgress(event.contentId, event.scrollPercentage)
            is StudentLessonViewEvent.UpdateVideoPosition -> updateVideoPosition(event.contentId, event.currentPositionMs, event.durationMs)
            is StudentLessonViewEvent.UpdateAudioPosition -> updateAudioPosition(event.contentId, event.currentPositionMs, event.durationMs)
            is StudentLessonViewEvent.GetCompletionRule -> { /* Handled via public function */ }
            is StudentLessonViewEvent.CheckContentCompletion -> { /* Handled via public function */ }

            else -> {}
        }
    }

    private fun startContentView(contentId: String, contentType: String) {
        Log.d(TAG, "📖 startContentView() contentId=$contentId, type=$contentType")

        // Ghi nhận thời gian bắt đầu xem
        val startTime = System.currentTimeMillis()
        setState {
            copy(
                currentContentViewStartTime = startTime,
                currentContentElapsedSeconds = 0
            )
        }

        // Bắt đầu theo dõi thời gian xem
        startContentViewTimeTracking(contentId, contentType)

        // Ghi nhận tương tác
        recordInteraction("START_VIEW")

        Log.d(TAG, "   ✅ Content view started at: $startTime")
    }

    private fun startContentViewTimeTracking(contentId: String, contentType: String) {
        // Hủy job cũ nếu có
        contentViewTimeJob?.cancel()

        if (appLifecycleManager.isAppInBackground()) {
            Log.d(TAG, "⏸️ Skipping content view tracking - app in background")
            return
        }

        contentViewTimeJob = viewModelScope.launch {
            var elapsedSeconds = 0L

            while (true) {
                delay(1000)  // Cập nhật mỗi giây

                if (appLifecycleManager.isAppInBackground() || !appLifecycleManager.isScreenOn.value) {
                    Log.d(TAG, "⏸️ Content view tracking paused - app in background or screen off")
                    continue
                }

                elapsedSeconds++

                // Cập nhật state
                setState { copy(currentContentElapsedSeconds = elapsedSeconds) }

                // Gọi CompletionRulesManager để kiểm tra hoàn thành
                when (contentType.uppercase()) {
                    "TEXT" -> {
                        completionRulesManager.handleTextViewed(contentId, elapsedSeconds)
                    }
                    "IMAGE" -> {
                        completionRulesManager.handleImageViewed(contentId, elapsedSeconds)
                    }
                }

                // Log mỗi 5 giây
                if (elapsedSeconds % 5 == 0L) {
                    Log.d(TAG, "⏱️ Content $contentId ($contentType) viewed for ${elapsedSeconds}s")

                    // Kiểm tra hoàn thành
                    val isCompleted = completionRulesManager.isContentCompleted(contentId)
                    if (isCompleted) {
                        Log.d(TAG, "✅ Content $contentId completed after ${elapsedSeconds}s")
                    }
                }
            }
        }
    }

    private fun updateContentViewTime(contentId: String, elapsedSeconds: Long, contentType: String) {
        Log.d(TAG, "📝 updateContentViewTime() contentId=$contentId, elapsed=${elapsedSeconds}s, type=$contentType")

        // Gọi CompletionRulesManager
        when (contentType.uppercase()) {
            "TEXT" -> completionRulesManager.handleTextViewed(contentId, elapsedSeconds)
            "IMAGE" -> completionRulesManager.handleImageViewed(contentId, elapsedSeconds)
        }

        // Cập nhật state
        setState { copy(currentContentElapsedSeconds = elapsedSeconds) }
    }

    private fun updatePdfScrollProgress(contentId: String, scrollPercentage: Int) {
        Log.d(TAG, "📕 updatePdfScrollProgress() contentId=$contentId, scroll=$scrollPercentage%")

        // Gọi CompletionRulesManager
        completionRulesManager.handlePdfScrolled(contentId, scrollPercentage)

        // Ghi nhận tương tác
        recordInteraction("PDF_SCROLL")
    }

    private fun updateVideoPosition(contentId: String, currentPositionMs: Long, durationMs: Long) {
//        Log.d(TAG, "▶️ updateVideoPosition() contentId=$contentId, pos=$currentPositionMs, dur=$durationMs")

        // Gọi CompletionRulesManager (sẽ phát hiện tua nhanh)
        completionRulesManager.handleVideoPositionChanged(contentId, currentPositionMs, durationMs)

        // Cập nhật state
        setState {
            copy(
                mediaDuration = durationMs,
                mediaPosition = currentPositionMs
            )
        }
    }

    private fun updateAudioPosition(contentId: String, currentPositionMs: Long, durationMs: Long) {
        Log.d(TAG, "🔊 updateAudioPosition() contentId=$contentId, pos=$currentPositionMs, dur=$durationMs")

        // Gọi CompletionRulesManager (sẽ phát hiện tua nhanh)
        completionRulesManager.handleAudioPositionChanged(contentId, currentPositionMs, durationMs)

        // Cập nhật state
        setState {
            copy(
                mediaDuration = durationMs,
                mediaPosition = currentPositionMs
            )
        }
    }

    fun getCompletionRuleForContent(contentType: String): String {
        return completionRulesManager.getCompletionRule(contentType)
    }

    fun checkIsContentCompleted(contentId: String): Boolean {
        return completionRulesManager.isContentCompleted(contentId)
    }

    fun getContentProgressValue(contentId: String): Int {
        return completionRulesManager.getContentProgress(contentId)
    }

    fun getStudySeriousnessInfo(): String {
        val state = state.value
        return buildString {
            append("📊 ĐÁNH GIÁ MỨC ĐỘ HỌC TẬP\n")
            append("================================\n")
            append("🎯 Điểm nghiêm túc: ${state.studySeriousnessScore}/100\n")
            append("📈 Mức độ: ${state.studySeriousnessLevel}\n")
            append("✅ Tỷ lệ hoàn thành đúng: ${state.properCompletionRate}%\n")
            append("⏱️ Thời gian học thực tế: ${state.totalActualStudyTimeSeconds}s\n")
            append("⚠️ Số lần tua nhanh: ${state.totalFastForwardCount}\n")
            append("🔔 Số lần cảnh báo: ${state.inactivityWarningCount}\n")

            if (state.hasDetectedCheating) {
                append("\n⚠️ CẢNH BÁO: Phát hiện hành vi gian lận (tua nhanh video/audio)\n")
            }
        }
    }

    private fun loadLesson(lessonId: String, initialContentId: String?) {
        viewModelScope.launch {
            Log.d(TAG, "🎓 loadLesson() called with lessonId=$lessonId")

            completionRulesManager.setOnContentCompletedCallback { contentId ->
                Log.d(TAG, "✅ Content completed callback: $contentId")
                // Tự động đánh dấu nội dung là đã xem khi hoàn thành theo quy tắc
                val currentContent = state.value.currentContent
                if (currentContent?.id == contentId) {
                    markCurrentContentAsViewed()
                }
            }

            contentCompletionManager.setOnContentCompletedCallback { contentId ->
                Log.d(TAG, "📌 ContentCompletionManager callback: $contentId completed")
            }

            Log.d(TAG, "🚀 Starting all managers...")

            // Khởi động SessionManager
            sessionManager.startSession()
            Log.d(TAG, "   ✅ SessionManager started")

            // Khởi động StudyTimeManager
            studyTimeManager.startSession()
            Log.d(TAG, "   ✅ StudyTimeManager started")

            // Khởi động InactivityManager
            inactivityManager.startInactivityTracking()
            Log.d(TAG, "   ✅ InactivityManager started")

            // Cập nhật state
            val sessionStartTime = System.currentTimeMillis()
            setState {
                copy(
                    isLoading = true,
                    error = null,
                    lessonId = lessonId,
                    sessionStartTime = sessionStartTime,
                    currentContentViewStartTime = sessionStartTime
                )
            }

            // Khởi động AutoSaveManager
            autoSaveManager.startAutoSave()
            Log.d(TAG, "   ✅ AutoSaveManager started (will save every 10 seconds)")

            // Tích hợp AppLifecycleManager
            inactivityManager.setAppLifecycleManager(appLifecycleManager)
            Log.d(TAG, "   ✅ AppLifecycleManager integrated with InactivityManager")

            // Lưu ý: AppLifecycleManager.startMonitoring() được gọi từ UI qua startLifecycleMonitoring()

            var resolvedStudentId: String? = null
            try {
                val currentUserId = currentUserIdFlow.value.ifBlank {
                    currentUserIdFlow.first { it.isNotBlank() }
                }
                if (currentUserId.isBlank()) {
                    Log.e(TAG, "loadLesson() aborted: currentUserId is blank")
                    showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                } else {
                    val profileResult = getStudentProfileByUserId(currentUserId)
                        .first { it !is Resource.Loading }
                    when (profileResult) {
                        is Resource.Success -> {
                            resolvedStudentId = profileResult.data?.id
                            if (resolvedStudentId.isNullOrBlank()) {
                                Log.e(TAG, "loadLesson() failed: studentId is null/blank")
                            } else {
                                setState { copy(studentId = resolvedStudentId) }
                            }
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "loadLesson() failed to resolve studentId: ${profileResult.message}")
                        }
                        is Resource.Loading -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadLesson() exception while resolving studentId", e)
            }

            val studentId = resolvedStudentId

            combine(
                lessonUseCases.getLessonById(lessonId),
                lessonUseCases.getLessonContentsByLesson(lessonId)
            ) { lessonResult, contentsResult ->
                Pair(lessonResult, contentsResult)
            }.collectLatest { (lessonResult, contentsResult) ->
                when {
                    lessonResult is Resource.Loading || contentsResult is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    lessonResult is Resource.Success && contentsResult is Resource.Success -> {
                        val contents = contentsResult.data?.sortedBy { it.order } ?: emptyList()

                        setState {
                            copy(
                                lesson = lessonResult.data,
                                lessonContents = contents,
                                currentContentIndex = 0,
                                isLoading = false,
                                error = null
                            )
                        }

                        contents.forEach { content ->
                            if (content.contentType != ContentType.TEXT && content.content.isNotEmpty()) {
                                loadDirectContentUrl(content)
                            }
                        }

                        if (!studentId.isNullOrBlank()) {
                            loadExistingProgressForLesson(studentId, lessonId)
                        }

                        if (!initialContentId.isNullOrBlank()) {
                            navigateToContentById(initialContentId)
                        }

                        contents.firstOrNull()?.let { firstContent ->
                            startContentView(firstContent.id, firstContent.contentType.name)
                        }

                        // Lưu tiến độ ngay lập tức sau khi bài học được tải
                        Log.d(TAG, "💾 Saving initial progress after lesson loaded...")
                        delay(500)  // Chờ state được cập nhật
                        saveProgress()
                    }
                    lessonResult is Resource.Error -> {
                        setState {
                            copy(isLoading = false, error = lessonResult.message)
                        }
                        showNotification(lessonResult.message, NotificationType.ERROR)
                    }
                    contentsResult is Resource.Error -> {
                        setState {
                            copy(isLoading = false, error = contentsResult.message)
                        }
                        showNotification(contentsResult.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun loadDirectContentUrl(content: LessonContent) {
        viewModelScope.launch {
            lessonUseCases.getDirectLessonContentUrl(content.content.trimStart('/'))
                .collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val url = result.data
                            if (!url.isNullOrBlank()) {
                                setState {
                                    copy(contentUrls = contentUrls + (content.id to url))
                                }
                            }
                        }
                        is Resource.Error -> {
                            showNotification("Lỗi khi tải URL: ${result.message}", NotificationType.ERROR)
                        }
                        is Resource.Loading -> {}
                    }
                }
        }
    }

    private suspend fun loadExistingProgressForLesson(studentId: String, lessonId: String) {
        try {
            val result = progressRepository
                .getLessonProgress(studentId, lessonId)
                .first { it !is Resource.Loading }

            when (result) {
                is Resource.Success -> {
                    val existing = result.data ?: return
                    val contents = state.value.lessonContents
                    if (contents.isEmpty()) {
                        setState {
                            copy(progress = existing, studentId = studentId, lessonId = lessonId)
                        }
                        return
                    }

                    val totalContents = contents.size
                    val percentage = existing.progressPercentage.coerceIn(0, 100)
                    val viewedCount = (percentage * totalContents) / 100
                    val safeViewedCount = viewedCount.coerceIn(0, totalContents)

                    val initialViewedIds = contents
                        .take(safeViewedCount)
                        .map { it.id }
                        .toMutableSet()

                    val lastContentIndex = existing.lastAccessedContentId?.let { lastId ->
                        contents.indexOfFirst { it.id == lastId }
                    } ?: -1

                    val targetIndex = when {
                        lastContentIndex in contents.indices -> lastContentIndex
                        safeViewedCount > 0 -> safeViewedCount - 1
                        else -> 0
                    }.coerceIn(0, contents.lastIndex)

                    if (targetIndex in contents.indices) {
                        initialViewedIds.add(contents[targetIndex].id)
                    }

                    setState {
                        copy(
                            progress = existing,
                            studentId = studentId,
                            lessonId = lessonId,
                            currentContentIndex = targetIndex,
                            viewedContentIds = initialViewedIds
                        )
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "loadExistingProgressForLesson() error: ${result.message}")
                }
                is Resource.Loading -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadExistingProgressForLesson() exception", e)
        }
    }

    private fun getMaxAccessibleIndex(state: StudentLessonViewState): Int {
        val contents = state.lessonContents
        if (contents.isEmpty()) return -1

        if (state.viewedContentIds.isEmpty()) return 0

        var maxAccessibleIndex = 0
        for (index in contents.indices) {
            val content = contents[index]
            if (content.id in state.viewedContentIds) {
                maxAccessibleIndex = index
            } else {
                break
            }
        }

        return (maxAccessibleIndex + 1).coerceAtMost(contents.lastIndex)
    }

    private fun navigateToNextContent() {
        val current = state.value
        if (current.canGoNext) {
            val maxAccessibleIndex = getMaxAccessibleIndex(current)
            val newIndex = current.currentContentIndex + 1
            if (newIndex > maxAccessibleIndex) return

            val targetContent = current.lessonContents.getOrNull(newIndex) ?: return

            contentViewTimeJob?.cancel()
            studyTimeManager.endContentView()
            studyTimeManager.startContentView()

            setState {
                copy(
                    currentContentIndex = newIndex,
                    viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                        viewedContentIds + targetContent.id
                    } else {
                        viewedContentIds
                    },
                    currentContentViewStartTime = System.currentTimeMillis(),
                    currentContentElapsedSeconds = 0
                )
            }

            startContentView(targetContent.id, targetContent.contentType.name)

            recordInteraction("NAVIGATION")
        }
    }

    private fun navigateToPreviousContent() {
        val current = state.value
        if (current.canGoPrevious) {
            val newIndex = current.currentContentIndex - 1
            val targetContent = current.lessonContents.getOrNull(newIndex) ?: return

            contentViewTimeJob?.cancel()
            studyTimeManager.endContentView()
            studyTimeManager.startContentView()

            setState {
                copy(
                    currentContentIndex = newIndex,
                    viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                        viewedContentIds + targetContent.id
                    } else {
                        viewedContentIds
                    },
                    currentContentViewStartTime = System.currentTimeMillis(),
                    currentContentElapsedSeconds = 0
                )
            }

            startContentView(targetContent.id, targetContent.contentType.name)

            recordInteraction("NAVIGATION")
        }
    }

    private fun navigateToContent(index: Int) {
        val current = state.value
        val maxAccessibleIndex = getMaxAccessibleIndex(current)
        if (index !in current.lessonContents.indices || index > maxAccessibleIndex) return

        val targetContent = current.lessonContents[index]

        contentViewTimeJob?.cancel()
        studyTimeManager.endContentView()
        studyTimeManager.startContentView()

        setState {
            copy(
                currentContentIndex = index,
                viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                    viewedContentIds + targetContent.id
                } else {
                    viewedContentIds
                },
                currentContentViewStartTime = System.currentTimeMillis(),
                currentContentElapsedSeconds = 0
            )
        }

        startContentView(targetContent.id, targetContent.contentType.name)

        recordInteraction("NAVIGATION")
    }

    private fun navigateToContentById(contentId: String) {
        val current = state.value
        val index = current.lessonContents.indexOfFirst { it.id == contentId }
        if (index < 0) return

        val maxAccessibleIndex = getMaxAccessibleIndex(current)
        if (index > maxAccessibleIndex) return

        val targetContent = current.lessonContents[index]

        contentViewTimeJob?.cancel()
        studyTimeManager.endContentView()
        studyTimeManager.startContentView()

        setState {
            copy(
                currentContentIndex = index,
                viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                    viewedContentIds + targetContent.id
                } else {
                    viewedContentIds
                },
                currentContentViewStartTime = System.currentTimeMillis(),
                currentContentElapsedSeconds = 0
            )
        }

        startContentView(targetContent.id, targetContent.contentType.name)
    }

    private fun markCurrentContentAsViewed() {
        val current = state.value
        val currentContent = current.currentContent ?: return

        val isCompleted = completionRulesManager.isContentCompleted(currentContent.id)

        if (currentContent.contentType == ContentType.VIDEO || currentContent.contentType == ContentType.AUDIO) {
            val progress = mediaProgressManager.getProgressPercentage(currentContent.id)
            val isFastForwarded = state.value.contentCompletionStatus[currentContent.id]?.isFastForwarded ?: false

            if (isFastForwarded) {
                Log.w(TAG, "⚠️ Cannot mark as viewed: Fast forward detected!")
                showNotification(
                    "Bạn đã tua nhanh video/audio. Vui lòng xem lại từ đầu.",
                    NotificationType.ERROR
                )
                return
            }

            if (progress < 70) {
                Log.w(TAG, "Cannot mark video/audio as viewed: only watched $progress% (need 70%)")
                showNotification(
                    "Bạn cần xem ít nhất 70% để hoàn thành ($progress%)",
                    NotificationType.ERROR
                )
                return
            }
        }

        Log.d(TAG, "✅ markCurrentContentAsViewed() contentId=${currentContent.id}, completed=$isCompleted")
        setState {
            copy(viewedContentIds = viewedContentIds + currentContent.id)
        }
    }

    private fun saveProgress() {
        viewModelScope.launch {
            Log.d(TAG, "💾 saveProgress() called")
            val currentState = state.value
            Log.d(TAG, "   - Current lesson: ${currentState.lesson?.id}")
            Log.d(TAG, "   - Study seriousness score: ${currentState.studySeriousnessScore}")
            Log.d(TAG, "   - Fast forward detected: ${currentState.isFastForwardDetected}")

            val lesson = currentState.lesson ?: return@launch
            val lessonId = currentState.lessonId ?: lesson.id

            // Lấy currentUserId
            Log.d(TAG, "   - Resolving currentUserId...")
            val currentUserId = currentUserIdFlow.value.ifBlank {
                Log.d(TAG, "   - CurrentUserId is blank, waiting for first non-blank value...")
                currentUserIdFlow.first { it.isNotBlank() }
            }
            Log.d(TAG, "   - CurrentUserId: $currentUserId")

            if (currentUserId.isBlank()) {
                Log.e(TAG, "❌ saveProgress() aborted: currentUserId is blank")
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            // Lấy studentId
            Log.d(TAG, "   - Resolving studentId...")
            var resolvedStudentId: String? = currentState.studentId
            if (resolvedStudentId.isNullOrBlank()) {
                Log.d(TAG, "   - StudentId is null/blank in state, fetching from profile...")
                try {
                    val profileResult = getStudentProfileByUserId(currentUserId)
                        .first { it !is Resource.Loading }
                    when (profileResult) {
                        is Resource.Success -> {
                            resolvedStudentId = profileResult.data?.id
                            Log.d(TAG, "   - ✅ Resolved studentId: $resolvedStudentId")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "❌ saveProgress() failed to resolve studentId: ${profileResult.message}")
                            showNotification(profileResult.message, NotificationType.ERROR)
                        }
                        is Resource.Loading -> {
                            Log.d(TAG, "   - ⏳ Loading student profile...")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception while resolving studentId: ${e.message}", e)
                }
            } else {
                Log.d(TAG, "   - ✅ Using studentId from state: $resolvedStudentId")
            }

            val studentId = resolvedStudentId
            if (studentId.isNullOrBlank()) {
                Log.e(TAG, "❌ saveProgress() aborted: studentId is null/blank")
                showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                return@launch
            }

            // Tính toán tiến độ
            Log.d(TAG, "   - Calculating progress...")
            val now = System.currentTimeMillis()
            val sessionStart = currentState.sessionStartTime.takeIf { it > 0 } ?: now
            val additionalSeconds = ((now - sessionStart) / 1000).coerceAtLeast(0)

            val progressPercentage = currentState.progressPercentage
            val lastContentId = currentState.currentContent?.id

            Log.d(TAG, "   - Session start: $sessionStart")
            Log.d(TAG, "   - Now: $now")
            Log.d(TAG, "   - Additional seconds: $additionalSeconds")
            Log.d(TAG, "   - Progress percentage: $progressPercentage%")
            Log.d(TAG, "   - Last content ID: $lastContentId")

            // Tạo params để lưu
            val params = UpdateLessonProgressParams(
                studentId = studentId,
                lessonId = lessonId,
                progressPercentage = progressPercentage,
                lastAccessedContentId = lastContentId,
                additionalTimeSeconds = additionalSeconds
            )

            Log.d(TAG, "📊 Preparing to save progress:")
            Log.d(TAG, "   - Student ID: $studentId")
            Log.d(TAG, "   - Lesson ID: $lessonId")
            Log.d(TAG, "   - Progress: $progressPercentage%")
            Log.d(TAG, "   - Time spent: $additionalSeconds seconds")
            Log.d(TAG, "   - Study seriousness score: ${currentState.studySeriousnessScore}")
            Log.d(TAG, "   - Fast forward detected: ${currentState.isFastForwardDetected}")

            // Cập nhật dữ liệu chờ lưu cho autoSaveManager
            Log.d(TAG, "   - Calling autoSaveManager.updatePendingProgress()...")
            autoSaveManager.updatePendingProgress(params)
            Log.d(TAG, "✅ Updated pending progress in autoSaveManager")

            // Lưu tiến độ
            lessonUseCases.updateLessonProgress(params).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "⏳ Saving progress to Room + Firebase...")
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "✅ Progress saved successfully!")
                        Log.d(TAG, "   - Collections created/updated:")
                        Log.d(TAG, "     • student_lesson_progress")
                        Log.d(TAG, "     • student_daily_study_time (if time > 0)")

                        // Reset session start time để tính thời gian thêm lần tiếp theo
                        val newSessionStartTime = System.currentTimeMillis()
                        setState {
                            copy(
                                progress = result.data,
                                studentId = studentId,
                                lessonId = lessonId,
                                isLoading = false,
                                sessionStartTime = newSessionStartTime
                            )
                        }

                        // Reset accumulated time trong StudyTimeManager
                        studyTimeManager.resetAccumulatedTime()
                        Log.d(TAG, "   ✅ StudyTimeManager: accumulated time reset")
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "❌ Error saving progress: ${result.message}")
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Không thể lưu tiến độ", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun recordInteraction(interactionType: String) {
        Log.d(TAG, "════════════════════════════════════════════════════════════════")
        Log.d(TAG, "👆 recordInteraction(type=$interactionType)")
        Log.d(TAG, "   - Interaction Type: $interactionType")
        Log.d(TAG, "   - Current warning count: ${state.value.inactivityWarningCount}")
        Log.d(TAG, "   - Current inactivity duration: ${inactivityManager.getInactivityDuration()}ms")
        Log.d(TAG, "════════════════════════════════════════════════════════════════")

        val currentTime = System.currentTimeMillis()
        val previousWarningCount = state.value.inactivityWarningCount

        val warningWasReset = inactivityManager.recordInteraction(interactionType)

        if (warningWasReset) {
            val newWarningCount = inactivityManager.getWarningCount()
            Log.d(TAG, "✅ Warning count was reset due to interaction: $interactionType")
            Log.d(TAG, "   - Previous count: $previousWarningCount → New count: $newWarningCount")
            
            // Cập nhật state với warning count mới
            setState {
                copy(
                    lastInteractionTime = currentTime,
                    inactivityWarningCount = newWarningCount
                )
            }

            // Hiển thị thông báo cho user biết warning đã được reset
            if (previousWarningCount > 0) {
                showNotification(
                    "✅ Đã ghi nhận hoạt động. Số cảnh báo đã được đặt lại (từ $previousWarningCount → $newWarningCount)",
                    NotificationType.SUCCESS
                )
            }
        } else {
            setState { copy(lastInteractionTime = currentTime) }
        }

        // Cập nhật study seriousness score
        updateSeriousnessScore()
    }

    private fun showInactivityWarning() {
        // Lấy số lần cảnh báo từ InactivityManager
        val warningCount = inactivityManager.getWarningCount()
        Log.d(TAG, "⚠️ showInactivityWarning() count=$warningCount/${LearningProgressConfig.MAX_INACTIVITY_WARNINGS}")

        val currentState = state.value
        Log.d(TAG, "   - Current content: ${currentState.currentContent?.title}")
        Log.d(TAG, "   - Progress: ${currentState.progressPercentage}%")
        Log.d(TAG, "   - Time spent: ${(System.currentTimeMillis() - currentState.sessionStartTime) / 1000}s")

        // Cập nhật state để hiển thị dialog
        setState {
            copy(
                inactivityWarningCount = warningCount,
                showInactivityWarning = true
            )
        }
        Log.d(TAG, "   ✅ State updated: showInactivityWarning = true")

        // Kiểm tra xem có nên thoát không
        if (inactivityManager.shouldExitSession()) {
            Log.d(TAG, "❌ MAX WARNINGS REACHED - Auto-exiting immediately")

            showNotification(
                "Bạn đã không tương tác quá lâu. Tiến trình sẽ không được lưu.",
                NotificationType.ERROR
            )

            // Thoát sau delay
            autoExitJob?.cancel()
            autoExitJob = viewModelScope.launch {
                delay(AUTO_EXIT_DELAY)
                Log.d(TAG, "   ⏳ Waiting ${AUTO_EXIT_DELAY}ms before exit...")
                Log.d(TAG, "❌ Auto-exiting lesson due to max warnings")
                exitLessonWithoutSaving()
            }
        } else {
            // Hiển thị notification cảnh báo
            Log.d(TAG, "📢 Showing notification for warning #$warningCount")
            showNotification(
                "Bạn vẫn đang theo dõi bài học chứ? Hãy tương tác để tiếp tục! ($warningCount/${LearningProgressConfig.MAX_INACTIVITY_WARNINGS})",
                NotificationType.ERROR
            )
        }
    }

    private fun dismissInactivityWarning() {
        Log.d(TAG, "dismissInactivityWarning() called")
        setState { copy(showInactivityWarning = false) }
    }

    private fun continueLesson() {
        Log.d(TAG, "▶️ continueLesson() - User confirmed to continue")

        // Ẩn cảnh báo
        setState { copy(showInactivityWarning = false) }

        // ⚠️ KHÔNG reset warning count ở đây
        // Warning count chỉ reset khi user tương tác với nội dung bài học
        // Không reset khi nhấn nút trong dialog cảnh báo

        // Khởi động lại timer để bắt đầu đếm 60 giây mới
        inactivityManager.startInactivityTracking()

        Log.d(TAG, "   ✅ Dialog closed, inactivity tracking restarted (warning count NOT reset)")
    }

    private fun updateSeriousnessScore() {
        val statusMap = state.value.contentCompletionStatus
        val newScore = calculateSeriousnessScore(statusMap)

        if (newScore != state.value.studySeriousnessScore) {
            setState { copy(studySeriousnessScore = newScore) }
            Log.d(TAG, "📊 Study seriousness score updated: $newScore")
        }
    }

    private fun exitLessonWithoutSaving() {
        Log.d(TAG, "❌ exitLessonWithoutSaving() - NOT saving progress")

        val currentState = state.value
        Log.d(TAG, "   - Lesson: ${currentState.lesson?.title}")
        Log.d(TAG, "   - Progress: ${currentState.progressPercentage}%")
        Log.d(TAG, "   - Time spent: ${(System.currentTimeMillis() - currentState.sessionStartTime) / 1000}s")
        Log.d(TAG, "   - Reason: Inactivity (${currentState.inactivityWarningCount} warnings)")

        // Dừng tất cả managers
        Log.d(TAG, "   ⏹️ Stopping all managers...")

        autoSaveManager.stopAutoSave()
        Log.d(TAG, "      ✅ AutoSaveManager stopped")

        inactivityManager.stopInactivityTracking()
        Log.d(TAG, "      ✅ InactivityManager stopped")

        sessionManager.endSession()
        Log.d(TAG, "      ✅ SessionManager ended")

        studyTimeManager.endSession()
        Log.d(TAG, "      ✅ StudyTimeManager ended")

        autoExitJob?.cancel()
        Log.d(TAG, "      ✅ Auto-exit job cancelled")

        // Hiển thị notification
        Log.d(TAG, "   📢 Showing exit notification...")
        showNotification(
            "Bạn đã thoát khỏi bài học. Tiến trình không được lưu.",
            NotificationType.ERROR
        )

        // Cập nhật state để thoát
        setState { copy(shouldAutoExitLesson = true) }
        Log.d(TAG, "   ✅ State updated: shouldAutoExitLesson = true")
    }

    private fun onMediaStateChanged(isPlaying: Boolean, contentType: ContentType?) {
        Log.d(TAG, "onMediaStateChanged() isPlaying=$isPlaying, type=$contentType")

        setState {
            copy(
                isMediaPlaying = isPlaying,
                currentMediaType = contentType
            )
        }

        // Chỉ ghi nhận tương tác khi user PLAY (user tương tác thực sự)
        // Không ghi nhận khi PAUSE vì có thể do dialog hiển thị tự động pause
        if (isPlaying) {
            recordInteraction("MEDIA_PLAY")
        }
    }

    private fun onMediaProgress(duration: Long, position: Long) {
        val currentContent = state.value.currentContent ?: return

        when (currentContent.contentType) {
            ContentType.VIDEO -> {
                updateVideoPosition(currentContent.id, position, duration)
            }
            ContentType.AUDIO -> {
                updateAudioPosition(currentContent.id, position, duration)
            }
            else -> {}
        }
    }

    private fun onMediaSeek(fromPosition: Long, toPosition: Long) {
        Log.d(TAG, "onMediaSeek() from=$fromPosition to=$toPosition")
        recordInteraction("MEDIA_SEEK")
    }

    private fun validateVideoProgress() {
        val currentContent = state.value.currentContent ?: return

        // Lấy tiến độ từ MediaProgressManager
        val progress = mediaProgressManager.getProgressPercentage(currentContent.id)
        val isFastForwarded = state.value.contentCompletionStatus[currentContent.id]?.isFastForwarded ?: false

        Log.d(TAG, "validateVideoProgress() watched=$progress%, fastForward=$isFastForwarded")

        if (isFastForwarded) {
            showNotification(
                "Bạn đã tua nhanh video. Vui lòng xem lại từ đầu.",
                NotificationType.ERROR
            )
            return
        }

        // Kiểm tra xem đã xem đủ 70% chưa
        if (progress < 70) {
            showNotification(
                "Bạn cần xem ít nhất 70% video để hoàn thành (hiện tại: $progress%)",
                NotificationType.ERROR
            )
        } else {
            // Đánh dấu nội dung là đã xem
            markCurrentContentAsViewed()
            // Lưu tiến độ
            saveProgress()
        }
    }

    // ========== CompletionRulesManager Event Handlers ==========

    fun handleContentViewed(contentType: String, elapsedSeconds: Long) {
        val currentContent = state.value.currentContent ?: return
        Log.d(TAG, "handleContentViewed() type=$contentType, elapsed=${elapsedSeconds}s")

        // Gọi CompletionRulesManager để xử lý nội dung được xem
        when (contentType.uppercase()) {
            "TEXT" -> {
                Log.d(TAG, "   📄 Calling completionRulesManager.handleTextViewed()...")
                completionRulesManager.handleTextViewed(currentContent.id, elapsedSeconds)
            }
            "IMAGE" -> {
                Log.d(TAG, "   🖼️ Calling completionRulesManager.handleImageViewed()...")
                completionRulesManager.handleImageViewed(currentContent.id, elapsedSeconds)
            }
            else -> {}
        }
    }

    fun handlePdfScrolled(scrollPercentage: Int) {
        val currentContent = state.value.currentContent ?: return
        Log.d(TAG, "handlePdfScrolled() percentage=$scrollPercentage%")

        // Gọi CompletionRulesManager để xử lý PDF cuộn
        Log.d(TAG, "   📕 Calling completionRulesManager.handlePdfScrolled()...")
        completionRulesManager.handlePdfScrolled(currentContent.id, scrollPercentage)
    }

    fun handleMediaPositionChanged(contentType: String, position: Long, duration: Long) {
        val currentContent = state.value.currentContent ?: return
        Log.d(TAG, "handleMediaPositionChanged() type=$contentType, pos=$position, dur=$duration")

        // Gọi CompletionRulesManager để xử lý vị trí media thay đổi
        when (contentType.uppercase()) {
            "VIDEO" -> {
                Log.d(TAG, "   ▶️ Calling completionRulesManager.handleVideoPositionChanged()...")
                completionRulesManager.handleVideoPositionChanged(currentContent.id, position, duration)
            }
            "AUDIO" -> {
                Log.d(TAG, "   🔊 Calling completionRulesManager.handleAudioPositionChanged()...")
                completionRulesManager.handleAudioPositionChanged(currentContent.id, position, duration)
            }
            else -> {}
        }
    }

    // ========== NavigationControlManager Integration ==========

    fun checkNavigationAllowed(targetIndex: Int): Boolean {
        val currentState = state.value
        Log.d(TAG, "checkNavigationAllowed() target=$targetIndex")

        // Kiểm tra xem có thể điều hướng không
        val (canNavigate, reason) = navigationControlManager.canNavigateTo(
            lessonContents = currentState.lessonContents,
            currentIndex = currentState.currentContentIndex,
            selectedIndex = targetIndex
        )

        if (!canNavigate) {
            Log.w(TAG, "   ⚠️ Navigation blocked: $reason")
            showNotification(reason, NotificationType.ERROR)
        } else {
            Log.d(TAG, "   ✅ Navigation allowed")
        }

        return canNavigate
    }

    fun getContentStatusInfo(contentId: String): String {
        val currentState = state.value
        val content = currentState.lessonContents.find { it.id == contentId } ?: return ""

        Log.d(TAG, "getContentStatusInfo() contentId=$contentId")

        // Lấy thông tin trạng thái nội dung từ NavigationControlManager
        val status = navigationControlManager.getContentStatus(
            contentId = content.id,
            contentTitle = content.title,
            contentType = content.contentType.name
        )
        Log.d(TAG, "   📊 Status:\n$status")
        return status
    }

    fun getCompletedContentsList(): List<String> {
        Log.d(TAG, "getCompletedContentsList() - Getting all completed contents")

        // Lấy danh sách nội dung đã hoàn thành từ CompletionRulesManager
        val completed = completionRulesManager.getCompletedContents()
        Log.d(TAG, "   ✅ Completed contents: ${completed.size} items")
        completed.forEach { contentId ->
            Log.d(TAG, "      - $contentId")
        }
        return completed
    }

    fun getContentProgressInfo(contentId: String): Int {
        Log.d(TAG, "getContentProgressInfo() contentId=$contentId")

        // Lấy tiến độ nội dung từ CompletionRulesManager
        val progress = completionRulesManager.getContentProgress(contentId)
        Log.d(TAG, "   📊 Progress: $progress%")
        return progress
    }

    fun getCompletionRule(contentType: String): String {
//        Log.d(TAG, "getCompletionRule() contentType=$contentType")

        // Lấy quy tắc hoàn thành từ CompletionRulesManager
        val rule = completionRulesManager.getCompletionRule(contentType)
//        Log.d(TAG, "   📋 Rule: $rule")
        return rule
    }

    fun isContentCompleted(contentId: String): Boolean {
        Log.d(TAG, "isContentCompleted() contentId=$contentId")

        // Kiểm tra xem nội dung có hoàn thành không
        val isCompleted = completionRulesManager.isContentCompleted(contentId)
        Log.d(TAG, "   ✅ Is completed: $isCompleted")
        return isCompleted
    }

    fun clearCompletionRules() {
        Log.d(TAG, "clearCompletionRules() - Clearing all completion rules")

        // Xóa tất cả quy tắc hoàn thành
        completionRulesManager.clear()
        Log.d(TAG, "   ✅ Completion rules cleared")
    }

    override fun onCleared() {
        super.onCleared()

        Log.d(TAG, "🧹 ViewModel cleared - cleaning up")

        // Dừng lifecycle monitoring
        stopLifecycleMonitoring()

        // Kết thúc các session
        sessionManager.endSession()
        studyTimeManager.endSession()

        // Dừng auto save
        autoSaveManager.stopAutoSave()

        // Dừng inactivity tracking
        inactivityManager.stopInactivityTracking()

        // Cancel jobs
        contentViewTimeJob?.cancel()
        autoExitJob?.cancel()

        // Xóa các callback
        pauseMediaPlayerCallback = null
        resumeMediaPlayerCallback = null
    }
}
