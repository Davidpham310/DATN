package com.example.datn.presentation.parent.studentprofile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.domain.usecase.parentstudent.ParentStudentUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentDetailState(
    val isLoading: Boolean = false,
    val studentInfo: LinkedStudentInfo? = null,
    val error: String? = null
)

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val parentStudentUseCases: ParentStudentUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(StudentDetailState())
    val state: StateFlow<StudentDetailState> = _state.asStateFlow()
    
    // Giống như ClassManagerViewModel - tạo StateFlow cho parentId
    private val currentParentIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun loadStudentDetail(studentId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                // Lấy parent ID từ StateFlow (đợi giá trị hợp lệ)
                val parentId = currentParentIdFlow
                    .filter { it.isNotBlank() }
                    .first()
                
                Log.d("StudentDetailViewModel", "Current parent ID: $parentId")

                // Get all linked students
                parentStudentUseCases.getLinkedStudents(parentId).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _state.value = _state.value.copy(isLoading = true)
                        }
                        is Resource.Success -> {
                            val studentInfo = resource.data?.find { it.student.id == studentId }
                            if (studentInfo != null) {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    studentInfo = studentInfo,
                                    error = null
                                )
                            } else {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = "Không tìm thấy thông tin học sinh"
                                )
                            }
                        }
                        is Resource.Error -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = resource.message ?: "Lỗi tải thông tin học sinh"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Đã xảy ra lỗi"
                )
            }
        }
    }
}
