package com.example.datn.presentation.parent.relative.viewmodel

import android.util.Log
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.RelationshipType
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.CheckStudentLinkedToParentParams
import com.example.datn.domain.usecase.parentstudent.CheckStudentHasOtherPrimaryGuardianParams
import com.example.datn.domain.usecase.parentstudent.LinkParentToStudentParams
import com.example.datn.domain.usecase.parentstudent.ParentStudentUseCases
import com.example.datn.domain.usecase.parentstudent.UnlinkStudentParams
import com.example.datn.domain.usecase.parentstudent.UpdateRelationshipParams
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.parent.relative.event.ParentManageChildrenEvent
import com.example.datn.presentation.parent.relative.state.ParentManageChildrenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@HiltViewModel
class ParentManageChildrenViewModel @Inject constructor(
    private val parentStudentUseCases: ParentStudentUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ParentManageChildrenState, ParentManageChildrenEvent>(
    ParentManageChildrenState(),
    notificationManager
) {

    companion object {
        private const val TAG = "ParentManageChildrenVM"
    }

    private suspend fun resolveParentId(): String? {
        var resolvedParentId: String? = null

        authUseCases.getCurrentIdUser.invoke().collect { id ->
            if (id.isNotBlank()) {
                resolvedParentId = id
            }
        }

        return resolvedParentId
    }

    init {
        Log.d(TAG, "init -> LoadLinkedStudents")
        onEvent(ParentManageChildrenEvent.LoadLinkedStudents)
    }

    override fun onEvent(event: ParentManageChildrenEvent) {
        Log.d(TAG, "onEvent: $event")
        when (event) {
            is ParentManageChildrenEvent.LoadLinkedStudents -> loadLinkedStudents()
            is ParentManageChildrenEvent.OpenRelationshipDialog -> openRelationshipDialog(event.student)
            is ParentManageChildrenEvent.DismissRelationshipDialog -> dismissRelationshipDialog()
            is ParentManageChildrenEvent.ChangeRelationship -> setState {
                copy(relationshipForEdit = event.relationship)
            }
            is ParentManageChildrenEvent.ChangePrimaryGuardian -> setState {
                copy(isPrimaryGuardianForEdit = event.isPrimary)
            }
            is ParentManageChildrenEvent.SaveRelationship -> saveRelationship()
            is ParentManageChildrenEvent.UnlinkStudent -> openUnlinkDialog(event.student)

            is ParentManageChildrenEvent.OpenUnlinkDialog -> openUnlinkDialog(event.student)
            ParentManageChildrenEvent.DismissUnlinkDialog -> dismissUnlinkDialog()
            ParentManageChildrenEvent.ConfirmUnlinkStudent -> confirmUnlinkStudent()
            is ParentManageChildrenEvent.UpdateSearchQuery -> setState {
                copy(
                    searchQuery = event.query,
                    hasSearched = false,
                    searchResults = emptyList(),
                    selectedSearchResult = null,
                    showLinkDialog = false
                )
            }
            is ParentManageChildrenEvent.SearchStudents -> searchStudents()
            is ParentManageChildrenEvent.OpenLinkDialog -> openLinkDialog(event.result)
            is ParentManageChildrenEvent.DismissLinkDialog -> dismissLinkDialog()
            is ParentManageChildrenEvent.ChangeLinkRelationship -> setState { copy(relationshipForLink = event.relationship) }
            is ParentManageChildrenEvent.ChangeLinkPrimaryGuardian -> setState { copy(isPrimaryGuardianForLink = event.isPrimary) }
            is ParentManageChildrenEvent.ConfirmLinkStudent -> confirmLinkStudent()
            is ParentManageChildrenEvent.ClearMessages -> setState { copy(successMessage = null, error = null) }
        }
    }

    private fun openUnlinkDialog(student: com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo) {
        setState { copy(selectedStudentForUnlink = student, showUnlinkDialog = true) }
    }

    private fun dismissUnlinkDialog() {
        setState { copy(selectedStudentForUnlink = null, showUnlinkDialog = false) }
    }

    private fun confirmUnlinkStudent() {
        val student = state.value.selectedStudentForUnlink ?: return
        dismissUnlinkDialog()
        unlinkStudent(student)
    }

    private fun openLinkDialog(result: com.example.datn.domain.usecase.parentstudent.StudentSearchResult) {
        Log.d(TAG, "openLinkDialog() studentId=${result.student.id}")
        setState {
            copy(
                selectedSearchResult = result,
                showLinkDialog = true,
                relationshipForLink = RelationshipType.GUARDIAN,
                isPrimaryGuardianForLink = true
            )
        }
    }

    private fun dismissLinkDialog() {
        Log.d(TAG, "dismissLinkDialog()")
        setState { copy(selectedSearchResult = null, showLinkDialog = false) }
    }

    private fun confirmLinkStudent() {
        val current = state.value
        val selected = current.selectedSearchResult ?: return

        launch {
            val parentId = resolveParentId()
            if (parentId.isNullOrBlank()) {
                showNotification("Không tìm thấy tài khoản phụ huynh", NotificationType.ERROR)
                return@launch
            }

            // E1: already linked
            var alreadyLinked = false
            parentStudentUseCases.checkStudentLinkedToParent(
                CheckStudentLinkedToParentParams(parentId = parentId, studentId = selected.student.id)
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> setState { copy(isProcessingAction = true, error = null) }
                    is Resource.Success -> alreadyLinked = resource.data == true
                    is Resource.Error -> {
                        setState { copy(isProcessingAction = false, error = resource.message) }
                        showNotification(resource.message ?: "Không thể kiểm tra liên kết", NotificationType.ERROR)
                    }
                }
            }

            if (alreadyLinked) {
                val message = "Học sinh đã được liên kết với phụ huynh này"
                setState { copy(isProcessingAction = false, error = message) }
                showNotification(message, NotificationType.ERROR)
                return@launch
            }

            // E2: only one primary guardian
            if (current.isPrimaryGuardianForLink) {
                var hasPrimaryGuardian = false
                parentStudentUseCases.checkStudentHasPrimaryGuardian(selected.student.id).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> setState { copy(isProcessingAction = true, error = null) }
                        is Resource.Success -> hasPrimaryGuardian = resource.data == true
                        is Resource.Error -> {
                            setState { copy(isProcessingAction = false, error = resource.message) }
                            showNotification(resource.message ?: "Không thể kiểm tra người giám hộ", NotificationType.ERROR)
                        }
                    }
                }

                if (hasPrimaryGuardian) {
                    val message = "Học sinh chỉ có một người giám hộ chính"
                    setState { copy(isProcessingAction = false, error = message) }
                    showNotification(message, NotificationType.ERROR)
                    return@launch
                }
            }

            // Link
            parentStudentUseCases.linkParentToStudent(
                LinkParentToStudentParams(
                    studentId = selected.student.id,
                    parentId = parentId,
                    relationship = current.relationshipForLink.name,
                    isPrimaryGuardian = current.isPrimaryGuardianForLink
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> setState { copy(isProcessingAction = true, error = null) }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isProcessingAction = false,
                                showLinkDialog = false,
                                selectedSearchResult = null,
                                successMessage = "Liên kết học sinh thành công"
                            )
                        }
                        loadLinkedStudents()
                    }
                    is Resource.Error -> {
                        setState { copy(isProcessingAction = false, error = resource.message) }
                        showNotification(resource.message ?: "Không thể liên kết học sinh", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun loadLinkedStudents() {
        Log.d(TAG, "loadLinkedStudents() called")
        launch {
            authUseCases.getCurrentIdUser.invoke()
                .filter { it.isNotBlank() }
                .flatMapLatest { parentId ->
                    Log.d(TAG, "loadLinkedStudents() parentId=$parentId")
                    parentStudentUseCases.getLinkedStudents(parentId)
                }
                .collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            Log.d(TAG, "loadLinkedStudents() -> Loading")
                            setState { copy(isLoading = true, error = null) }
                        }
                        is Resource.Success -> {
                            val count = resource.data?.size ?: 0
                            Log.d(TAG, "loadLinkedStudents() -> Success, count=$count")
                            setState {
                                copy(
                                    isLoading = false,
                                    linkedStudents = resource.data ?: emptyList(),
                                    error = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "loadLinkedStudents() -> Error: ${resource.message}")
                            setState {
                                copy(
                                    isLoading = false,
                                    error = resource.message
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun openRelationshipDialog(student: com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo) {
        Log.d(TAG, "openRelationshipDialog() studentId=${student.student.id}")
        setState {
            copy(
                selectedStudent = student,
                showRelationshipDialog = true,
                relationshipForEdit = student.parentStudent.relationship,
                isPrimaryGuardianForEdit = student.parentStudent.isPrimaryGuardian
            )
        }
    }

    private fun dismissRelationshipDialog() {
        Log.d(TAG, "dismissRelationshipDialog()")
        setState {
            copy(
                showRelationshipDialog = false,
                selectedStudent = null
            )
        }
    }

    private fun saveRelationship() {
        val current = state.value
        val student = current.selectedStudent ?: return

        launch {
            val parentId = resolveParentId()
            if (parentId.isNullOrBlank()) {
                Log.e(TAG, "saveRelationship() -> parentId is blank")
                showNotification("Không tìm thấy tài khoản phụ huynh", NotificationType.ERROR)
                return@launch
            }

            // UC08.3 - E1: ensure this student is linked to this parent
            var isLinked = false
            parentStudentUseCases.checkStudentLinkedToParent(
                CheckStudentLinkedToParentParams(parentId = parentId, studentId = student.student.id)
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> setState { copy(isProcessingAction = true, error = null) }
                    is Resource.Success -> isLinked = resource.data == true
                    is Resource.Error -> {
                        setState { copy(isProcessingAction = false, error = resource.message) }
                        showNotification(resource.message ?: "Không thể kiểm tra liên kết", NotificationType.ERROR)
                    }
                }
            }

            if (!isLinked) {
                val message = "Học sinh đã được liên kết với phụ huynh này"
                setState { copy(isProcessingAction = false, error = message) }
                showNotification(message, NotificationType.ERROR)
                return@launch
            }

            // UC08.3 - E2: only one primary guardian (excluding current parent)
            if (current.isPrimaryGuardianForEdit) {
                var hasOtherPrimaryGuardian = false
                parentStudentUseCases.checkStudentHasOtherPrimaryGuardian(
                    CheckStudentHasOtherPrimaryGuardianParams(
                        studentId = student.student.id,
                        parentId = parentId
                    )
                ).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> setState { copy(isProcessingAction = true, error = null) }
                        is Resource.Success -> hasOtherPrimaryGuardian = resource.data == true
                        is Resource.Error -> {
                            setState { copy(isProcessingAction = false, error = resource.message) }
                            showNotification(resource.message ?: "Không thể kiểm tra người giám hộ", NotificationType.ERROR)
                        }
                    }
                }

                if (hasOtherPrimaryGuardian) {
                    val message = "Học sinh chỉ có một người giám hộ chính"
                    setState { copy(isProcessingAction = false, error = message) }
                    showNotification(message, NotificationType.ERROR)
                    return@launch
                }
            }

            Log.d(
                TAG,
                "saveRelationship() parentId=$parentId, studentId=${student.student.id}, relationship=${current.relationshipForEdit}, isPrimary=${current.isPrimaryGuardianForEdit}"
            )

            parentStudentUseCases.updateRelationship(
                UpdateRelationshipParams(
                    parentId = parentId,
                    studentId = student.student.id,
                    relationship = current.relationshipForEdit,
                    isPrimaryGuardian = current.isPrimaryGuardianForEdit
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "saveRelationship() -> Loading")
                        setState { copy(isProcessingAction = true) }
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "saveRelationship() -> Success")
                        setState {
                            copy(
                                isProcessingAction = false,
                                showRelationshipDialog = false,
                                selectedStudent = null,
                                successMessage = "Cập nhật quan hệ thành công"
                            )
                        }
                        loadLinkedStudents()
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "saveRelationship() -> Error: ${resource.message}")
                        setState {
                            copy(
                                isProcessingAction = false,
                                error = resource.message
                            )
                        }
                        showNotification(
                            resource.message ?: "Không thể cập nhật quan hệ",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }

    private fun unlinkStudent(student: com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo) {
        Log.d(TAG, "unlinkStudent() studentId=${student.student.id}")
        launch {
            val parentId = resolveParentId()
            if (parentId.isNullOrBlank()) {
                Log.e(TAG, "unlinkStudent() -> parentId is blank")
                showNotification("Không tìm thấy tài khoản phụ huynh", NotificationType.ERROR)
                return@launch
            }

            parentStudentUseCases.unlinkStudent(
                UnlinkStudentParams(parentId = parentId, studentId = student.student.id)
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "unlinkStudent() -> Loading")
                        setState { copy(isProcessingAction = true) }
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "unlinkStudent() -> Success")
                        setState {
                            copy(
                                isProcessingAction = false,
                                successMessage = "Hủy liên kết thành công"
                            )
                        }
                        showNotification("Hủy liên kết thành công", NotificationType.SUCCESS)
                        loadLinkedStudents()
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "unlinkStudent() -> Error: ${resource.message}")
                        setState {
                            copy(
                                isProcessingAction = false,
                                error = resource.message
                            )
                        }
                        showNotification(
                            resource.message ?: "Không thể hủy liên kết",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }

    private fun searchStudents() {
        val query = state.value.searchQuery.trim()
        if (query.isBlank()) {
            Log.d(TAG, "searchStudents() -> empty query, clear results")
            setState { copy(searchResults = emptyList(), isSearching = false, hasSearched = false, error = null) }
            return
        }

        Log.d(TAG, "searchStudents() query='$query'")
        launch {
            parentStudentUseCases.searchStudent(query).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "searchStudents() -> Loading")
                        setState { copy(isSearching = true, hasSearched = true, error = null) }
                    }
                    is Resource.Success -> {
                        val count = resource.data?.size ?: 0
                        Log.d(TAG, "searchStudents() -> Success, count=$count")
                        setState {
                            copy(
                                isSearching = false,
                                hasSearched = true,
                                searchResults = resource.data ?: emptyList(),
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "searchStudents() -> Error: ${resource.message}")
                        setState {
                            copy(
                                isSearching = false,
                                hasSearched = true,
                                searchResults = emptyList(),
                                error = resource.message
                            )
                        }
                        showNotification(
                            resource.message ?: "Không thể tìm kiếm học sinh",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
}
