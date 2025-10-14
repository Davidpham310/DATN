package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository
import java.io.InputStream

class UploadFileUseCase(private val repository: IFileRepository) {
    suspend operator fun invoke(objectName: String, inputStream: InputStream, size: Long, contentType: String) {
        repository.uploadFile(objectName, inputStream, size, contentType)
    }
}