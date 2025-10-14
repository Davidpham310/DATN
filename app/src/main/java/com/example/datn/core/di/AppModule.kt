package com.example.datn.core.di

import android.content.Context
import androidx.room.Room
import com.example.datn.BuildConfig
import com.example.datn.core.network.datasource.FirebaseAuthDataSource
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.classroom.ClassService
import com.example.datn.core.network.service.user.UserService
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.data.local.AppDatabase
import com.example.datn.data.local.dao.ClassDao
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.repository.impl.AuthRepositoryImpl
import com.example.datn.data.repository.impl.ClassRepositoryImpl
import com.example.datn.data.repository.impl.UserRepositoryImpl
import com.example.datn.domain.repository.IAuthRepository
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.repository.IUserRepository
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

    // Firebase data sources
    @Provides
    @Singleton
    fun provideFirebaseAuthDataSource(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): FirebaseAuthDataSource = FirebaseAuthDataSource(firebaseAuth, firestore)

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

    // Services
    @Provides
    @Singleton
    fun provideUserService(): UserService = UserService()


    @Provides
    @Singleton
    fun provideClassService(): ClassService = ClassService()
}
