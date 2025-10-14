package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository
import java.io.InputStream

class DownloadFileUseCase(private val repository: IFileRepository) {
    suspend operator fun invoke(objectName: String): InputStream {
        return repository.getFile(objectName)
    }
}
