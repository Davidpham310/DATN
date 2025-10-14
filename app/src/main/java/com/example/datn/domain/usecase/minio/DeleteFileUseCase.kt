package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository

class DeleteFileUseCase(private val repository: IFileRepository) {
    suspend operator fun invoke(objectName: String) {
        repository.deleteFile(objectName)
    }
}