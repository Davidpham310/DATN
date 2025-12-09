package com.example.datn.presentation.student.lessons.managers

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt Module để provide CoroutineScope cho managers
 */
@Module
@InstallIn(SingletonComponent::class)
object ManagersModule {
    
    /**
     * Provide CoroutineScope cho managers
     * Sử dụng SupervisorJob để một coroutine lỗi không ảnh hưởng đến các coroutine khác
     */
    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
