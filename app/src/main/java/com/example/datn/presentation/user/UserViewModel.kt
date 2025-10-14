package com.example.datn.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.usecase.user.UserUseCases
import com.example.datn.presentation.common.user.UserState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userUseCases: UserUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state.asStateFlow()

    // Lấy user theo ID
    fun getUserById(userId: String) {
        viewModelScope.launch {
            userUseCases.getUserById(userId).collect { result ->
                when(result) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> _state.update { it.copy(user = result.data, isLoading = false) }
                    is Resource.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                }
            }
        }
    }

    // Lấy user theo email
//    fun getUserByEmail(email: String) {
//        viewModelScope.launch {
//            userUseCases.getUserByEmail(email).collect { result ->
//                when(result) {
//                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
//                    is Resource.Success -> _state.update { it.copy(user = result.data, isLoading = false) }
//                    is Resource.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
//                }
//            }
//        }
//    }

    // Lấy danh sách user theo role
//    fun getUsersByRole(role: String) {
//        viewModelScope.launch {
//            userUseCases.getUsersByRole(role).collect { result ->
//                when(result) {
//                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
//                    is Resource.Success -> _state.update { it.copy(users = result.data ?: emptyList(), isLoading = false) }
//                    is Resource.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
//                }
//            }
//        }
//    }

    // Thêm user
    fun addUser(user: User, id: String? = null) {
        viewModelScope.launch {
            userUseCases.addUser(user, id).collect { result ->
                when(result) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> _state.update { it.copy(isLoading = false) }
                    is Resource.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                }
            }
        }
    }

    // Cập nhật user
//    fun updateUser(id: String, user: User) {
//        viewModelScope.launch {
//            userUseCases.updateUser(id, user).collect { result ->
//                when(result) {
//                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
//                    is Resource.Success -> _state.update { it.copy(isLoading = false) }
//                    is Resource.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
//                }
//            }
//        }
//    }

    // Cập nhật avatar
    fun updateAvatar(userId: String, avatarUrl: String) {
        viewModelScope.launch {
            userUseCases.updateAvatar(userId, avatarUrl).collect { result ->
                when(result) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> _state.update { it.copy(isLoading = false) }
                    is Resource.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                }
            }
        }
    }

    // Xoá user
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            userUseCases.deleteUser(userId).collect { result ->
                when(result) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> _state.update { it.copy(isLoading = false) }
                    is Resource.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                }
            }
        }
    }
}
