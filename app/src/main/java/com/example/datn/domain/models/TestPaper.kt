package com.example.datn.domain.models

data class TestPaper(
    val id: String,
    val classId: String,
    val title: String,
    val description: String?,
    val duration: Int?,
    val startTime: Long?,
    val endTime: Long?,
    val allowRetry: Boolean = false,
    val allowViewAnswer: Boolean = false
)
