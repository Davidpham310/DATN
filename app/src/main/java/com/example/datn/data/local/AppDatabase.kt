package com.example.datn.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.datn.data.local.converters.DateTimeConverter
import com.example.datn.data.local.converters.EnumConverter
import com.example.datn.data.local.dao.*
import com.example.datn.data.local.entities.*

/**
 * Cơ sở dữ liệu Room chính của ứng dụng.
 * Khai báo tất cả các Entity và TypeConverter cần thiết.
 */
@Database(
    entities = [
        // 1. Core Users
        UserEntity::class, TeacherEntity::class, ParentEntity::class, StudentEntity::class,

        // 2. Joins & Relationships
        ParentStudentEntity::class, ClassStudentEntity::class, ConversationParticipantEntity::class,

        // 3. Educational Content
        ClassEntity::class, LessonEntity::class, LessonContentEntity::class,

        // 4. Tests & Results
        TestEntity::class,
        TestQuestionEntity::class,
        TestOptionEntity::class,
        StudentTestResultEntity::class,
        StudentTestAnswerEntity::class,

        // 5. Games
        MiniGameEntity::class,
        MiniGameQuestionEntity::class,
        MiniGameOptionEntity::class,
        StudentMiniGameResultEntity::class,
        StudentMiniGameAnswerEntity::class,

        // 6. Progress & Communication
        StudentLessonProgressEntity::class,
        DailyStudyTimeEntity::class,
        NotificationEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 2, // Tăng phiên bản khi thay đổi cấu trúc DB
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class, EnumConverter::class)
abstract class AppDatabase : RoomDatabase() {

    // --- Khai báo các Data Access Object (DAOs) ---

    // Core & Join DAOs
    abstract fun userDao(): UserDao
    abstract fun studentDao(): StudentDao // Giả định bạn có StudentDao
    abstract fun teacherDao(): TeacherDao // Giả định bạn có TeacherDao
    abstract fun parentStudentDao(): ParentStudentDao
    abstract fun classStudentDao(): ClassStudentDao

    // Content DAOs
    abstract fun classDao(): ClassDao
    abstract fun lessonDao(): LessonDao
    abstract fun lessonContentDao(): LessonContentDao

    // Test & Game DAOs
    abstract fun testDao(): TestDao
    abstract fun testQuestionDao(): TestQuestionDao
    abstract fun testOptionDao(): TestOptionDao
    abstract fun studentTestResultDao(): StudentTestResultDao
    abstract fun studentTestAnswerDao(): StudentTestAnswerDao
    abstract fun miniGameDao(): MiniGameDao
    abstract fun miniGameQuestionDao(): MiniGameQuestionDao
    abstract fun miniGameOptionDao(): MiniGameOptionDao
    abstract fun studentMiniGameResultDao(): StudentMiniGameResultDao
    abstract fun studentMiniGameAnswerDao(): StudentMiniGameAnswerDao

    // Progress & Communication DAOs
    abstract fun studentLessonProgressDao(): StudentLessonProgressDao
    abstract fun dailyStudyTimeDao(): DailyStudyTimeDao
    abstract fun notificationDao(): NotificationDao
    abstract fun conversationDao(): ConversationDao
    abstract fun conversationParticipantDao(): ConversationParticipantDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration từ version 1 sang 2:
         * Thêm các trường hint, pairId, pairContent vào bảng minigame_option
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Thêm các cột mới vào bảng minigame_option
                database.execSQL(
                    "ALTER TABLE minigame_option ADD COLUMN hint TEXT DEFAULT NULL"
                )
                database.execSQL(
                    "ALTER TABLE minigame_option ADD COLUMN pairId TEXT DEFAULT NULL"
                )
                database.execSQL(
                    "ALTER TABLE minigame_option ADD COLUMN pairContent TEXT DEFAULT NULL"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
