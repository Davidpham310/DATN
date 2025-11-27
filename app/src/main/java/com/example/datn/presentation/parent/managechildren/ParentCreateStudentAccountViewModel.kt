package com.example.datn.presentation.parent.managechildren

import android.util.Log
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.RelationshipType
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.CreateStudentAccountForParentUseCase
import com.example.datn.domain.usecase.parentstudent.CreateStudentAccountParams
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class ParentCreateStudentAccountViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    private val createStudentAccountForParent: CreateStudentAccountForParentUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<ParentCreateStudentAccountState, ParentCreateStudentAccountEvent>(
    ParentCreateStudentAccountState(),
    notificationManager
) {

    companion object {
        private const val TAG = "ParentCreateStudentVM"
    }

    override fun onEvent(event: ParentCreateStudentAccountEvent) {
        Log.d(TAG, "onEvent: $event")
        when (event) {
            is ParentCreateStudentAccountEvent.Submit -> submit(event)
            is ParentCreateStudentAccountEvent.ClearMessages ->
                setState { copy(error = null, isSuccess = false) }
        }
    }

    private fun submit(event: ParentCreateStudentAccountEvent.Submit) {
        Log.d(
            TAG,
            "submit() called with name='${event.name}', email='${event.email}', gradeLevel='${event.gradeLevel}', dob='${event.dateOfBirthText}', relationship=${event.relationship}, isPrimary=${event.isPrimaryGuardian}"
        )
        if (event.name.isBlank() || event.email.isBlank() || event.password.isBlank() || event.gradeLevel.isBlank() || event.dateOfBirthText.isBlank()) {
            Log.w(TAG, "submit() -> missing required fields")
            showNotification("Vui lòng điền đầy đủ thông tin", NotificationType.ERROR)
            return
        }

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dateOfBirth: LocalDate = try {
            Log.d(TAG, "submit() -> parsing dateOfBirth='${event.dateOfBirthText}'")
            LocalDate.parse(event.dateOfBirthText, formatter)
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "submit() -> invalid date format: ${e.message}")
            showNotification("Ngày sinh không hợp lệ (định dạng yyyy-MM-dd)", NotificationType.ERROR)
            return
        }

        launch {
            var resolvedParentId: String? = null

            authUseCases.getCurrentIdUser.invoke().collect { id ->
                if (id.isNotBlank()) {
                    resolvedParentId = id
                }
            }

            val parentId = resolvedParentId
            if (parentId.isNullOrBlank()) {
                Log.e(TAG, "submit() -> parentId is blank")
                showNotification("Không tìm thấy tài khoản phụ huynh", NotificationType.ERROR)
                return@launch
            }

            Log.d(TAG, "submit() -> parentId=$parentId, creating student account")
            createStudentAccountForParent(
                CreateStudentAccountParams(
                    parentId = parentId,
                    email = event.email,
                    password = event.password,
                    name = event.name,
                    dateOfBirth = dateOfBirth,
                    gradeLevel = event.gradeLevel,
                    relationship = event.relationship.name,
                    isPrimaryGuardian = event.isPrimaryGuardian
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "submit() -> Loading")
                        setState { copy(isLoading = true, error = null, isSuccess = false) }
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "submit() -> Success")
                        setState { copy(isLoading = false, error = null, isSuccess = true) }
                        showNotification("Tạo tài khoản học sinh thành công", NotificationType.SUCCESS)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "submit() -> Error: ${resource.message}")
                        setState { copy(isLoading = false, error = resource.message, isSuccess = false) }
                        showNotification(
                            resource.message ?: "Không thể tạo tài khoản học sinh",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
}
