package com.example.datn.domain.models

import java.time.Instant

data class Parent(
    val id: String,
    val userId: String,
    val createdAt: Instant,
    val updatedAt: Instant
)