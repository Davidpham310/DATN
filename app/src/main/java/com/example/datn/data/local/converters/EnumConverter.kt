package com.example.datn.data.local.converters

import androidx.room.TypeConverter
import com.example.datn.domain.models.*

class EnumConverter {
    // Chuyển ENUM sang String để lưu trữ

    @TypeConverter
    fun fromUserRole(value: UserRole?): String? = value?.name

    @TypeConverter
    fun toUserRole(value: String?): UserRole? = value?.let { UserRole.valueOf(it) }

    @TypeConverter
    fun fromConversationType(value: ConversationType?): String? = value?.name

    @TypeConverter
    fun toConversationType(value: String?): ConversationType? = value?.let { ConversationType.valueOf(it) }

    @TypeConverter
    fun fromTestStatus(value: TestStatus?): String? = value?.name

    @TypeConverter
    fun toTestStatus(value: String?): TestStatus? = value?.let { TestStatus.valueOf(it) }

    @TypeConverter
    fun fromRelationshipType(value: RelationshipType?): String? = value?.name

    @TypeConverter
    fun toRelationshipType(value: String?): RelationshipType? =
        value?.let { RelationshipType.valueOf(it) }

    @TypeConverter
    fun fromEnrollmentStatus(value: EnrollmentStatus?): String? = value?.name

    @TypeConverter
    fun toEnrollmentStatus(value: String?): EnrollmentStatus? =
        value?.let { EnrollmentStatus.valueOf(it) }

    @TypeConverter
    fun fromContentType(value: ContentType?): String? = value?.name

    @TypeConverter
    fun toContentType(value: String?): ContentType? =
        value?.let { ContentType.valueOf(it) }

    @TypeConverter
    fun fromLevel(value: Level?): String? = value?.name

    @TypeConverter
    fun toLevel(value: String?): Level? =
        value?.let { Level.valueOf(it) }

    @TypeConverter
    fun fromQuestionType(value: QuestionType?): String? = value?.name

    @TypeConverter
    fun toQuestionType(value: String?): QuestionType? =
        value?.let { QuestionType.valueOf(it) }

    @TypeConverter
    fun fromNotificationType(value: NotificationType?): String? = value?.name

    @TypeConverter
    fun toNotificationType(value: String?): NotificationType? =
        value?.let { NotificationType.valueOf(it) }

    @TypeConverter
    fun fromGameStatus(value: GameStatus?): String? = value?.name

    @TypeConverter
    fun toGameStatus(value: String?): GameStatus? =
        value?.let { GameStatus.valueOf(it) }

    @TypeConverter
    fun fromCompletionStatus(value: CompletionStatus?): String? = value?.name

    @TypeConverter
    fun toCompletionStatus(value: String?): CompletionStatus? =
        value?.let { CompletionStatus.valueOf(it) }

    @TypeConverter
    fun fromActionType(value: ActionType?): String? = value?.name

    @TypeConverter
    fun toActionType(value: String?): ActionType? =
        value?.let { ActionType.valueOf(it) }
}