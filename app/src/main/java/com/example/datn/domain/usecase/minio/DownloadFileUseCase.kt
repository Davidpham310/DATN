package com.example.datn.domain.usecase.minio

import com.example.datn.domain.repository.IFileRepository
import java.io.InputStream
import javax.inject.Inject

class DownloadFileUseCase @Inject constructor(
    private val repository: IFileRepository
) {
    suspend operator fun invoke(objectName: String): InputStream {
        return repository.getFile(objectName)
    }
}
