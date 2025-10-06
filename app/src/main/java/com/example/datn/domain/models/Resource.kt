package com.example.datn.domain.models

data class Resource(
    val id: String,
    val title: String,
    val subject: String? = null,
    val url: String? = null,
    val uploadedBy: String? = null,
    val createdAt: Long? = null
)
