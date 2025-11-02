package com.example.datn.domain.usecase.notification

/**
 * Enum định nghĩa các loại đối tượng tham chiếu trong notification
 */
enum class ReferenceObjectType(val displayName: String, val value: String) {
    NONE("Không có", "NONE"),
    CLASS("Lớp học", "CLASS"),
    LESSON("Bài học", "LESSON"),
    LESSON_CONTENT("Nội dung bài học", "LESSON_CONTENT"),
    TEST("Bài kiểm tra", "TEST"),
    MINI_GAME("Trò chơi", "MINI_GAME"),
    MESSAGE("Tin nhắn", "MESSAGE");
    
    companion object {
        fun fromValue(value: String): ReferenceObjectType? {
            return values().find { it.value == value }
        }
        
        fun fromDisplayName(name: String): ReferenceObjectType? {
            return values().find { it.displayName == name }
        }
    }
}

/**
 * Data class đại diện cho một reference object có thể chọn
 */
data class ReferenceObject(
    val id: String,
    val name: String,
    val type: ReferenceObjectType
)
