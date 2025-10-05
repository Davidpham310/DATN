package com.example.datn.core.di

import android.content.Context
import androidx.room.Room
import com.example.datn.core.network.FirebaseAuthDataSource
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.data.local.AppDatabase
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.repository.impl.AuthRepositoryImpl
import com.example.datn.data.repository.impl.SplashRepositoryImpl
import com.example.datn.domain.repository.IAuthRepository
import com.example.datn.domain.repository.ISplashRepository
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

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideMinioClient(): MinioClient = MinioClient.builder()
        .endpoint("your-minio-endpoint") // e.g., "http://localhost:9000"
        .credentials("accessKey", "secretKey")
        .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app_db")
            .build()


    @Provides
    @Singleton
    fun provideFirebaseAuthDataSource(firebaseAuth: FirebaseAuth , firestore: FirebaseFirestore): FirebaseAuthDataSource =
        FirebaseAuthDataSource(firebaseAuth , firestore)

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthDataSource: FirebaseAuthDataSource,
        userDao: UserDao
    ): IAuthRepository = AuthRepositoryImpl(firebaseAuthDataSource, userDao)

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideNotificationManager(): NotificationManager = NotificationManager()

    @Provides
    @Singleton
    fun provideSplashRepository(
        userDao: UserDao,
    ): ISplashRepository = SplashRepositoryImpl(userDao)
}