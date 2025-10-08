package com.example.datn.data.local.converters

import androidx.room.TypeConverter
import com.example.datn.domain.models.*

class EnumConverter {
    // Chuyển ENUM sang String để lưu trữ

    @TypeConverter
    fun fromUserRole(value: UserRole?): String? = value?.name

    @TypeConverter
    fun toUserRole(value: String?): UserRole? = value?.let { UserRole.valueOf(it) }

    // ... Cần lặp lại cho TẤT CẢ các ENUM bạn đã dùng:
    // ConversationType, TestStatus, QuestionType, GameType, Level, RelationshipType, v.v.

    @TypeConverter
    fun fromConversationType(value: ConversationType?): String? = value?.name

    @TypeConverter
    fun toConversationType(value: String?): ConversationType? = value?.let { ConversationType.valueOf(it) }

    @TypeConverter
    fun fromTestStatus(value: TestStatus?): String? = value?.name

    @TypeConverter
    fun toTestStatus(value: String?): TestStatus? = value?.let { TestStatus.valueOf(it) }

}