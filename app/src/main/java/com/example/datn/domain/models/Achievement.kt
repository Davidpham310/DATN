package com.example.datn.domain.models

data class Achievement(
    val id: String,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val condition: String?
)
