package com.example.datn.presentation.student.lessons.managers

import android.util.Log
import com.example.datn.domain.models.LessonContent
import javax.inject.Inject

/**
 * Quản lý điều khiển điều hướng giữa các nội dung
 * 
 * Trách nhiệm:
 * - Kiểm tra xem có thể chuyển sang nội dung tiếp theo không
 * - Đảm bảo nội dung hiện tại đã hoàn thành
 * - Cho phép quay lại nội dung trước đó
 * - Cung cấp thông tin về trạng thái điều hướng
 */
class NavigationControlManager @Inject constructor(
    private val completionRulesManager: CompletionRulesManager
) {
    
    private val TAG = "NavigationControlManager"
    
    /**
     * Kiểm tra xem có thể chuyển sang nội dung khác không
     * 
     * @param lessonContents Danh sách nội dung của bài học
     * @param currentIndex Index của nội dung hiện tại
     * @param selectedIndex Index của nội dung muốn chuyển đến
     * @return Pair<Boolean, String> - (có thể chuyển, lý do nếu không thể)
     */
    fun canNavigateTo(
        lessonContents: List<LessonContent>,
        currentIndex: Int,
        selectedIndex: Int
    ): Pair<Boolean, String> {
        // Kiểm tra index hợp lệ
        if (selectedIndex < 0 || selectedIndex >= lessonContents.size) {
            Log.w(TAG, "⚠️ Invalid index: $selectedIndex")
            return Pair(false, "Nội dung không tồn tại")
        }
        
        // Cho phép quay lại nội dung trước đó
        if (selectedIndex < currentIndex) {
            Log.d(TAG, "✅ Can navigate backward to index: $selectedIndex")
            return Pair(true, "")
        }
        
        // Nếu chuyển tiến, kiểm tra nội dung hiện tại đã hoàn thành chưa
        if (selectedIndex > currentIndex) {
            val currentContent = lessonContents.getOrNull(currentIndex)
            if (currentContent == null) {
                Log.w(TAG, "⚠️ Current content not found at index: $currentIndex")
                return Pair(false, "Nội dung hiện tại không tồn tại")
            }
            
            val isCurrentContentCompleted = completionRulesManager.isContentCompleted(currentContent.id)
            
            if (!isCurrentContentCompleted) {
                Log.w(TAG, "⚠️ Cannot move forward: Current content not completed")
                Log.w(TAG, "   - Current content: ${currentContent.title}")
                Log.w(TAG, "   - Content ID: ${currentContent.id}")
                Log.w(TAG, "   - Completion rule: ${completionRulesManager.getCompletionRule(currentContent.contentType.name)}")
                
                return Pair(
                    false,
                    "Vui lòng hoàn thành nội dung '${currentContent.title}' trước khi chuyển sang nội dung tiếp theo"
                )
            }
            
            Log.d(TAG, "✅ Can navigate forward to index: $selectedIndex")
            return Pair(true, "")
        }
        
        // Nếu chỉ số bằng nhau, không cần chuyển
        Log.d(TAG, "ℹ️ Already at index: $selectedIndex")
        return Pair(true, "")
    }
    
    /**
     * Kiểm tra xem nút "Tiếp" có thể được bấm không
     */
    fun canMoveToNextContent(
        lessonContents: List<LessonContent>,
        currentIndex: Int
    ): Boolean {
        // Kiểm tra có nội dung tiếp theo không
        if (currentIndex >= lessonContents.size - 1) {
            Log.d(TAG, "ℹ️ Already at last content (index: $currentIndex / ${lessonContents.size - 1})")
            return false
        }
        
        // Kiểm tra nội dung hiện tại đã hoàn thành chưa
        val currentContent = lessonContents.getOrNull(currentIndex)
        if (currentContent == null) {
            Log.w(TAG, "⚠️ Current content not found at index: $currentIndex")
            return false
        }
        
        val isCompleted = completionRulesManager.isContentCompleted(currentContent.id)
        val progress = completionRulesManager.getContentProgress(currentContent.id)
        Log.d(TAG, "▶️ canMoveToNextContent() - Next button enabled: $isCompleted")
        Log.d(TAG, "   - Content: ${currentContent.title} (${currentContent.contentType})")
        Log.d(TAG, "   - Progress: $progress%")
        Log.d(TAG, "   - Rule: ${completionRulesManager.getCompletionRule(currentContent.contentType.name)}")
        
        return isCompleted
    }
    
    /**
     * Kiểm tra xem nút "Trước" có thể được bấm không
     */
    fun canMoveToPreviousContent(currentIndex: Int): Boolean {
        val canMove = currentIndex > 0
        Log.d(TAG, "◀️ canMoveToPreviousContent() - Previous button enabled: $canMove (index: $currentIndex)")
        return canMove
    }
    
    /**
     * Lấy thông báo lỗi khi không thể chuyển nước
     */
    fun getNavigationErrorMessage(
        lessonContents: List<LessonContent>,
        currentIndex: Int
    ): String {
        val currentContent = lessonContents.getOrNull(currentIndex)
        if (currentContent == null) {
            return "Nội dung không tồn tại"
        }
        
        val isCompleted = completionRulesManager.isContentCompleted(currentContent.id)
        if (!isCompleted) {
            val rule = completionRulesManager.getCompletionRule(currentContent.contentType.name)
            return "Hoàn thành nội dung: $rule"
        }
        
        return ""
    }
    
    /**
     * Lấy thông tin chi tiết về trạng thái nội dung
     */
    fun getContentStatus(contentId: String, contentTitle: String, contentType: String): String {
        val isCompleted = completionRulesManager.isContentCompleted(contentId)
        val progress = completionRulesManager.getContentProgress(contentId)
        val rule = completionRulesManager.getCompletionRule(contentType)
        
        Log.d(TAG, "📊 getContentStatus() - $contentTitle")
        Log.d(TAG, "   - Type: $contentType")
        Log.d(TAG, "   - Progress: $progress%")
        Log.d(TAG, "   - Completed: $isCompleted")
        Log.d(TAG, "   - Rule: $rule")
        
        return buildString {
            append("📌 Nội dung: $contentTitle\n")
            append("📝 Loại: $contentType\n")
            append("📊 Tiến độ: $progress%\n")
            append("✅ Hoàn thành: ${if (isCompleted) "Có" else "Không"}\n")
            append("📋 Yêu cầu: $rule")
        }
    }
}
