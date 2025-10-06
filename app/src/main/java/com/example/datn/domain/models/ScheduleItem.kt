package com.example.datn.domain.models

data class ScheduleItem(
    val id: String,
    val classId: String?,
    val title: String,
    val startTime: Long?,
    val endTime: Long?,
    val type: String?,
    val createdAt: Long?
)
