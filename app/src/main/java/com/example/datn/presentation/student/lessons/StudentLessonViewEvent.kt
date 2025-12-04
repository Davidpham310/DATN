package com.example.datn.presentation.student.lessons

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.ContentType

sealed class StudentLessonViewEvent : BaseEvent {

    // ========== LESSON LOADING & BASIC NAVIGATION ==========

    /**
     * Load lesson data with optional initial content
     * @param lessonId ID of the lesson to load
     * @param initialContentId Optional ID of specific content to jump to
     */
    data class LoadLesson(
        val lessonId: String,
        val initialContentId: String? = null
    ) : StudentLessonViewEvent()

    /**
     * Navigate to next content (if available and accessible)
     */
    object NextContent : StudentLessonViewEvent()

    /**
     * Navigate to previous content (if available)
     */
    object PreviousContent : StudentLessonViewEvent()

    /**
     * Navigate to specific content by index
     * @param index Target content index (must be within accessible range)
     */
    data class GoToContent(val index: Int) : StudentLessonViewEvent()

    /**
     * Mark current content as viewed
     * For video content, validates 70% watch requirement first
     */
    object MarkCurrentAsViewed : StudentLessonViewEvent()

    // ========== PROGRESS MANAGEMENT ==========

    /**
     * Show progress summary dialog
     */
    object ShowProgressDialog : StudentLessonViewEvent()

    /**
     * Dismiss progress dialog
     */
    object DismissProgressDialog : StudentLessonViewEvent()

    /**
     * Manually trigger progress save
     * (Auto-save runs every 10s automatically)
     */
    object SaveProgress : StudentLessonViewEvent()

    /**
     * Force complete current lesson (admin/debug)
     */
    object ForceCompleteLesson : StudentLessonViewEvent()

    // ========== INTERACTION TRACKING ==========

    /**
     * Record user interaction to prevent inactivity warnings
     * @param interactionType Type of interaction:
     *   - CLICK: Button/element click
     *   - TAP: Screen tap
     *   - LONG_PRESS: Long press gesture
     *   - SCROLL: Scrolling content
     *   - SWIPE: Swipe gesture
     *   - NAVIGATION: Content navigation
     *   - TEXT_INPUT: Typing in input field
     *   - CONTINUE: Continue after warning
     *   - MEDIA_PLAY: Video/audio play
     *   - MEDIA_PAUSE: Video/audio pause
     *   - MEDIA_SEEK: Video/audio seek
     */
    data class RecordInteraction(
        val interactionType: String = "CLICK"
    ) : StudentLessonViewEvent()

    /**
     * Show inactivity warning dialog
     * Triggered automatically after 60s of no interaction
     */
    object ShowInactivityWarning : StudentLessonViewEvent()

    /**
     * Dismiss inactivity warning without resetting timer
     */
    object DismissInactivityWarning : StudentLessonViewEvent()

    /**
     * User confirms they're still learning
     * Resets inactivity timer and warning count
     */
    object ContinueLesson : StudentLessonViewEvent()

    /**
     * Exit lesson without saving progress
     * Triggered by:
     *   - User choosing to exit from warning
     *   - Auto-exit after 3 warnings + 5s timeout
     */
    object ExitLessonWithoutSaving : StudentLessonViewEvent()

    /**
     * Pause inactivity detection temporarily
     * Use when showing dialogs/overlays that prevent interaction
     */
    object PauseInactivityDetection : StudentLessonViewEvent()

    /**
     * Resume inactivity detection
     */
    object ResumeInactivityDetection : StudentLessonViewEvent()

    // ========== MEDIA TRACKING (VIDEO/AUDIO) ==========

    /**
     * Update media playback state
     * @param isPlaying Whether media is currently playing
     * @param contentType Type of media (VIDEO or AUDIO)
     */
    data class OnMediaStateChanged(
        val isPlaying: Boolean,
        val contentType: ContentType?
    ) : StudentLessonViewEvent()

    /**
     * Update media playback progress
     * Should be called every ~1 second during playback
     * @param duration Total media duration in milliseconds
     * @param position Current playback position in milliseconds
     */
    data class OnMediaProgress(
        val duration: Long,
        val position: Long
    ) : StudentLessonViewEvent()

    /**
     * Record media seek event
     * @param fromPosition Position before seek (ms)
     * @param toPosition Position after seek (ms)
     */
    data class OnMediaSeek(
        val fromPosition: Long,
        val toPosition: Long
    ) : StudentLessonViewEvent()

    /**
     * Validate if video has been watched enough (≥70%)
     * Shows warning if not, allows marking as viewed if yes
     */
    object ValidateVideoProgress : StudentLessonViewEvent()

    /**
     * User requests to skip video validation (admin/debug only)
     */
    object SkipVideoValidation : StudentLessonViewEvent()

    /**
     * Show video completion overlay (when ≥70% watched)
     */
    object ShowVideoCompletionOverlay : StudentLessonViewEvent()

    /**
     * Dismiss video completion overlay
     */
    object DismissVideoCompletionOverlay : StudentLessonViewEvent()

    /**
     * Media playback error occurred
     * @param errorMessage Error description
     * @param errorCode Optional error code
     */
    data class OnMediaError(
        val errorMessage: String,
        val errorCode: Int? = null
    ) : StudentLessonViewEvent()

    /**
     * Media loaded successfully
     * @param duration Total media duration
     */
    data class OnMediaLoaded(
        val duration: Long
    ) : StudentLessonViewEvent()

    /**
     * Media playback completed (reached end)
     */
    object OnMediaCompleted : StudentLessonViewEvent()

