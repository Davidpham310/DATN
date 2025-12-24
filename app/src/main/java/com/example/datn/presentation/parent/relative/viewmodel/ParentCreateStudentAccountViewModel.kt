package com.example.datn.presentation.parent.relative.viewmodel

import android.util.Log
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.validation.rules.parentstudent.StudentDateOfBirthValidator
import com.example.datn.core.utils.validation.rules.parentstudent.StudentEmailValidator
import com.example.datn.core.utils.validation.rules.parentstudent.StudentGradeLevelValidator
import com.example.datn.core.utils.validation.rules.parentstudent.StudentNameValidator
import com.example.datn.core.utils.validation.rules.parentstudent.StudentPasswordValidator
import com.example.datn.domain.models.RelationshipType
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.CreateStudentAccountForParentUseCase
import com.example.datn.domain.usecase.parentstudent.CreateStudentAccountParams
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.parent.relative.event.ParentCreateStudentAccountEvent
import com.example.datn.presentation.parent.relative.state.ParentCreateStudentAccountState
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val nameValidator = StudentNameValidator()
    private val emailValidator = StudentEmailValidator()
    private val passwordValidator = StudentPasswordValidator()
    private val gradeLevelValidator = StudentGradeLevelValidator()
    private val dateOfBirthValidator = StudentDateOfBirthValidator()

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

        val name = event.name.trim()
        val nameResult = nameValidator.validate(name)
        if (!nameResult.successful) {
            showNotification(nameResult.errorMessage ?: "Tên học sinh không hợp lệ", NotificationType.ERROR)
            return
        }

        val email = event.email.trim()
        val emailResult = emailValidator.validate(email)
        if (!emailResult.successful) {
            showNotification(emailResult.errorMessage ?: "Email không hợp lệ", NotificationType.ERROR)
            return
        }

        val password = event.password
        val passwordResult = passwordValidator.validate(password)
        if (!passwordResult.successful) {
            showNotification(passwordResult.errorMessage ?: "Mật khẩu không hợp lệ", NotificationType.ERROR)
            return
        }

        val gradeLevelRaw = event.gradeLevel.trim()
        val gradeResult = gradeLevelValidator.validate(gradeLevelRaw)
        if (!gradeResult.successful) {
            showNotification(gradeResult.errorMessage ?: "Khối lớp không hợp lệ", NotificationType.ERROR)
            return
        }

        val dobText = event.dateOfBirthText.trim()
        val dobResult = dateOfBirthValidator.validate(dobText)
        if (!dobResult.successful) {
            showNotification(dobResult.errorMessage ?: "Ngày sinh không hợp lệ", NotificationType.ERROR)
            return
        }
        val dateOfBirth = dateOfBirthValidator.parse(dobText) ?: run {
            showNotification("Ngày sinh không hợp lệ", NotificationType.ERROR)
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
                    email = email,
                    password = password,
                    name = name,
                    dateOfBirth = dateOfBirth,
                    gradeLevel = gradeLevelRaw,
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
                        val message = when {
                            resource.message?.contains("đã được sử dụng", ignoreCase = true) == true -> "Email đã được sử dụng"
                            resource.message?.contains("already", ignoreCase = true) == true &&
                                resource.message?.contains("use", ignoreCase = true) == true -> "Email đã được sử dụng"
                            else -> resource.message ?: "Không thể tạo tài khoản học sinh"
                        }
                        setState { copy(isLoading = false, error = message, isSuccess = false) }
                        showNotification(
                            message,
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
}
