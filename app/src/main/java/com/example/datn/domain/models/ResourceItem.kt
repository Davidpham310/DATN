package com.example.datn.domain.models

data class ResourceItem(
    val id: String,
    val title: String,
    val subject: String?,
    val url: String?,
    val uploadedBy: String?,
    val createdAt: Long?
)
