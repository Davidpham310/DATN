package com.example.datn.presentation.student.lessons

import android.util.Log
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentLessonViewViewModel @Inject constructor(
    private val lessonUseCases: LessonUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    private val progressRepository: IProgressRepository,
    notificationManager: NotificationManager
) : BaseViewModel<StudentLessonViewState, StudentLessonViewEvent>(
    StudentLessonViewState(),
    notificationManager
) {

    companion object {
        private const val TAG = "StudentLessonViewVM"
        private const val AUTO_EXIT_DELAY = 5000L
        private const val VIDEO_PASSIVE_CHECK_INTERVAL = 30000L // Check every 30s during video
    }

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // Managers
    private val interactionManager = InteractionManager()
    private val sessionTimer = SessionTimer()
    private val mediaTrackingManager = MediaTrackingManager()
    private val progressManager = ProgressManager()
    private val completionManager = LessonCompletionManager()

    private lateinit var autoSaveManager: AutoSaveManager
    private lateinit var inactivityManager: InactivityManager

    private var autoExitJob: Job? = null
    private var videoPassiveCheckJob: Job? = null

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

            // Interaction tracking
            is StudentLessonViewEvent.RecordInteraction -> recordInteraction(event.interactionType)
            StudentLessonViewEvent.ShowInactivityWarning -> showInactivityWarning()
            StudentLessonViewEvent.DismissInactivityWarning -> dismissInactivityWarning()
            StudentLessonViewEvent.ContinueLesson -> continueLesson()
            StudentLessonViewEvent.ExitLessonWithoutSaving -> exitLessonWithoutSaving()

            // Media tracking
            is StudentLessonViewEvent.OnMediaStateChanged -> onMediaStateChanged(event.isPlaying, event.contentType)
            is StudentLessonViewEvent.OnMediaProgress -> onMediaProgress(event.duration, event.position)
            is StudentLessonViewEvent.OnMediaSeek -> onMediaSeek(event.fromPosition, event.toPosition)
            StudentLessonViewEvent.ValidateVideoProgress -> validateVideoProgress()
            else -> {}
        }
    }

    private fun loadLesson(lessonId: String, initialContentId: String?) {
        viewModelScope.launch {
            Log.d(TAG, "loadLesson() called with lessonId=$lessonId")

            // Initialize managers with enhanced callback
            autoSaveManager = AutoSaveManager(viewModelScope) { saveProgress() }
            inactivityManager = InactivityManager(viewModelScope, interactionManager) { warningCount ->
                showInactivityWarning(warningCount)
            }

            sessionTimer.startSession()

            setState {
                copy(
                    isLoading = true,
                    error = null,
                    lessonId = lessonId,
                    sessionStartTime = System.currentTimeMillis()
                )
            }

            // Resolve studentId
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
                        
                        // Initialize completion manager
                        completionManager.initialize(contents)
                        
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

                        // Start managers
                        autoSaveManager.startAutoSave()
                        inactivityManager.startInactivityCheck()

                        // Start passive video check if current content is video
                        val currentContent = state.value.currentContent
                        if (currentContent?.contentType == ContentType.VIDEO) {
                            startVideoPassiveCheck()
                        }
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
        
        // Nội dung đầu tiên luôn được truy cập
        if (state.viewedContentIds.isEmpty()) return 0

        // Tìm nội dung hoàn thành cuối cùng (100%)
        var maxAccessibleIndex = 0
        for (index in contents.indices) {
            val content = contents[index]
            if (content.id in state.viewedContentIds) {
                // Nội dung này đã hoàn thành 100%
                maxAccessibleIndex = index
            } else {
                // Nội dung này chưa hoàn thành, dừng lại
                break
            }
        }

        // Cho phép truy cập nội dung tiếp theo (nếu có)
        return (maxAccessibleIndex + 1).coerceAtMost(contents.lastIndex)
    }

    private fun navigateToNextContent() {
        val current = state.value
        if (current.canGoNext) {
            val maxAccessibleIndex = getMaxAccessibleIndex(current)
            val newIndex = current.currentContentIndex + 1
            if (newIndex > maxAccessibleIndex) return

            val targetContent = current.lessonContents.getOrNull(newIndex) ?: return

            // Stop video check when leaving video content
            if (current.currentContent?.contentType == ContentType.VIDEO) {
                stopVideoPassiveCheck()
                mediaTrackingManager.resetMediaState()
            }

            setState {
                copy(
                    currentContentIndex = newIndex,
                    viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                        viewedContentIds + targetContent.id
                    } else {
                        viewedContentIds
                    }
                )
            }

            // Start video check for new content if video
            if (targetContent.contentType == ContentType.VIDEO) {
                startVideoPassiveCheck()
            }

            recordInteraction("NAVIGATION")
        }
    }

    private fun navigateToPreviousContent() {
        val current = state.value
        if (current.canGoPrevious) {
            val newIndex = current.currentContentIndex - 1
            val targetContent = current.lessonContents.getOrNull(newIndex) ?: return

            if (current.currentContent?.contentType == ContentType.VIDEO) {
                stopVideoPassiveCheck()
                mediaTrackingManager.resetMediaState()
            }

            setState {
                copy(
                    currentContentIndex = newIndex,
                    viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                        viewedContentIds + targetContent.id
                    } else {
                        viewedContentIds
                    }
                )
            }

            if (targetContent.contentType == ContentType.VIDEO) {
                startVideoPassiveCheck()
            }

            recordInteraction("NAVIGATION")
        }
    }

    private fun navigateToContent(index: Int) {
        val current = state.value
        val maxAccessibleIndex = getMaxAccessibleIndex(current)
        if (index !in current.lessonContents.indices || index > maxAccessibleIndex) return

        val targetContent = current.lessonContents[index]

        if (current.currentContent?.contentType == ContentType.VIDEO) {
            stopVideoPassiveCheck()
            mediaTrackingManager.resetMediaState()
        }

        setState {
            copy(
                currentContentIndex = index,
                viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                    viewedContentIds + targetContent.id
                } else {
                    viewedContentIds
                }
            )
        }

        if (targetContent.contentType == ContentType.VIDEO) {
            startVideoPassiveCheck()
        }

        recordInteraction("NAVIGATION")
    }

    private fun navigateToContentById(contentId: String) {
        val current = state.value
        val index = current.lessonContents.indexOfFirst { it.id == contentId }
        if (index < 0) return

        val maxAccessibleIndex = getMaxAccessibleIndex(current)
        if (index > maxAccessibleIndex) return

        val targetContent = current.lessonContents[index]

        if (current.currentContent?.contentType == ContentType.VIDEO) {
            stopVideoPassiveCheck()
            mediaTrackingManager.resetMediaState()
        }

        setState {
            copy(
                currentContentIndex = index,
                viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                    viewedContentIds + targetContent.id
                } else {
                    viewedContentIds
                }
            )
        }

        if (targetContent.contentType == ContentType.VIDEO) {
            startVideoPassiveCheck()
        }
    }

    private fun markCurrentContentAsViewed() {
        val current = state.value
        val currentContent = current.currentContent ?: return

        // For video content, validate watch percentage before marking
        if (currentContent.contentType == ContentType.VIDEO) {
            if (!mediaTrackingManager.hasWatchedEnough()) {
                val watchPercentage = mediaTrackingManager.getWatchPercentage()
                Log.w(TAG, "Cannot mark video as viewed: only watched $watchPercentage% (need 70%)")
                showNotification(
                    "Bạn cần xem ít nhất 70% video để hoàn thành ($watchPercentage%)",
                    NotificationType.ERROR
                )
                return
            }

            // Check for suspicious behavior
            if (mediaTrackingManager.isSuspiciousBehavior()) {
                val details = mediaTrackingManager.getSuspiciousDetails()
                Log.w(TAG, "Suspicious video behavior detected: $details")
                showNotification(
                    "Phát hiện hành vi bất thường khi xem video",
                    NotificationType.ERROR
                )
                // Still mark as viewed but log for review
            }
        }

        Log.d(TAG, "markCurrentContentAsViewed() contentId=${currentContent.id}")
        setState {
            copy(viewedContentIds = viewedContentIds + currentContent.id)
        }
    }

    private fun saveProgress() {
        viewModelScope.launch {
            val currentState = state.value
            val lesson = currentState.lesson ?: return@launch
            val lessonId = currentState.lessonId ?: lesson.id

            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.first { it.isNotBlank() }
            }
            if (currentUserId.isBlank()) {
                Log.e(TAG, "saveProgress() aborted: currentUserId is blank")
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            var resolvedStudentId: String? = currentState.studentId
            if (resolvedStudentId.isNullOrBlank()) {
                getStudentProfileByUserId(currentUserId).collectLatest { profileResult ->
                    when (profileResult) {
                        is Resource.Success -> {
                            resolvedStudentId = profileResult.data?.id
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "saveProgress() failed to resolve studentId: ${profileResult.message}")
                            showNotification(profileResult.message, NotificationType.ERROR)
                        }
                        is Resource.Loading -> {}
                    }
                }
            }

            val studentId = resolvedStudentId
            if (studentId.isNullOrBlank()) {
                Log.e(TAG, "saveProgress() aborted: studentId is null/blank")
                showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                return@launch
            }

            val now = System.currentTimeMillis()
            val sessionStart = currentState.sessionStartTime.takeIf { it > 0 } ?: now
            val additionalSeconds = ((now - sessionStart) / 1000).coerceAtLeast(0)

            val progressPercentage = currentState.progressPercentage
            val lastContentId = currentState.currentContent?.id

            val params = UpdateLessonProgressParams(
                studentId = studentId,
                lessonId = lessonId,
                progressPercentage = progressPercentage,
                lastAccessedContentId = lastContentId,
                additionalTimeSeconds = additionalSeconds
            )

            lessonUseCases.updateLessonProgress(params).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        setState {
                            copy(
                                progress = result.data,
                                studentId = studentId,
                                lessonId = lessonId,
                                isLoading = false,
                                sessionStartTime = now
                            )
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Không thể lưu tiến độ", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    // ========== ENHANCED INTERACTION TRACKING ==========

    private fun recordInteraction(interactionType: String = "CLICK") {
        Log.d(TAG, "recordInteraction() type=$interactionType")

        if (state.value.showInactivityWarning) {
            Log.d(TAG, "recordInteraction() - Warning active, ignoring")
            return
        }

        interactionManager.recordInteraction(interactionType)

        setState {
            copy(
                lastInteractionTime = System.currentTimeMillis(),
                showInactivityWarning = false
            )
        }
    }

    private fun showInactivityWarning(warningCount: Int = 1) {
        Log.d(TAG, "showInactivityWarning() count=$warningCount")

        setState {
            copy(
                inactivityWarningCount = warningCount,
                showInactivityWarning = true
            )
        }

        if (warningCount >= 3) {
            Log.d(TAG, "Max warnings reached (3/3), auto-exiting immediately")

            showNotification(
                "Bạn đã không tương tác quá lâu. Tiến trình sẽ không được lưu.",
                NotificationType.ERROR
            )

            autoExitJob?.cancel()
            autoExitJob = viewModelScope.launch {
                delay(AUTO_EXIT_DELAY)  // 5 giây
                Log.d(TAG, "Auto-exiting lesson due to max warnings")
                exitLessonWithoutSaving()
            }
        } else {
            // Cảnh báo 1/3 hoặc 2/3
            showNotification(
                "Bạn vẫn đang theo dõi bài học chứ? Hãy tương tác để tiếp tục! ($warningCount/3)",
                NotificationType.ERROR
            )

            // Timeout 10 giây - nếu không click thì auto-continue
            autoExitJob?.cancel()
            autoExitJob = viewModelScope.launch {
                delay(10000)  // 10 giây
                Log.d(TAG, "Dialog timeout (10s), auto-continuing")
                continueLesson()
            }
        }
    }

    private fun dismissInactivityWarning() {
        Log.d(TAG, "dismissInactivityWarning() called")
        setState { copy(showInactivityWarning = false) }
    }

    private fun continueLesson() {
        Log.d(TAG, "continueLesson() - dismissing warning dialog")

        // Cancel timeout 10 giây (nếu user click button)
        autoExitJob?.cancel()

        val currentWarningCount = state.value.inactivityWarningCount
        
        // Nếu đây là warning lần 3, set lên 4 để lần tiếp theo auto-exit
        if (currentWarningCount >= 3) {
            Log.d(TAG, "continueLesson() - Warning count is 3, setting to 4 for next auto-exit")
            setState {
                copy(
                    showInactivityWarning = false,
                    inactivityWarningCount = 4  // Set to 4 để lần tiếp theo auto-exit
                )
            }
        } else {
            // Dismiss dialog và reset warning count (chỉ cho warnings 1 và 2)
            setState {
                copy(
                    showInactivityWarning = false,
                    inactivityWarningCount = 0  // Reset warning count
                )
            }
        }

        // Record interaction để reset timer không tương tác
        recordInteraction("CONTINUE_LESSON")

        // Tiếp tục kiểm tra không tương tác
        if (::inactivityManager.isInitialized) {
            inactivityManager.startInactivityCheck()
        }
    }

    private fun exitLessonWithoutSaving() {
        Log.d(TAG, "exitLessonWithoutSaving() - NOT saving progress")

        autoSaveManager.stopAutoSave()
        inactivityManager.stopInactivityCheck()
        sessionTimer.stopSession()
        stopVideoPassiveCheck()
        autoExitJob?.cancel()

        showNotification(
            "Bạn đã thoát khỏi bài học. Tiến trình không được lưu.",
            NotificationType.ERROR
        )

        setState { copy(shouldAutoExitLesson = true) }
    }

    // ========== MEDIA TRACKING INTEGRATION ==========

    private fun onMediaStateChanged(isPlaying: Boolean, contentType: ContentType?) {
        Log.d(TAG, "onMediaStateChanged() isPlaying=$isPlaying, type=$contentType")

        mediaTrackingManager.setMediaPlaying(isPlaying, contentType)

        setState {
            copy(
                isMediaPlaying = isPlaying,
                currentMediaType = contentType
            )
        }

        // Record interaction when playing/pausing
        recordInteraction(if (isPlaying) "MEDIA_PLAY" else "MEDIA_PAUSE")

        // Restart passive check if resumed
        if (isPlaying && contentType == ContentType.VIDEO) {
            startVideoPassiveCheck()
        }
    }

    private fun onMediaProgress(duration: Long, position: Long) {
        mediaTrackingManager.updateMediaInfo(duration, position)

        setState {
            copy(
                mediaDuration = duration,
                mediaPosition = position
            )
        }
    }

    private fun onMediaSeek(fromPosition: Long, toPosition: Long) {
        Log.d(TAG, "onMediaSeek() from=$fromPosition to=$toPosition")

        mediaTrackingManager.recordSeek(fromPosition, toPosition)
        recordInteraction("MEDIA_SEEK")
    }

    private fun validateVideoProgress() {
        val hasWatchedEnough = mediaTrackingManager.hasWatchedEnough()
        val watchPercentage = mediaTrackingManager.getWatchPercentage()

        Log.d(TAG, "validateVideoProgress() watched=$watchPercentage%, enough=$hasWatchedEnough")

        if (!hasWatchedEnough) {
            showNotification(
                "Bạn cần xem ít nhất 70% video để hoàn thành (hiện tại: $watchPercentage%)",
                NotificationType.ERROR
            )
        } else {
            markCurrentContentAsViewed()
            saveProgress()
        }
    }

    private fun startVideoPassiveCheck() {
        videoPassiveCheckJob?.cancel()

        Log.d(TAG, "Starting video passive check")

        videoPassiveCheckJob = viewModelScope.launch {
            while (isActive) {
                delay(VIDEO_PASSIVE_CHECK_INTERVAL)

                val isPlaying = mediaTrackingManager.isMediaPlaying()
                val timeSinceInteraction = interactionManager.getTimeSinceLastInteraction()

                // If video is playing but no interaction for 30s
                if (isPlaying && timeSinceInteraction > VIDEO_PASSIVE_CHECK_INTERVAL) {
                    Log.w(TAG, "Video playing without interaction for ${timeSinceInteraction}ms")

                    // Check if paused too long
                    if (mediaTrackingManager.isPausedTooLong()) {
                        Log.w(TAG, "Video paused too long!")
                        showNotification(
                            "Video đang tạm dừng quá lâu. Vui lòng tiếp tục xem.",
                            NotificationType.ERROR
                        )
                    }
                }

                // Check for suspicious behavior
                if (mediaTrackingManager.isSuspiciousBehavior()) {
                    val details = mediaTrackingManager.getSuspiciousDetails()
                    Log.w(TAG, "Suspicious video behavior: $details")
                }
            }
        }
    }

    private fun stopVideoPassiveCheck() {
        videoPassiveCheckJob?.cancel()
        videoPassiveCheckJob = null
        Log.d(TAG, "Stopped video passive check")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared() - stopping all managers")

        if (::autoSaveManager.isInitialized) {
            autoSaveManager.stopAutoSave()
        }
        if (::inactivityManager.isInitialized) {
            inactivityManager.stopInactivityCheck()
        }
        sessionTimer.stopSession()
        stopVideoPassiveCheck()
        autoExitJob?.cancel()

        if (!state.value.shouldAutoExitLesson) {
            viewModelScope.launch {
                Log.d(TAG, "onCleared() -> final saveProgress()")
                saveProgress()
            }
        }
    }
}