package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String? = null,
    val url: String? = null,
    val uploadedBy: String? = null,
    val createdAt: Long? = null
)
