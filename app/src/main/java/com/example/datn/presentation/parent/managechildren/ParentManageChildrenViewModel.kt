package com.example.datn.presentation.parent.managechildren

import android.util.Log
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.RelationshipType
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.LinkParentToStudentParams
import com.example.datn.domain.usecase.parentstudent.ParentStudentUseCases
import com.example.datn.domain.usecase.parentstudent.UnlinkStudentParams
import com.example.datn.domain.usecase.parentstudent.UpdateRelationshipParams
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
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
            is ParentManageChildrenEvent.UnlinkStudent -> unlinkStudent(event.student)
            is ParentManageChildrenEvent.UpdateSearchQuery -> setState {
                copy(searchQuery = event.query)
            }
            is ParentManageChildrenEvent.SearchStudents -> searchStudents()
            is ParentManageChildrenEvent.LinkExistingStudent -> linkExistingStudent(
                event.result,
                event.relationship,
                event.isPrimaryGuardian
            )
            is ParentManageChildrenEvent.ClearMessages -> setState { copy(successMessage = null, error = null) }
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
            setState { copy(searchResults = emptyList()) }
            return
        }

        Log.d(TAG, "searchStudents() query='$query'")
        launch {
            parentStudentUseCases.searchStudent(query).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "searchStudents() -> Loading")
                        setState { copy(isSearching = true) }
                    }
                    is Resource.Success -> {
                        val count = resource.data?.size ?: 0
                        Log.d(TAG, "searchStudents() -> Success, count=$count")
                        setState {
                            copy(
                                isSearching = false,
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

    private fun linkExistingStudent(
        result: com.example.datn.domain.usecase.parentstudent.StudentSearchResult,
        relationship: RelationshipType,
        isPrimaryGuardian: Boolean
    ) {
        Log.d(
            TAG,
            "linkExistingStudent() studentId=${result.student.id}, relationship=$relationship, isPrimary=$isPrimaryGuardian"
        )
        launch {
            val parentId = resolveParentId()
            if (parentId.isNullOrBlank()) {
                Log.e(TAG, "linkExistingStudent() -> parentId is blank")
                showNotification("Không tìm thấy tài khoản phụ huynh", NotificationType.ERROR)
                return@launch
            }

            Log.d(TAG, "linkExistingStudent() parentId=$parentId")
            parentStudentUseCases.linkParentToStudent(
                LinkParentToStudentParams(
                    studentId = result.student.id,
                    parentId = parentId,
                    relationship = relationship.name
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "linkExistingStudent() -> Loading")
                        setState { copy(isProcessingAction = true) }
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "linkExistingStudent() -> Success, updating primary=$isPrimaryGuardian if needed")
                        // Nếu cần set isPrimaryGuardian khác, gọi UpdateRelationshipUseCase
                        if (!isPrimaryGuardian) {
                            parentStudentUseCases.updateRelationship(
                                UpdateRelationshipParams(
                                    parentId = parentId,
                                    studentId = result.student.id,
                                    relationship = relationship,
                                    isPrimaryGuardian = isPrimaryGuardian
                                )
                            ).collect()
                        }

                        setState {
                            copy(
                                isProcessingAction = false,
                                successMessage = "Liên kết học sinh thành công"
                            )
                        }
                        loadLinkedStudents()
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "linkExistingStudent() -> Error: ${resource.message}")
                        setState {
                            copy(
                                isProcessingAction = false,
                                error = resource.message
                            )
                        }
                        showNotification(
                            resource.message ?: "Không thể liên kết học sinh",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
}
