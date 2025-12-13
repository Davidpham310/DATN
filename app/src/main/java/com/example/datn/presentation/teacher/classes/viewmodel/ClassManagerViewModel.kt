package com.example.datn.presentation.teacher.classes.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.presentation.common.notifications.NotificationEvent
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.validation.rules.classmanager.ValidateClassCode
import com.example.datn.core.utils.validation.rules.classmanager.ValidateClassName
import com.example.datn.core.utils.validation.rules.classmanager.ValidateGradeLevel
import com.example.datn.core.utils.validation.rules.classmanager.ValidateSubject
import com.example.datn.domain.models.Class
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.classmanager.AddClassParams
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.domain.usecase.classmanager.UpdateClassParams
import com.example.datn.presentation.common.classmanager.ClassManagerEvent
import com.example.datn.presentation.common.classmanager.ClassManagerState
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassManagerViewModel @Inject constructor(
    private val classUseCases: ClassUseCases,
    private val authUseCase: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ClassManagerState, ClassManagerEvent>(ClassManagerState(), notificationManager) {

    private val validateName = ValidateClassName()
    private val validateCode = ValidateClassCode()
    private val validateGrade = ValidateGradeLevel()
    private val validateSubject = ValidateSubject()

    private val currentTeacherIdFlow: StateFlow<String> = authUseCase.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    init {
        observeClasses()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeClasses() {
        viewModelScope.launch {
            currentTeacherIdFlow
                .filter { it.isNotBlank() }
                .flatMapLatest { teacherId ->
                    classUseCases.getClassesByTeacher(teacherId)
                }
                .distinctUntilChanged()
                .collect { result ->
                    when (result) {
                        is Resource.Success -> setState {
                            copy(classes = result.data ?: emptyList(), isLoading = false, error = null)
                        }
                        is Resource.Error -> {
                            setState { copy(error = result.message, isLoading = false) }
                            showNotification(result.message ?: "Tải danh sách thất bại", NotificationType.ERROR)
                        }
                        is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    }
                }
        }
    }

    override fun onEvent(event: ClassManagerEvent) {
        when (event) {
            is ClassManagerEvent.ShowAddClassDialog -> setState {
                copy(showAddEditDialog = true, editingClass = null)
            }
            ClassManagerEvent.DismissDialog -> setState {
                copy(showAddEditDialog = false, editingClass = null)
            }
            is ClassManagerEvent.ConfirmAddClass -> addClass(
                event.name, event.classCode, event.gradeLevel, event.subject
            )
            is ClassManagerEvent.ConfirmEditClass -> updateClass(
                event.id, event.name, event.classCode, event.gradeLevel, event.subject
            )
            is ClassManagerEvent.DeleteClass -> {
                showConfirmDeleteClass(event.classModel)
            }
            is ClassManagerEvent.EditClass -> setState {
                copy(showAddEditDialog = true, editingClass = event.classModel)
            }
            is ClassManagerEvent.SelectClass -> setState {
                copy(selectedClass = event.classModel)
            }
            is ClassManagerEvent.ShowError -> showNotification(event.message, NotificationType.ERROR)
            ClassManagerEvent.RefreshClasses -> observeClasses()
        }
    }

    private fun showConfirmDeleteClass(classModel: Class) {
        setState {
            copy(
                // Dùng constructor mới
                confirmDeleteState = ConfirmationDialogState(
                    isShowing = true,
                    title = "Xác nhận xóa lớp",
                    message = "Bạn có chắc chắn muốn xóa lớp \"${classModel.name}\"?\n\nHành động này sẽ xóa toàn bộ dữ liệu liên quan và không thể hoàn tác.",
                    data = classModel
                )
            )
        }
    }
    fun dismissConfirmDeleteDialog() {
        setState {
            // Reset bằng phương thức empty() static
            copy(confirmDeleteState = ConfirmationDialogState.empty())
        }
    }

    // PHƯƠNG THỨC MỚI: Xác nhận và thực hiện xóa
    fun confirmDeleteClass(classModel: Class) { // NHẬN đối tượng Class đã được xác nhận
        // 1. Đóng dialog trước khi thực hiện xóa
        dismissConfirmDeleteDialog()

        // 2. Tiến hành xóa
        deleteClass(classModel)
    }

    private fun addClass(name: String, classCode: String, gradeLevel: Int, subject: String) {
        val teacherId = currentTeacherIdFlow.value
        if (teacherId.isBlank()) {
            showNotification("Không xác định được giáo viên", NotificationType.ERROR)
            return
        }

        val nameResult = validateName.validate(name)
        val codeResult = validateCode.validate(classCode)
        val gradeResult = validateGrade.validate(gradeLevel)
        val subjectResult = validateSubject.validate(subject)

        val hasError = listOf(nameResult, codeResult, gradeResult, subjectResult).any { !it.successful }

        if (hasError) {
            val message = listOf(nameResult, codeResult, gradeResult, subjectResult)
                .firstOrNull { !it.successful }?.errorMessage
            showNotification(message ?: "Dữ liệu không hợp lệ", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            classUseCases.addClass(AddClassParams(name, classCode, teacherId, gradeLevel, subject))
                .collect { resource ->
                    when (resource) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { copy(isLoading = false, showAddEditDialog = false, editingClass = null) }
                            showNotification("Thêm lớp học thành công!", NotificationType.SUCCESS)
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            showNotification(resource.message ?: "Thêm lớp thất bại", NotificationType.ERROR)
                        }
                    }
                }
            observeClasses()
        }
    }

    private fun deleteClass(classModel: Class) {
        viewModelScope.launch {
            classUseCases.deleteClass(classModel.id)
                .collect { resource ->
                    when (resource) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { copy(isLoading = false) }
                            showNotification("Xóa lớp học thành công!", NotificationType.SUCCESS)
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            showNotification(resource.message ?: "Xóa lớp thất bại", NotificationType.ERROR)
                        }
                    }
                }
            observeClasses()
        }
    }

    private fun updateClass(id: String, name: String, classCode: String, gradeLevel: Int, subject: String) {
        val teacherId = currentTeacherIdFlow.value
        if (teacherId.isBlank()) {
            showNotification("Không xác định được giáo viên", NotificationType.ERROR)
            return
        }

        val nameResult = validateName.validate(name)
        val codeResult = validateCode.validate(classCode)
        val gradeResult = validateGrade.validate(gradeLevel)
        val subjectResult = validateSubject.validate(subject)

        val hasError = listOf(nameResult, codeResult, gradeResult, subjectResult).any { !it.successful }
        if (hasError) {
            val message = listOf(nameResult, codeResult, gradeResult, subjectResult)
                .firstOrNull { !it.successful }?.errorMessage
            showNotification(message ?: "Dữ liệu không hợp lệ", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            classUseCases.updateClass(
                UpdateClassParams(
                    id = id,
                    name = name,
                    classCode = classCode,
                    teacherId = teacherId,
                    gradeLevel = gradeLevel,
                    subject = subject
                )
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false, editingClass = null) }
                        showNotification("Cập nhật lớp học thành công!", NotificationType.SUCCESS)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(resource.message ?: "Cập nhật lớp học thất bại", NotificationType.ERROR)
                    }
                }
            }
            observeClasses()
        }
    }
}