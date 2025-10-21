package com.example.datn.core.network.service.minio

import android.util.Log
import com.example.datn.BuildConfig
import io.minio.*
import io.minio.http.Method
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MinIOService @Inject constructor(
    private val client: MinioClient,
    private val bucketName: String
) {

    /**
     * 🟢 CREATE - Upload file mới
     */
    suspend fun uploadFile(
        objectName: String,
        inputStream: InputStream,
        size: Long = -1,
        contentType: String,
    ) = withContext(Dispatchers.IO) {
        try {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(inputStream, size, 10 * 1024 * 1024 )// 10MB mặc định
                    .contentType(contentType)
                    .build()
            )
            Log.d("MinIO", "✅ Upload thành công: $objectName với size là $size với kiểu là $contentType" )
        } catch (e: Exception) {
            Log.e("MinIO", "❌ Lỗi upload: ${e.message}")
            throw e
        }
    }

    suspend fun getFile(objectName: String): InputStream = withContext(Dispatchers.IO) {
        client.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .build()
        )
    }

    suspend fun getDirectFileUrl(objectName: String): String = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.MINIO_ENDPOINT.trimEnd('/')
        val bucket = bucketName.trim('/')
        val path = objectName.trimStart('/')

        val fullUrl = "$baseUrl/$bucket/$path"

        Log.d("MinIO", "🌍 Direct URL: $fullUrl")
        fullUrl
    }

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
