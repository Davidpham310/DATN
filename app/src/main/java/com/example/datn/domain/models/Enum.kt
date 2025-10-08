package com.example.datn.domain.models

enum class UserRole(val displayName: String) {
    TEACHER("Giáo viên"),
    PARENT("Phụ huynh"),
    STUDENT("Học sinh");

    companion object {
        fun fromDisplayName(displayName: String): UserRole? {
            return values().find { it.displayName == displayName }
        }
        fun fromString(role: String): UserRole? {
            return values().find { it.name.equals(role, ignoreCase = true) }
        }
    }
}

enum class RelationshipType(val displayName: String) {
    FATHER("Bố"),
    MOTHER("Mẹ"),
    GRANDPARENT("Ông/Bà"),
    GUARDIAN("Người giám hộ khác");

    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}

enum class Level(val displayName: String) {
    EASY("Dễ"),
    MEDIUM("Trung bình"),
    HARD("Khó");
    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return RelationshipType.values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return RelationshipType.values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}

enum class QuestionType(val displayName: String) {
    SINGLE_CHOICE("Trắc nghiệm đơn"),
    MULTIPLE_CHOICE("Trắc nghiệm đa"),
    FILL_BLANK("Điền vào chỗ trống"),
    ESSAY("Tự luận");
    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return RelationshipType.values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return RelationshipType.values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}

enum class EnrollmentStatus(val displayName: String) {
    ENROLLED("Đã tham gia"),
    NOT_ENROLLED("Chưa tham gia");
    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return RelationshipType.values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return RelationshipType.values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}
enum class ContentType(val displayName: String) {
    TEXT("Văn bản"),
    VIDEO("Video"),
    AUDIO("Âm thanh"),
    IMAGE("Hình ảnh"),
    PDF("Tài liệu PDF"),
    MINIGAME("Trò chơi nhỏ");
    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return RelationshipType.values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return RelationshipType.values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}
enum class GameType(val displayName: String) {
    QUIZ("Trắc nghiệm"),
    PUZZLE("Đoán chữ"),
    MATCHING("Ghép cặp");
    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return RelationshipType.values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return RelationshipType.values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}
enum class GameStatus(val displayName: String) {
    COMPLETED("Hoàn thành"),
    IN_PROGRESS("Đang thực hiện"),
    ABANDONED("Bỏ dở"),
    FAILED("Thất bại"),
    TIMEOUT("Hết giờ");
    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return RelationshipType.values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return RelationshipType.values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}
enum class TestStatus(val displayName: String) {
    SUBMITTED("Đã nộp bài"),
    GRADED("Đã chấm điểm"),
    IN_PROGRESS("Đang làm bài"),
    OVERDUE("Quá hạn nộp"),
    UNSUBMITTED("Chưa nộp");
    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return RelationshipType.values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return RelationshipType.values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}

enum class NotificationType(val displayName: String) {
    ASSIGNMENT("Bài tập mới"),
    MESSAGE("Tin nhắn mới"),
    SYSTEM_ALERT("Cảnh báo hệ thống"),
    GRADE_UPDATE("Cập nhật điểm"),
    CLASS_UPDATE("Cập nhật lớp học");
    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return RelationshipType.values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return RelationshipType.values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}

enum class ConversationType(val displayName: String) {
    ONE_TO_ONE("Trò chuyện cá nhân"),
    GROUP("Trò chuyện nhóm");
    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return RelationshipType.values().find { it.displayName == displayName }
        }
        fun fromString(type: String): RelationshipType? {
            return RelationshipType.values().find { it.name.equals(type, ignoreCase = true) }
        }
    }
}

enum class ActionType(val displayName: String) {
    CREATE("Tạo"),
    UPDATE("Cập nhật"),
    DELETE("Xóa"),
    VIEW("Xem"),
}
