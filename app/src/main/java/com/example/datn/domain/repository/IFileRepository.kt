package com.example.datn.domain.repository

import java.io.InputStream

interface IFileRepository {
    suspend fun uploadFile(objectName: String, inputStream: InputStream, size: Long, contentType: String)
    suspend fun getFile(objectName: String): InputStream
    suspend fun getFileUrl(objectName: String, expirySeconds: Int = 3600): String
    suspend fun updateFile(objectName: String, newStream: InputStream, size: Long, contentType: String)
    suspend fun deleteFile(objectName: String)
    suspend fun fileExists(objectName: String): Boolean
}