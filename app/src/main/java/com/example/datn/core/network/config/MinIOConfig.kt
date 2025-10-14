package com.example.datn.core.network.config

import com.example.datn.BuildConfig
import io.minio.MinioClient

object MinIOConfig {
    private val endpoint = BuildConfig.MINIO_ENDPOINT
    private val accessKey = BuildConfig.MINIO_ACCESS_KEY
    private val secretKey = BuildConfig.MINIO_SECRET_KEY

    val client: MinioClient by lazy {
        MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
    }

    val bucketName: String = BuildConfig.MINIO_BUCKET
}
