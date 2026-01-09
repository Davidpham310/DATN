package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository
import javax.inject.Inject

class UploadFileUseCase @Inject constructor(
    private val repository: IFileRepository
) {
    suspend operator fun invoke(
        objectName: String,
        inputStream: java.io.InputStream,
        size: Long,
        contentType: String,
        onProgress: ((uploaded: Long, total: Long) -> Unit)? = null
    ) {
        repository.uploadFile(objectName, inputStream, size, contentType, onProgress)
    }
}
