package com.example.datn.presentation.splash

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.splash.SplashUseCase
import com.example.datn.presentation.common.splash.SplashEvent
import com.example.datn.presentation.common.splash.SplashState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val splashUsecase: SplashUseCase
) : BaseViewModel<SplashState, SplashEvent>(SplashState()) {

    init {
        checkUser()
    }

    private fun checkUser() {
        viewModelScope.launch {
            splashUsecase().collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        sendEvent(SplashEvent.NavigateToHome(result.data.role))
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        sendEvent(SplashEvent.NavigateToLogin)
                    }
                }
            }
        }
    }

    override fun onEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.CheckCurrentUser -> splashUsecase()
            else -> Unit
        }
    }
}
