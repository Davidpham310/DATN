package com.example.datn.data.mapper

import com.example.datn.data.local.entities.ScheduleEntity
import com.example.datn.domain.models.Schedule

fun ScheduleEntity.toDomain(): Schedule {
    return Schedule(
        id = id,
        classId = classId,
        title = title,
        startTime = startTime,
        endTime = endTime,
        type = type,
        createdAt = createdAt
    )
}

fun Schedule.toEntity(): ScheduleEntity {
    return ScheduleEntity(
        id = id,
        classId = classId,
        title = title,
        startTime = startTime,
        endTime = endTime,
        type = type,
        createdAt = createdAt
    )
}
