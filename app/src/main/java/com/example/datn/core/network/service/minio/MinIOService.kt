package com.example.datn.core.network.service.minio

import android.util.Log
import com.example.datn.core.network.config.MinIOConfig
import io.minio.*
import io.minio.http.Method
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.TimeUnit

object MinIOService {

    private val client = MinIOConfig.client
    private val bucketName = MinIOConfig.bucketName

    /**
     * 🟢 CREATE - Upload file mới
     */
    suspend fun uploadFile(
        objectName: String,
        inputStream: InputStream,
        size: Long,
        contentType: String
    ) = withContext(Dispatchers.IO) {
        try {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            )
            Log.d("MinIO", "✅ Upload thành công: $objectName")
        } catch (e: Exception) {
            Log.e("MinIO", "❌ Lỗi upload: ${e.message}")
            throw e
        }
    }

    /**
     * 🔵 READ - Lấy InputStream của file
     */
    suspend fun getFile(objectName: String): InputStream = withContext(Dispatchers.IO) {
        client.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .build()
        )
    }

    /**
     * 🔵 READ - Lấy URL tạm thời (dùng hiển thị ảnh trực tiếp)
     */
    suspend fun getFileUrl(objectName: String, expirySeconds: Int = 3600): String =
        withContext(Dispatchers.IO) {
            client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .`object`(objectName)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build()
            )
        }

    /**
     * 🟠 UPDATE - Ghi đè file cũ (thực chất là upload lại)
     */
    suspend fun updateFile(
        objectName: String,
        newStream: InputStream,
        size: Long,
        contentType: String
    ) = withContext(Dispatchers.IO) {
        deleteFile(objectName) // xóa cũ
        uploadFile(objectName, newStream, size, contentType) // upload lại
        Log.d("MinIO", "♻️ Đã cập nhật file: $objectName")
    }

    /**
     * 🔴 DELETE - Xóa file
     */
    suspend fun deleteFile(objectName: String) = withContext(Dispatchers.IO) {
        try {
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
            Log.d("MinIO", "🗑️ Đã xóa file: $objectName")
        } catch (e: Exception) {
            Log.e("MinIO", "❌ Lỗi xóa file: ${e.message}")
            throw e
        }
    }

    /**
     * 🔍 KIỂM TRA - File có tồn tại không
     */
    suspend fun fileExists(objectName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
