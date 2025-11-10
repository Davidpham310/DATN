package com.example.datn.domain.models

import java.time.Instant

/**
 * Represents the synchronization status of data between Firebase and Room
 */
data class SyncStatus(
    val entityType: SyncEntityType,
    val lastSyncTime: Instant,
    val isSyncing: Boolean = false,
    val lastError: String? = null
)

enum class SyncEntityType {
    TESTS,
    TEST_QUESTIONS,
    TEST_OPTIONS,
    MINI_GAMES,
    MINI_GAME_QUESTIONS,
    MINI_GAME_OPTIONS,
    STUDENT_TEST_RESULTS,
    STUDENT_TEST_ANSWERS
}

sealed class SyncResult<out T> {
    data class Success<T>(val data: T) : SyncResult<T>()
    data class Error(val message: String) : SyncResult<Nothing>()
    object Loading : SyncResult<Nothing>()
}
