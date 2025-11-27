package com.example.datn.presentation.parent.home

import android.util.Log
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

    companion object {
        private const val TAG = "ParentHomeViewModel"
    }

    private val _state = MutableStateFlow(ParentHomeState())
    val state: StateFlow<ParentHomeState> = _state.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

    init {
        Log.d(TAG, "init: loadLinkedStudents()")
        loadLinkedStudents()
    }

    fun refreshChildren() {
        Log.d(TAG, "refreshChildren() called -> reload linked students")
        loadLinkedStudents()
    }

    private fun loadLinkedStudents() {
        Log.d(TAG, "loadLinkedStudents() called")
        viewModelScope.launch {
            authUseCases.getCurrentIdUser.invoke()
                .filter { it.isNotBlank() }
                .flatMapLatest { parentId ->
                    Log.d(TAG, "loadLinkedStudents() parentId=$parentId")
                    parentStudentUseCases.getLinkedStudents(parentId)
                }
                .distinctUntilChanged()
                .collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            Log.d(TAG, "loadLinkedStudents() -> Loading")
                            _state.value = _state.value.copy(isLoading = true)
                        }
                        is Resource.Success -> {
                            val count = resource.data?.size ?: 0
                            Log.d(TAG, "loadLinkedStudents() -> Success, count=$count")
                            _state.value = _state.value.copy(
                                isLoading = false,
                                linkedStudents = resource.data ?: emptyList(),
                                error = null
                            )
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "loadLinkedStudents() -> Error: ${resource.message}")
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
