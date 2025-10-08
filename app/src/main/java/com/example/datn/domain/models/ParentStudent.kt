package com.example.datn.domain.models

import java.time.Instant

data class ParentStudent(
    val parentId: String,
    val studentId: String,
    val relationship: RelationshipType,
    val linkedAt: Instant,
    val isPrimaryGuardian: Boolean
)
