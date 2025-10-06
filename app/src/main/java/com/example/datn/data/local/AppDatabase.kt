package com.example.datn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.datn.data.local.converters.StringListConverter
import com.example.datn.data.local.dao.*
import com.example.datn.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        StudentProfileEntity::class,
        ClassEntity::class,
        ClassMemberEntity::class,
        LessonEntity::class,
        AssignmentEntity::class,
        SubmissionEntity::class,
        TestEntity::class,
        QuestionEntity::class,
        TestResultEntity::class,
        AchievementEntity::class,
        StudentAchievementEntity::class,
        ResourceEntity::class,
        NotificationEntity::class,
        MessageEntity::class,
        ScheduleEntity::class,
        AttendanceEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun classDao(): ClassDao
    abstract fun classMemberDao(): ClassMemberDao
    abstract fun lessonDao(): LessonDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun submissionDao(): SubmissionDao
    abstract fun testDao(): TestDao
    abstract fun questionDao(): QuestionDao
    abstract fun testResultDao(): TestResultDao
    abstract fun achievementDao(): AchievementDao
    abstract fun studentAchievementDao(): StudentAchievementDao
    abstract fun resourceDao(): ResourceDao
    abstract fun notificationDao(): NotificationDao
    abstract fun messageDao(): MessageDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun studentProfileDao(): StudentProfileDao
}
