package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository

class FileExistsUseCase(private val repository: IFileRepository) {
    suspend operator fun invoke(objectName: String): Boolean {
        return repository.fileExists(objectName)
    }
}
