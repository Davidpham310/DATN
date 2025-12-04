package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import com.example.datn.domain.models.ContentType

/**
 * Quản lý theo dõi trạng thái phương tiện (video/audio)
 * Theo dõi xem phương tiện có đang phát hay tạm dừng
 * Phát hiện hành vi gian lận (tua nhanh, xem < 70%, pause lâu)
 */
class MediaTrackingManager {
    companion object {
        private const val TAG = "MediaTrackingManager"
        private const val MIN_WATCH_PERCENTAGE = 70  // 70% để đánh dấu đã xem
        private const val PAUSE_WARNING_THRESHOLD = 30000L  // 30 giây pause
        private const val SEEK_THRESHOLD = 10000L  // Tua > 10 giây = nghi vấn
    }

    private var isMediaPlaying: Boolean = false
    private var currentMediaType: ContentType? = null
    private var mediaDuration: Long = 0L  // Tổng thời lượng (ms)
    private var mediaWatchedTime: Long = 0L  // Thời gian xem (ms)
    private var lastPlayPosition: Long = 0L  // Vị trí phát cuối cùng
    private var pauseStartTime: Long = 0L  // Thời điểm pause
    private var seekCount: Int = 0  // Số lần tua
    private var lastSeekAmount: Long = 0L  // Lần tua cuối

    /**
     * Cập nhật trạng thái phương tiện
     */
    fun setMediaPlaying(isPlaying: Boolean, contentType: ContentType? = null) {
        isMediaPlaying = isPlaying
        currentMediaType = contentType
        
        if (isPlaying) {
            pauseStartTime = 0L  // Reset pause timer khi play
            Log.d(TAG, "Media playing: contentType=$contentType")
        } else {
            pauseStartTime = System.currentTimeMillis()  // Ghi nhận thởi điểm pause
            Log.d(TAG, "Media paused: contentType=$contentType")
        }
    }

    /**
     * Kiểm tra xem phương tiện có đang phát không
     */
    fun isMediaPlaying(): Boolean = isMediaPlaying

    /**
     * Lấy loại nội dung phương tiện hiện tại
     */
    fun getCurrentMediaType(): ContentType? = currentMediaType

    /**
     * Kiểm tra xem loại nội dung có phải là phương tiện không
     */
    fun isMediaContent(contentType: ContentType): Boolean {
        return contentType == ContentType.VIDEO || contentType == ContentType.AUDIO
    }

    /**
     * Cập nhật thông tin video (thời lượng, vị trí phát)
     */
    fun updateMediaInfo(duration: Long, currentPosition: Long) {
        mediaDuration = duration
        mediaWatchedTime = currentPosition
        Log.d(TAG, "Media info updated: duration=${duration}ms, position=${currentPosition}ms")
    }

    /**
     * Ghi nhận sự kiện tua video
     */
    fun recordSeek(fromPosition: Long, toPosition: Long) {
        val seekAmount = kotlin.math.abs(toPosition - fromPosition)
        
        if (seekAmount > SEEK_THRESHOLD) {
            seekCount++
            lastSeekAmount = seekAmount
            Log.w(TAG, "Suspicious seek detected! Amount: ${seekAmount}ms, Count: $seekCount")
        }
        
        lastPlayPosition = toPosition
    }

    /**
     * Kiểm tra xem video pause quá lâu không
     */
    fun isPausedTooLong(): Boolean {
        if (pauseStartTime == 0L) return false
        
        val pauseDuration = System.currentTimeMillis() - pauseStartTime
        val isPausedLong = pauseDuration > PAUSE_WARNING_THRESHOLD
        
        if (isPausedLong) {
            Log.w(TAG, "Video paused too long: ${pauseDuration}ms")
        }
        
        return isPausedLong
    }

    /**
     * Lấy thởi gian pause hiện tại (ms)
     */
    fun getPauseDuration(): Long {
        if (pauseStartTime == 0L) return 0L
        return System.currentTimeMillis() - pauseStartTime
    }

    /**
     * Kiểm tra xem đã xem đủ phần trăm video không
     */
    fun hasWatchedEnough(): Boolean {
        if (mediaDuration <= 0) return false
        
        val watchPercentage = (mediaWatchedTime * 100) / mediaDuration
        val isEnough = watchPercentage >= MIN_WATCH_PERCENTAGE
        
        Log.d(TAG, "Watch percentage: $watchPercentage% (need $MIN_WATCH_PERCENTAGE%)")
        
        return isEnough
    }

    /**
     * Lấy phần trăm video đã xem
     */
    fun getWatchPercentage(): Int {
        if (mediaDuration <= 0) return 0
        return ((mediaWatchedTime * 100) / mediaDuration).toInt()
    }

    /**
     * Kiểm tra hành vi gian lận
     */
    fun isSuspiciousBehavior(): Boolean {
        val hasExcessiveSeeks = seekCount > 5  // Tua > 5 lần
        val hasPausedTooLong = isPausedTooLong()
        val hasNotWatchedEnough = !hasWatchedEnough()
        
        val isSuspicious = hasExcessiveSeeks || (hasPausedTooLong && !isMediaPlaying)
        
        if (isSuspicious) {
            Log.w(TAG, "Suspicious behavior detected! Seeks: $seekCount, Paused: $hasPausedTooLong, Watched: ${getWatchPercentage()}%")
        }
        
        return isSuspicious
    }

    /**
     * Lấy chi tiết hành vi gian lận
     */
    fun getSuspiciousDetails(): String {
        return buildString {
            append("Seeks: $seekCount, ")
            append("Paused: ${getPauseDuration()}ms, ")
            append("Watched: ${getWatchPercentage()}%")
        }
    }

    /**
     * Reset trạng thái phương tiện
     */
    fun resetMediaState() {
        isMediaPlaying = false
        currentMediaType = null
        mediaDuration = 0L
        mediaWatchedTime = 0L
        lastPlayPosition = 0L
        pauseStartTime = 0L
        seekCount = 0
        lastSeekAmount = 0L
        Log.d(TAG, "Media state reset")
    }
}