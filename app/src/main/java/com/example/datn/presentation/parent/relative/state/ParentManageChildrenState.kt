package com.example.datn.presentation.parent.relative.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.RelationshipType
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.domain.usecase.parentstudent.StudentSearchResult

data class ParentManageChildrenState(
    override val isLoading: Boolean = false,
    override val error: String? = null,

    val linkedStudents: List<LinkedStudentInfo> = emptyList(),

    // Search existing students
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val searchResults: List<StudentSearchResult> = emptyList(),

    val selectedSearchResult: StudentSearchResult? = null,
    val showLinkDialog: Boolean = false,
    val relationshipForLink: RelationshipType = RelationshipType.GUARDIAN,
    val isPrimaryGuardianForLink: Boolean = true,

    // Relationship editing
    val selectedStudent: LinkedStudentInfo? = null,
    val showRelationshipDialog: Boolean = false,
    val relationshipForEdit: RelationshipType = RelationshipType.GUARDIAN,
    val isPrimaryGuardianForEdit: Boolean = true,

    val selectedStudentForUnlink: LinkedStudentInfo? = null,
    val showUnlinkDialog: Boolean = false,

    // Action state
    val isProcessingAction: Boolean = false,
    val successMessage: String? = null
) : BaseState
