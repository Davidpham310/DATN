package com.example.datn.data.mapper

import com.example.datn.data.local.entities.AchievementEntity
import com.example.datn.domain.models.Achievement

fun AchievementEntity.toDomain(): Achievement {
    return Achievement(
        id = id,
        name = name,
        description = description,
        iconUrl = iconUrl,
        condition = condition
    )
}

fun Achievement.toEntity(): AchievementEntity {
    return AchievementEntity(
        id = id,
        name = name,
        description = description,
        iconUrl = iconUrl,
        condition = condition
    )
}
