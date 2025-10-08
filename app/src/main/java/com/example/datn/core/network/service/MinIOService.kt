package com.example.datn.core.network.service

import java.io.InputStream
import javax.inject.Inject

class MinIOService @Inject constructor(
    // private val minIOClient: MinIOClient // Giả định có MinIO Client
) {
    /**
     * Tải file lên MinIO và trả về đường dẫn lưu trữ.
     * @param inputStream Dữ liệu file
     * @param path Đường dẫn (bucket/folder/file.ext)
     * @return URL công khai (hoặc đường dẫn) của file trong MinIO.
     */
    suspend fun uploadFile(inputStream: InputStream, path: String): Result<String> {
        // ... Logic gọi MinIO Client ...
        return Result.failure(NotImplementedError("MinIO upload not implemented"))
    }
}