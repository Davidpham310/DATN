package com.example.datn.presentation.teacher.classes

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.classmanager.AddClassParams
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.presentation.common.classmanager.ClassManagerEvent
import com.example.datn.presentation.common.classmanager.ClassManagerState
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
                .collect { result ->
                    when(result) {
                        is Resource.Success -> setState { copy(classes = result.data ?: emptyList(), isLoading = false, error = null) }
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
        when(event) {
            is ClassManagerEvent.ShowAddClassDialog -> setState { copy(showAddEditDialog = true, editingClass = null) }
            ClassManagerEvent.DismissDialog -> setState { copy(showAddEditDialog = false, editingClass = null) }

            is ClassManagerEvent.ConfirmAddClass -> addClass(event.name, event.classCode)
            is ClassManagerEvent.DeleteClass -> deleteClass(event.classModel)
            is ClassManagerEvent.EditClass -> setState { copy(showAddEditDialog = true, editingClass = event.classModel) }

            is ClassManagerEvent.SelectClass -> setState { copy(selectedClass = event.classModel) }
            is ClassManagerEvent.ShowError -> showNotification(event.message, NotificationType.ERROR)
            ClassManagerEvent.RefreshClasses -> observeClasses()
        }
    }

    private fun addClass(name: String, classCode: String) {
        val teacherId = currentTeacherIdFlow.value
        if (teacherId.isBlank()) {
            showNotification("Không xác định được giáo viên", NotificationType.ERROR)
            return
        }
        viewModelScope.launch {
            classUseCases.addClass(AddClassParams(name, classCode , teacherId))
                .collect { resource ->
                    when(resource) {
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
                    when(resource) {
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
}
