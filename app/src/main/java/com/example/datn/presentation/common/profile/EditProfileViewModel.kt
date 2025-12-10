package com.example.datn.presentation.common.profile

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.usecase.student.GetStudentProfileUseCase
import com.example.datn.domain.usecase.student.UpdateStudentProfileParams
import com.example.datn.domain.usecase.student.UpdateStudentProfileUseCase
import com.example.datn.domain.usecase.teacher.GetTeacherProfileUseCase
import com.example.datn.domain.usecase.teacher.UpdateTeacherProfileParams
import com.example.datn.domain.usecase.teacher.UpdateTeacherProfileUseCase
import com.example.datn.domain.usecase.parent.GetParentProfileUseCase
import com.example.datn.domain.usecase.parent.UpdateParentProfileParams
import com.example.datn.domain.usecase.parent.UpdateParentProfileUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getStudentProfile: GetStudentProfileUseCase,
    private val updateStudentProfile: UpdateStudentProfileUseCase,
    private val getTeacherProfile: GetTeacherProfileUseCase,
    private val updateTeacherProfile: UpdateTeacherProfileUseCase,
    private val getParentProfile: GetParentProfileUseCase,
    private val updateParentProfile: UpdateParentProfileUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<EditProfileState, EditProfileEvent>(
    initialState = EditProfileState(),
    notificationManager = notificationManager
) {

    override fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.LoadProfile -> loadProfile(event.userId, event.role)
            is EditProfileEvent.UpdateGradeLevel -> updateGradeLevel(event.gradeLevel)
            is EditProfileEvent.UpdateDateOfBirth -> updateDateOfBirth(event.dateOfBirth)
            is EditProfileEvent.UpdateSpecialization -> updateSpecialization(event.specialization)
            is EditProfileEvent.UpdateLevel -> updateLevel(event.level)
            is EditProfileEvent.UpdateExperienceYears -> updateExperienceYears(event.years)
            EditProfileEvent.SaveProfile -> saveProfile()
            EditProfileEvent.ClearMessages -> clearMessages()
        }
    }

    private fun loadProfile(userId: String, role: String) {
        viewModelScope.launch {
            when (role.uppercase()) {
                UserRole.STUDENT.name -> loadStudentProfile(userId)
                UserRole.TEACHER.name -> loadTeacherProfile(userId)
                UserRole.PARENT.name -> loadParentProfile(userId)
                else -> {
                    setState { copy(error = "Vai trò không hợp lệ") }
                    showNotification("Vai trò không hợp lệ", NotificationType.ERROR)
                }
            }
        }
    }

    private suspend fun loadStudentProfile(studentId: String) {
        getStudentProfile(studentId).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    val student = result.data
                    setState {
                        copy(
                            student = student,
                            gradeLevel = student?.gradeLevel ?: "",
                            dateOfBirth = student?.dateOfBirth,
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi tải hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private suspend fun loadTeacherProfile(teacherId: String) {
        getTeacherProfile(teacherId).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    val teacher = result.data
                    setState {
                        copy(
                            teacher = teacher,
                            specialization = teacher?.specialization ?: "",
                            level = teacher?.level ?: "",
                            experienceYears = teacher?.experienceYears?.toString() ?: "0",
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi tải hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private suspend fun loadParentProfile(parentId: String) {
        getParentProfile(parentId).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    setState { copy(parent = result.data, isLoading = false) }
                }
                is Resource.Error -> {
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi tải hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private fun updateGradeLevel(gradeLevel: String) {
        setState { copy(gradeLevel = gradeLevel) }
    }

    private fun updateDateOfBirth(dateOfBirth: java.time.LocalDate) {
        setState { copy(dateOfBirth = dateOfBirth) }
    }

    private fun updateSpecialization(specialization: String) {
        setState { copy(specialization = specialization) }
    }

    private fun updateLevel(level: String) {
        setState { copy(level = level) }
    }

    private fun updateExperienceYears(years: String) {
        setState { copy(experienceYears = years) }
    }

    private fun saveProfile() {
        val currentState = state.value

        viewModelScope.launch {
            when {
                currentState.student != null -> saveStudentProfile()
                currentState.teacher != null -> saveTeacherProfile()
                currentState.parent != null -> saveParentProfile()
                else -> {
                    showNotification("Không tìm thấy hồ sơ", NotificationType.ERROR)
                }
            }
        }
    }

    private suspend fun saveStudentProfile() {
        val currentState = state.value
        val studentId = currentState.student?.id
        
        if (studentId == null) {
            showNotification("Không tìm thấy ID học sinh", NotificationType.ERROR)
            return
        }

        updateStudentProfile(
            UpdateStudentProfileParams(
                studentId = studentId,
                gradeLevel = currentState.gradeLevel.ifBlank { null },
                dateOfBirth = currentState.dateOfBirth
            )
        ).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    setState { copy(isLoading = false, isSuccess = true, error = null) }
                    showNotification(
                        "Cập nhật hồ sơ thành công",
                        NotificationType.SUCCESS
                    )
                }
                is Resource.Error -> {
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi cập nhật hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private suspend fun saveTeacherProfile() {
        val currentState = state.value
        val teacherId = currentState.teacher?.id
        
        if (teacherId == null) {
            showNotification("Không tìm thấy ID giáo viên", NotificationType.ERROR)
            return
        }

        val experienceYears = currentState.experienceYears.toIntOrNull() ?: 0

        updateTeacherProfile(
            UpdateTeacherProfileParams(
                teacherId = teacherId,
                specialization = currentState.specialization.ifBlank { null },
                level = currentState.level.ifBlank { null },
                experienceYears = if (experienceYears > 0) experienceYears else null
            )
        ).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    setState { copy(isLoading = false, isSuccess = true, error = null) }
                    showNotification(
                        "Cập nhật hồ sơ thành công",
                        NotificationType.SUCCESS
                    )
                }
                is Resource.Error -> {
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi cập nhật hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private suspend fun saveParentProfile() {
        val currentState = state.value
        val parentId = currentState.parent?.id
        
        if (parentId == null) {
            showNotification("Không tìm thấy ID phụ huynh", NotificationType.ERROR)
            return
        }

        updateParentProfile(
            UpdateParentProfileParams(parentId = parentId)
        ).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    setState { copy(isLoading = false, isSuccess = true, error = null) }
                    showNotification(
                        "Cập nhật hồ sơ thành công",
                        NotificationType.SUCCESS
                    )
                }
                is Resource.Error -> {
                    setState {  copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi cập nhật hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private fun clearMessages() {
        setState { copy(error = null, isSuccess = false) }
    }
}
