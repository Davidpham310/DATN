package com.example.datn.core.di

import FileRepositoryImpl
import android.content.Context
import androidx.room.Room
import com.example.datn.BuildConfig
import com.example.datn.core.network.datasource.FirebaseAuthDataSource
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.classroom.ClassService
import com.example.datn.core.network.service.lesson.LessonContentService
import com.example.datn.core.network.service.lesson.LessonService
import com.example.datn.core.network.service.mini_game.MiniGameService
import com.example.datn.core.network.service.test.TestService
import com.example.datn.core.network.service.minio.MinIOService
import com.example.datn.core.network.service.user.UserService
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.data.local.AppDatabase
import com.example.datn.data.local.dao.ClassDao
import com.example.datn.data.local.dao.LessonContentDao
import com.example.datn.data.local.dao.LessonDao
import com.example.datn.data.local.dao.MiniGameDao
import com.example.datn.data.local.dao.MiniGameQuestionDao
import com.example.datn.data.local.dao.MiniGameOptionDao
import com.example.datn.data.local.dao.StudentTestResultDao
import com.example.datn.data.local.dao.TestDao
import com.example.datn.data.local.dao.TestQuestionDao
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.repository.impl.AuthRepositoryImpl
import com.example.datn.data.repository.impl.ClassRepositoryImpl
import com.example.datn.data.repository.impl.LessonContentRepositoryImpl
import com.example.datn.data.repository.impl.LessonRepositoryImpl
import com.example.datn.data.repository.impl.MiniGameRepositoryImpl
import com.example.datn.data.repository.impl.TestOptionRepositoryImpl
import com.example.datn.data.repository.impl.TestRepositoryImpl
import com.example.datn.data.repository.impl.UserRepositoryImpl
import com.example.datn.domain.repository.IAuthRepository
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.repository.IFileRepository
import com.example.datn.domain.repository.ILessonContentRepository
import com.example.datn.domain.repository.ILessonRepository
import com.example.datn.domain.repository.IMiniGameRepository
import com.example.datn.domain.repository.ITestOptionRepository
import com.example.datn.domain.repository.ITestRepository
import com.example.datn.domain.repository.IUserRepository
import com.example.datn.domain.usecase.minio.MinIOUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.minio.MinioClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Firebase
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    // 🧩 MinIO Client
    @Provides
    @Singleton
    fun provideMinioClient(): MinioClient {
        val endpoint = BuildConfig.MINIO_ENDPOINT
        val accessKey = BuildConfig.MINIO_ACCESS_KEY
        val secretKey = BuildConfig.MINIO_SECRET_KEY

        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
    }

    // 🧩 Tên bucket (inject được ở nơi cần)
    @Provides
    @Singleton
    fun provideMinioBucketName(): String = BuildConfig.MINIO_BUCKET


    @Provides
    @Singleton
    fun provideMinioService(
        client: MinioClient,
        bucketName: String
    ): MinIOService = MinIOService(client, bucketName)

    // Local Database
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app_db")
            .fallbackToDestructiveMigration() // Cho phép reset nếu cấu trúc đổi
            .build()


    // Notifications
    @Provides
    @Singleton
    fun provideNotificationManager(): NotificationManager = NotificationManager()

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideClassDao(db: AppDatabase): ClassDao = db.classDao()

    @Provides
    @Singleton
    fun provideLessonDao(db: AppDatabase): LessonDao = db.lessonDao()




    @Provides
    @Singleton
    fun provideLessonContentDao(db: AppDatabase): LessonContentDao = db.lessonContentDao()

    @Provides
    @Singleton
    fun provideMiniGameDao(db: AppDatabase): MiniGameDao = db.miniGameDao()

    @Provides
    @Singleton
    fun provideMiniGameQuestionDao(db: AppDatabase): MiniGameQuestionDao = db.miniGameQuestionDao()

    @Provides
    @Singleton
    fun provideMiniGameOptionDao(db: AppDatabase): MiniGameOptionDao = db.miniGameOptionDao()

    @Provides
    @Singleton
    fun provideTestDao(db: AppDatabase): TestDao = db.testDao()

    @Provides
    @Singleton
    fun provideTestQuestionDao(db: AppDatabase): TestQuestionDao = db.testQuestionDao()

    @Provides
    @Singleton
    fun provideStudentTestResultDao(db: AppDatabase): StudentTestResultDao = db.studentTestResultDao()

    // Firebase data sources
    @Provides
    @Singleton
    fun provideFirebaseAuthDataSource(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): FirebaseAuthDataSource = FirebaseAuthDataSource(firebaseAuth, firestore)

    @Provides
    @Singleton
    fun provideFirebaseDataSource(
        userService: UserService,
        classService: ClassService,
        lessonService: LessonService,
        lessonContentService: LessonContentService,
        miniGameService: MiniGameService,
        testService: TestService
    ): FirebaseDataSource = FirebaseDataSource(
        userService,
        classService,
        lessonService,
        lessonContentService,
        miniGameService,
        testService
    )

    // Repositories
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthDataSource: FirebaseAuthDataSource,
        userDao: UserDao
    ): IAuthRepository = AuthRepositoryImpl(firebaseAuthDataSource, userDao)

    @Provides
    @Singleton
    fun provideUserRepository(
        firebaseDataSource: FirebaseDataSource
    ): IUserRepository = UserRepositoryImpl(firebaseDataSource)


    @Provides
    @Singleton
    fun provideClassRepository(
        firebaseDataSource: FirebaseDataSource,
        classDao: ClassDao
    ): IClassRepository = ClassRepositoryImpl(firebaseDataSource , classDao)

    @Provides
    @Singleton
    fun provideLessonRepository(
        firebaseDataSource: FirebaseDataSource,
        lessonDao: LessonDao
    ): ILessonRepository = LessonRepositoryImpl(firebaseDataSource, lessonDao)

    @Provides
    @Singleton
    fun provideLessonContentRepository(
        firebaseDataSource: FirebaseDataSource,
        lessonContentDao: LessonContentDao,
        minIOUseCase: MinIOUseCase
    ): ILessonContentRepository = LessonContentRepositoryImpl(
        firebaseDataSource,
        lessonContentDao,
        minIOUseCase
    )

    @Provides
    @Singleton
    fun provideMiniGameRepository(
        firebaseDataSource: FirebaseDataSource,
        miniGameDao: MiniGameDao,
        miniGameQuestionDao: MiniGameQuestionDao,
        miniGameOptionDao: MiniGameOptionDao
    ): IMiniGameRepository = MiniGameRepositoryImpl(
        firebaseDataSource,
        miniGameDao,
        miniGameQuestionDao,
        miniGameOptionDao
    )

    @Provides
    @Singleton
    fun provideTestOptionRepository(
        firebaseDataSource: FirebaseDataSource,
        db: AppDatabase
    ): ITestOptionRepository = TestOptionRepositoryImpl(
        firebaseDataSource,
        db.testOptionDao()
    )

    @Provides
    @Singleton
    fun provideTestRepository(
        firebaseDataSource: FirebaseDataSource,
        testDao: TestDao,
        testQuestionDao: TestQuestionDao,
        studentTestResultDao: StudentTestResultDao
    ): ITestRepository = TestRepositoryImpl(
        firebaseDataSource,
        testDao,
        testQuestionDao,
        studentTestResultDao
    )

    @Provides
    @Singleton
    fun provideFileRepository(
        minIOService: MinIOService
    ): IFileRepository = FileRepositoryImpl(minIOService)

    // Services
    @Provides
    @Singleton
    fun provideUserService(): UserService = UserService()


    @Provides
    @Singleton
    fun provideClassService(): ClassService = ClassService()


    @Provides
    @Singleton
    fun provideLessonService(): LessonService = LessonService()

    @Provides
    @Singleton
    fun provideLessonContentService(
        minIOService: MinIOService
    ): LessonContentService = LessonContentService(minIOService)

    @Provides
    @Singleton
    fun provideMiniGameService(): MiniGameService = MiniGameService()

    @Provides
    @Singleton
    fun provideTestService(): TestService = TestService()

}
