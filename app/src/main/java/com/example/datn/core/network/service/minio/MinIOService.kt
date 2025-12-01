package com.example.datn.core.network.service.minio

import android.util.Log
import com.example.datn.BuildConfig
import io.minio.*
import io.minio.errors.MinioException
import io.minio.http.Method
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MinIOService @Inject constructor(
    private val client: MinioClient,
    private val bucketName: String,
) {

    private val TAG = "MinIO Upload"

    /**
     * 🔹 Upload từ File lớn với progress
     */
    suspend fun uploadFile(
        objectName: String,
        file: File,
        contentType: String = "application/octet-stream",
        onProgress: ((uploaded: Long, total: Long) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        require(file.exists()) { "File không tồn tại: ${file.absolutePath}" }

        val totalSize = file.length()
        val uploadedBytes = AtomicLong(0)

        Log.d(TAG, "Uploading object: $objectName, size=$totalSize, contentType=$contentType")

        file.inputStream().use { input ->
            val wrappedInput = ProgressInputStream(input, totalSize) { uploaded, total ->
                uploadedBytes.set(uploaded)
                onProgress?.invoke(uploaded, total)
            }


            try {
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .`object`(objectName)
                        .stream(wrappedInput, totalSize, 100L * 1024 * 1024) // 100MB partSize → upload nhanh hơn
                        .contentType(contentType)
                        .build()
                )
                Log.d(TAG, "✅ Upload thành công: $objectName")
                onProgress?.invoke(totalSize, totalSize)
            } catch (e: MinioException) {
                Log.e(TAG, "❌ MinIO Error: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * 🔹 Upload từ InputStream lớn với progress
     * @param size: bắt buộc chính xác để tránh lỗi SignatureDoesNotMatch
     */
    suspend fun uploadFile(
        objectName: String,
        inputStream: InputStream,
        size: Long,
        contentType: String = "application/octet-stream",
        onProgress: ((uploaded: Long, total: Long) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        require(size > 0) { "Size phải > 0 để tránh lỗi chữ ký" }

        val wrappedInput = ProgressInputStream(inputStream, size) { uploaded, total ->
            onProgress?.invoke(uploaded, total)
        }

        Log.d(TAG, "Uploading object: $objectName, size=$size, contentType=$contentType")

        try {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(wrappedInput, size, 100L * 1024 * 1024) // 100MB partSize
                    .contentType(contentType)
                    .build()
            )
            Log.d(TAG, "✅ Upload thành công: $objectName")
            onProgress?.invoke(size, size)
        } catch (e: MinioException) {
            Log.e(TAG, "❌ MinIO Error: ${e.message}", e)
            throw e
        } finally {
            inputStream.close()
        }
    }

    /**
     * InputStream wrapper để báo progress
     */
    private class ProgressInputStream(
        private val input: InputStream,
        private val totalSize: Long,
        private val progressCallback: (uploaded: Long, total: Long) -> Unit
    ) : InputStream() {

        private var uploaded: Long = 0
        private var lastPercentLogged = -1

        override fun read(): Int {
            val byte = input.read()
            if (byte != -1) {
                uploaded++
                reportProgress()
            }
            return byte
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val count = input.read(b, off, len)
            if (count > 0) {
                uploaded += count
                reportProgress()
            }
            return count
        }

        private fun reportProgress() {
            if (totalSize <= 0) return
            val percent = ((uploaded * 100) / totalSize).toInt()
            if (percent != lastPercentLogged) {
                lastPercentLogged = percent
                progressCallback(uploaded, totalSize)
                Log.d("MinIO Upload Progress", "$percent% ($uploaded / $totalSize bytes)")
            }
        }

        override fun close() {
            input.close()
        }
    }


    suspend fun getFile(objectName: String): InputStream = withContext(Dispatchers.IO) {
        client.getObject(GetObjectArgs.builder().bucket(bucketName).`object`(objectName).build())
    }

    suspend fun getDirectFileUrl(objectName: String): String = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.MINIO_ENDPOINT.trimEnd('/')
        val bucket = bucketName.trim('/')
        val path = objectName.trimStart('/')
        val fullUrl = "$baseUrl/$bucket/$path"
        Log.d(TAG, "🌍 Direct URL: $fullUrl")
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
            client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).`object`(objectName).build())
            Log.d(TAG, "🗑️ Đã xóa file: $objectName")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi xóa file: ${e.message}")
            throw e
        }
    }

    suspend fun fileExists(objectName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucketName).`object`(objectName).build())
            true
        } catch (e: Exception) {
            false
        }
    }
}
