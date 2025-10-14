package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository

class GetFileUrlUseCase(private val repository: IFileRepository) {
    suspend operator fun invoke(objectName: String, expirySeconds: Int = 3600): String {
        return repository.getFileUrl(objectName, expirySeconds)
    }
}
