package com.example.datn.presentation.parent.managechildren

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.RelationshipType

sealed class ParentCreateStudentAccountEvent : BaseEvent {
    data class Submit(
        val name: String,
        val email: String,
        val password: String,
        val gradeLevel: String,
        val dateOfBirthText: String,
        val relationship: RelationshipType,
        val isPrimaryGuardian: Boolean
    ) : ParentCreateStudentAccountEvent()

    object ClearMessages : ParentCreateStudentAccountEvent()
}
