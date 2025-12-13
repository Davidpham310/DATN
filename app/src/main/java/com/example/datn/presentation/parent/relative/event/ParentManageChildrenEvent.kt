package com.example.datn.presentation.parent.relative.event

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.RelationshipType
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.domain.usecase.parentstudent.StudentSearchResult

sealed class ParentManageChildrenEvent : BaseEvent {
    object LoadLinkedStudents : ParentManageChildrenEvent()

    data class OpenRelationshipDialog(val student: LinkedStudentInfo) : ParentManageChildrenEvent()
    object DismissRelationshipDialog : ParentManageChildrenEvent()
    data class ChangeRelationship(val relationship: RelationshipType) : ParentManageChildrenEvent()
    data class ChangePrimaryGuardian(val isPrimary: Boolean) : ParentManageChildrenEvent()
    object SaveRelationship : ParentManageChildrenEvent()

    data class UnlinkStudent(val student: LinkedStudentInfo) : ParentManageChildrenEvent()

    data class UpdateSearchQuery(val query: String) : ParentManageChildrenEvent()
    object SearchStudents : ParentManageChildrenEvent()
    data class LinkExistingStudent(
        val result: StudentSearchResult,
        val relationship: RelationshipType,
        val isPrimaryGuardian: Boolean
    ) : ParentManageChildrenEvent()

    object ClearMessages : ParentManageChildrenEvent()
}
