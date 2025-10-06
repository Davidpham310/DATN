package com.example.datn.data.mapper

import com.example.datn.data.local.entities.TestEntity
import com.example.datn.domain.models.TestPaper

fun TestEntity.toDomain(): TestPaper {
    return TestPaper(
        id = id,
        classId = classId,
        title = title,
        description = description,
        duration = duration,
        startTime = startTime,
        endTime = endTime,
        allowRetry = allowRetry,
        allowViewAnswer = allowViewAnswer
    )
}

fun TestPaper.toEntity(): TestEntity {
    return TestEntity(
        id = id,
        classId = classId,
        title = title,
        description = description,
        duration = duration,
        startTime = startTime,
        endTime = endTime,
        allowRetry = allowRetry,
        allowViewAnswer = allowViewAnswer
    )
}
