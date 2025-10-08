package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.GameType
import com.example.datn.domain.models.Level
import java.time.Instant

@Entity(tableName = "minigame")
data class MiniGameEntity(
    @PrimaryKey
    val id: String,
    val teacherId: String,
    val title: String,
    val description: String,
    val gameType: GameType,
    val contentUrl: String? = null,
    val level: Level,
    val createdAt: Instant,
    val updatedAt: Instant
)