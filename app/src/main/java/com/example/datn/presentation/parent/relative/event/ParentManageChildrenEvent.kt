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

    data class OpenUnlinkDialog(val student: LinkedStudentInfo) : ParentManageChildrenEvent()
    object DismissUnlinkDialog : ParentManageChildrenEvent()
    object ConfirmUnlinkStudent : ParentManageChildrenEvent()

    data class UpdateSearchQuery(val query: String) : ParentManageChildrenEvent()
    object SearchStudents : ParentManageChildrenEvent()

    data class OpenLinkDialog(val result: StudentSearchResult) : ParentManageChildrenEvent()
    object DismissLinkDialog : ParentManageChildrenEvent()
    data class ChangeLinkRelationship(val relationship: RelationshipType) : ParentManageChildrenEvent()
    data class ChangeLinkPrimaryGuardian(val isPrimary: Boolean) : ParentManageChildrenEvent()
    object ConfirmLinkStudent : ParentManageChildrenEvent()

    object ClearMessages : ParentManageChildrenEvent()
}