    /**
     * User manually adjusted playback speed
     * @param speed Playback speed (e.g., 1.0, 1.5, 2.0)
     */
    data class OnPlaybackSpeedChanged(
        val speed: Float
    ) : StudentLessonViewEvent()

    // ========== CONTENT INTERACTION ==========

    /**
     * User started reading text content
     * Used to estimate reading time
     */
    object OnTextContentStarted : StudentLessonViewEvent()

    /**
     * User finished reading text content
     */
    object OnTextContentFinished : StudentLessonViewEvent()

    /**
     * User opened a document/PDF
     */
    object OnDocumentOpened : StudentLessonViewEvent()

    /**
     * User scrolled in document (for PDF tracking)
     * @param progress Scroll progress (0.0 to 1.0)
     */
    data class OnDocumentScroll(
        val progress: Float
    ) : StudentLessonViewEvent()

    /**
     * User clicked on an embedded link
     * @param url The clicked URL
     */
    data class OnLinkClicked(
        val url: String
    ) : StudentLessonViewEvent()

    /**
     * User downloaded an attachment
     * @param fileName Name of downloaded file
     */
    data class OnAttachmentDownloaded(
        val fileName: String
    ) : StudentLessonViewEvent()

    // ========== QUIZ/EXERCISE INTERACTION ==========

    /**
     * User started a quiz/exercise
     * @param quizId ID of the quiz
     */
    data class OnQuizStarted(
        val quizId: String
    ) : StudentLessonViewEvent()

    /**
     * User submitted quiz answer
     * @param quizId ID of the quiz
     * @param questionId ID of the question
     * @param answerId Selected answer ID
     */
    data class OnQuizAnswered(
        val quizId: String,
        val questionId: String,
        val answerId: String
    ) : StudentLessonViewEvent()

    /**
     * User completed a quiz
     * @param quizId ID of the quiz
     * @param score Achieved score
     */
    data class OnQuizCompleted(
        val quizId: String,
        val score: Float
    ) : StudentLessonViewEvent()

    // ========== NOTES & BOOKMARKS ==========

    /**
     * User added a note to current content
     * @param note Note text
     */
    data class AddNote(
        val note: String
    ) : StudentLessonViewEvent()

    /**
     * User edited existing note
     * @param noteId ID of the note
     * @param newNote Updated note text
     */
    data class EditNote(
        val noteId: String,
        val newNote: String
    ) : StudentLessonViewEvent()

    /**
     * User deleted a note
     * @param noteId ID of the note to delete
     */
    data class DeleteNote(
        val noteId: String
    ) : StudentLessonViewEvent()

    /**
     * User bookmarked current content
     */
    object ToggleBookmark : StudentLessonViewEvent()

    /**
     * Show notes panel
     */
    object ShowNotesPanel : StudentLessonViewEvent()

    /**
     * Hide notes panel
     */
    object HideNotesPanel : StudentLessonViewEvent()

    // ========== UI STATE MANAGEMENT ==========

    /**
     * Toggle fullscreen mode
     */
    object ToggleFullscreen : StudentLessonViewEvent()

    /**
     * Show lesson info/overview
     */
    object ShowLessonInfo : StudentLessonViewEvent()

    /**
     * Hide lesson info
     */
    object DismissLessonInfo : StudentLessonViewEvent()

    /**
     * Show content list sidebar
     */
    object ShowContentList : StudentLessonViewEvent()

    /**
     * Hide content list sidebar
     */
    object HideContentList : StudentLessonViewEvent()

    /**
     * Toggle dark mode for reading
     */
    object ToggleDarkMode : StudentLessonViewEvent()

    /**
     * Adjust text size for reading
     * @param size Text size multiplier (0.8 to 2.0)
     */
    data class AdjustTextSize(
        val size: Float
    ) : StudentLessonViewEvent()

    // ========== ERROR HANDLING ==========

    /**
     * Retry failed operation
     */
    object RetryFailedOperation : StudentLessonViewEvent()

    /**
     * Clear error state
     */
    object ClearError : StudentLessonViewEvent()

    /**
     * Report issue to instructor
     * @param issueDescription Description of the issue
     */
    data class ReportIssue(
        val issueDescription: String
    ) : StudentLessonViewEvent()

    // ========== ACCESSIBILITY ==========

    /**
     * Enable/disable text-to-speech
     */
    object ToggleTextToSpeech : StudentLessonViewEvent()

    /**
     * Adjust playback speed for TTS
     * @param speed Speech speed (0.5 to 2.0)
     */
    data class AdjustTTSSpeed(
        val speed: Float
    ) : StudentLessonViewEvent()

    /**
     * Enable/disable captions for video
     */
    object ToggleCaptions : StudentLessonViewEvent()

    // ========== ANALYTICS & LOGGING ==========

    /**
     * Log custom analytics event
     * @param eventName Event identifier
     * @param parameters Additional event data
     */
    data class LogAnalyticsEvent(
        val eventName: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : StudentLessonViewEvent()

    /**
     * Track time spent on current content
     * Automatically called by SessionTimer
     */
    object TrackTimeSpent : StudentLessonViewEvent()

    // ========== SESSION MANAGEMENT ==========

    /**
     * Pause learning session
     * Stops timers and saves current progress
     */
    object PauseSession : StudentLessonViewEvent()

    /**
     * Resume learning session
     * Restarts timers and inactivity detection
     */
    object ResumeSession : StudentLessonViewEvent()

    /**
     * End learning session normally
     * Saves progress and cleans up resources
     */
    object EndSession : StudentLessonViewEvent()

    /**
     * Force exit (system back button, etc.)
     * Shows confirmation dialog if unsaved changes
     */
    object ForceExit : StudentLessonViewEvent()
}