package com.example.datn.data.mapper

import com.example.datn.data.local.entities.StudentAchievementEntity
import com.example.datn.domain.models.StudentAchievement

fun StudentAchievementEntity.toDomain(): StudentAchievement {
    return StudentAchievement(
        id = id,
        studentId = studentId,
        achievementId = achievementId,
        earnedAt = earnedAt
    )
}

fun StudentAchievement.toEntity(): StudentAchievementEntity {
    return StudentAchievementEntity(
        id = id,
        studentId = studentId,
        achievementId = achievementId,
        earnedAt = earnedAt
    )
}
