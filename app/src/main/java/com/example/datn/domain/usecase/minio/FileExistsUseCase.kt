package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository
import javax.inject.Inject

class FileExistsUseCase @Inject constructor(
    private val repository: IFileRepository
) {
    suspend operator fun invoke(objectName: String): Boolean {
        return repository.fileExists(objectName)
    }
}
