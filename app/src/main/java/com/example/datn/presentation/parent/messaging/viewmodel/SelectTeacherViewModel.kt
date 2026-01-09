package com.example.datn.presentation.parent.messaging.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datn.data.remote.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectTeacherState(
    val isLoading: Boolean = false,
    val teachers: List<User> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SelectTeacherViewModel @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(SelectTeacherState())
    val state: StateFlow<SelectTeacherState> = _state.asStateFlow()

    fun loadTeachers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val result = firebaseDataSource.getUsersByRole(UserRole.TEACHER.name)
                when (result) {
                    is Resource.Success -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            teachers = result.data ?: emptyList(),
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message ?: "Không thể tải danh sách giáo viên"
                        )
                    }
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(isLoading = true)
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
