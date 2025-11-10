package com.example.datn.data.mapper

import com.example.datn.data.local.entities.toEntity

/**
 * Convenience imports cho Sync System
 * 
 * File này re-export các mappers đã có sẵn để dễ sử dụng trong FirebaseRoomSyncManager.
 * Các mapper thực tế được định nghĩa trong các file riêng lẻ:
 * - TestMapper.kt
 * - TestQuestionMapper.kt
 * - TestOptionMapper.kt
 * - MiniGameMapper.kt
 * - MiniGameQuestionMapper.kt
 * - MiniGameOptionMapper.kt
 * - StudentTestResultMapper.kt
 * - StudentTestAnswerEntity.kt (inline mappers)
 * 
 * Usage:
 * ```kotlin
 * import com.example.datn.data.mapper.*
 * 
 * val entity = test.toEntity()
 * val domain = entity.toDomain()
 * ```
 */

// NOTE: Tất cả mappers đã được định nghĩa trong các file riêng:
// - Test.toEntity() / TestEntity.toDomain() → TestMapper.kt
// - TestQuestion.toEntity() / TestQuestionEntity.toDomain() → TestQuestionMapper.kt
// - TestOption.toEntity() / TestOptionEntity.toDomain() → TestOptionMapper.kt
// - MiniGame.toEntity() / MiniGameEntity.toDomain() → MiniGameMapper.kt
// - MiniGameQuestion.toEntity() / MiniGameQuestionEntity.toDomain() → MiniGameQuestionMapper.kt
// - MiniGameOption.toEntity() / MiniGameOptionEntity.toDomain() → MiniGameOptionMapper.kt
// - StudentTestResult.toEntity() / StudentTestResultEntity.toDomain() → StudentTestResultMapper.kt
// - StudentTestAnswer.toEntity() / StudentTestAnswerEntity.toDomainModel() → StudentTestAnswerEntity.kt

// ==================== BATCH EXTENSION HELPERS ====================

/**
 * Batch convert list of Test entities to domain models
 */
fun List<com.example.datn.data.local.entities.TestEntity>.toTestsDomain(): List<com.example.datn.domain.models.Test> = 
    map { it.toDomain() }

/**
 * Batch convert list of Test domain models to entities
 */
fun List<com.example.datn.domain.models.Test>.toTestEntities(): List<com.example.datn.data.local.entities.TestEntity> = 
    map { it.toEntity() }

/**
 * Batch convert list of TestQuestion entities to domain models
 */
fun List<com.example.datn.data.local.entities.TestQuestionEntity>.toQuestionsDomain(): List<com.example.datn.domain.models.TestQuestion> = 
    map { it.toDomain() }

/**
 * Batch convert list of TestQuestion domain models to entities
 */
fun List<com.example.datn.domain.models.TestQuestion>.toQuestionEntities(): List<com.example.datn.data.local.entities.TestQuestionEntity> = 
    map { it.toEntity() }

/**
 * Batch convert list of TestOption entities to domain models
 */
fun List<com.example.datn.data.local.entities.TestOptionEntity>.toOptionsDomain(): List<com.example.datn.domain.models.TestOption> = 
    map { it.toDomain() }

/**
 * Batch convert list of TestOption domain models to entities
 */
fun List<com.example.datn.domain.models.TestOption>.toOptionEntities(): List<com.example.datn.data.local.entities.TestOptionEntity> = 
    map { it.toEntity() }

/**
 * Batch convert list of MiniGame entities to domain models
 */
fun List<com.example.datn.data.local.entities.MiniGameEntity>.toGamesDomain(): List<com.example.datn.domain.models.MiniGame> = 
    map { it.toDomain() }

/**
 * Batch convert list of MiniGame domain models to entities
 */
fun List<com.example.datn.domain.models.MiniGame>.toGameEntities(): List<com.example.datn.data.local.entities.MiniGameEntity> = 
    map { it.toEntity() }

/**
 * Batch convert list of MiniGameQuestion entities to domain models
 */
fun List<com.example.datn.data.local.entities.MiniGameQuestionEntity>.toGameQuestionsDomain(): List<com.example.datn.domain.models.MiniGameQuestion> = 
    map { it.toDomain() }

/**
 * Batch convert list of MiniGameQuestion domain models to entities
 */
fun List<com.example.datn.domain.models.MiniGameQuestion>.toGameQuestionEntities(): List<com.example.datn.data.local.entities.MiniGameQuestionEntity> = 
    map { it.toEntity() }

/**
 * Batch convert list of MiniGameOption entities to domain models
 */
fun List<com.example.datn.data.local.entities.MiniGameOptionEntity>.toGameOptionsDomain(): List<com.example.datn.domain.models.MiniGameOption> = 
    map { it.toDomain() }

/**
 * Batch convert list of MiniGameOption domain models to entities
 */
fun List<com.example.datn.domain.models.MiniGameOption>.toGameOptionEntities(): List<com.example.datn.data.local.entities.MiniGameOptionEntity> = 
    map { it.toEntity() }

/**
 * Batch convert list of StudentTestAnswer entities to domain models
 */
fun List<com.example.datn.data.local.entities.StudentTestAnswerEntity>.toAnswersDomain(): List<com.example.datn.domain.models.StudentTestAnswer> = 
    map { it.toDomainModel() }

/**
 * Batch convert list of StudentTestAnswer domain models to entities
 */
fun List<com.example.datn.domain.models.StudentTestAnswer>.toAnswerEntities(): List<com.example.datn.data.local.entities.StudentTestAnswerEntity> = 
    map { it.toEntity() }
