package com.example.datn.domain.usecase.auth

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCurrentIdUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    operator fun invoke() = repository.getCurrentUser()
        .map { resource ->
        when (resource) {
            is Resource.Success -> resource.data?.id ?: ""
            else -> ""
        }
    }

}