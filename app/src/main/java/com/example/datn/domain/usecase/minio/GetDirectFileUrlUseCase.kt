package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository
import javax.inject.Inject

class GetDirectFileUrlUseCase @Inject constructor(
    private val repository: IFileRepository
) {
    suspend operator fun invoke(objectName: String): String {
        return repository.getDirectFileUrl(objectName)
    }
}