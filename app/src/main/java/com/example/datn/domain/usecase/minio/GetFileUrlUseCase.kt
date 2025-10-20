package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository
import javax.inject.Inject

class GetFileUrlUseCase @Inject constructor(
    private val repository: IFileRepository
) {
    suspend operator fun invoke(objectName: String, expirySeconds: Int = 3600): String {
        return repository.getFileUrl(objectName, expirySeconds)
    }
}
