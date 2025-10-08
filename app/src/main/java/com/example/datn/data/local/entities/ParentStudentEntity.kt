package com.example.datn.data.local.entities

import androidx.room.Entity
import com.example.datn.domain.models.RelationshipType
import java.time.Instant

@Entity(
    tableName = "parent_student",
    primaryKeys = ["parentId", "studentId"]
)
data class ParentStudentEntity(
    val parentId: String,
    val studentId: String,
    val relationship: RelationshipType,
    val linkedAt: Instant,
    val isPrimaryGuardian: Boolean
)