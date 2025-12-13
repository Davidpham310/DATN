package com.example.datn.presentation.teacher.messaging.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectRecipientState(
    val isLoading: Boolean = false,
    val allUsers: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val parentCount: Int = 0,
    val studentCount: Int = 0
)

@HiltViewModel
class SelectRecipientViewModel @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(SelectRecipientState())
    val state: StateFlow<SelectRecipientState> = _state.asStateFlow()
    
    private var currentTab = 0 // 0 = Parents, 1 = Students

    fun loadParents() {
        currentTab = 0
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val result = firebaseDataSource.getUsersByRole(UserRole.PARENT.name)
                when (result) {
                    is Resource.Success -> {
                        val users = result.data ?: emptyList()
                        _state.value = _state.value.copy(
                            isLoading = false,
                            allUsers = users,
                            filteredUsers = applySearch(users, _state.value.searchQuery),
                            parentCount = users.size,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message ?: "Không thể tải danh sách phụ huynh"
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

    fun loadStudents() {
        currentTab = 1
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val result = firebaseDataSource.getUsersByRole(UserRole.STUDENT.name)
                when (result) {
                    is Resource.Success -> {
                        val users = result.data ?: emptyList()
                        _state.value = _state.value.copy(
                            isLoading = false,
                            allUsers = users,
                            filteredUsers = applySearch(users, _state.value.searchQuery),
                            studentCount = users.size,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message ?: "Không thể tải danh sách học sinh"
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
    
    fun search(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            filteredUsers = applySearch(_state.value.allUsers, query)
        )
    }
    
    private fun applySearch(users: List<User>, query: String): List<User> {
        if (query.isBlank()) return users
        
        return users.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true)
        }
    }
    
    fun reload() {
        when (currentTab) {
            0 -> loadParents()
            1 -> loadStudents()
        }
    }
}
