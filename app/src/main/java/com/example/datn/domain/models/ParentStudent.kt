package com.example.datn.domain.models

import java.time.Instant

data class ParentStudent(
    val parentId: String,  // Parent's id from parents collection (not userId)
    val studentId: String,  // Student's id from students collection
    val relationship: RelationshipType,
    val linkedAt: Instant,
    val isPrimaryGuardian: Boolean
)
