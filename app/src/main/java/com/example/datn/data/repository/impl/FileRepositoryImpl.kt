package com.example.datn.data.repository.impl


import com.example.datn.core.network.service.minio.MinIOService
import com.example.datn.domain.repository.IFileRepository
import java.io.InputStream

class FileRepositoryImpl : IFileRepository {

    override suspend fun uploadFile(objectName: String, inputStream: InputStream, size: Long, contentType: String) {
        MinIOService.uploadFile(objectName, inputStream, size, contentType)
    }

    override suspend fun getFile(objectName: String): InputStream {
        return MinIOService.getFile(objectName)
    }

    override suspend fun getFileUrl(objectName: String, expirySeconds: Int): String {
        return MinIOService.getFileUrl(objectName, expirySeconds)
    }

    override suspend fun updateFile(objectName: String, newStream: InputStream, size: Long, contentType: String) {
        MinIOService.updateFile(objectName, newStream, size, contentType)
    }

    override suspend fun deleteFile(objectName: String) {
        MinIOService.deleteFile(objectName)
    }

    override suspend fun fileExists(objectName: String): Boolean {
        return MinIOService.fileExists(objectName)
    }
}
