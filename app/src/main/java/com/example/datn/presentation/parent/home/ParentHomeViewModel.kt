package com.example.datn.presentation.parent.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.domain.usecase.parentstudent.ParentStudentUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentHomeState(
    val isLoading: Boolean = false,
    val linkedStudents: List<LinkedStudentInfo> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ParentHomeViewModel @Inject constructor(
    private val parentStudentUseCases: ParentStudentUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(ParentHomeState())
    val state: StateFlow<ParentHomeState> = _state.asStateFlow()

    init {
        loadLinkedStudents()
    }

    private fun loadLinkedStudents() {
        viewModelScope.launch {
            authUseCases.getCurrentIdUser.invoke()
                .filter { it.isNotBlank() }
                .flatMapLatest { parentId ->
                    parentStudentUseCases.getLinkedStudents(parentId)
                }
                .distinctUntilChanged()
                .collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _state.value = _state.value.copy(isLoading = true)
                        }
                        is Resource.Success -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                linkedStudents = resource.data ?: emptyList(),
                                error = null
                            )
                        }
                        is Resource.Error -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = resource.message
                            )
                        }
                    }
                }
        }
    }
}
