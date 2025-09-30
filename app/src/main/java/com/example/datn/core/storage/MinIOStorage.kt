//package com.example.datn.core.storage
//
//import io.minio.MinioClient
//import javax.inject.Inject
//
//class MinIOStorage @Inject constructor(private val minioClient: MinioClient) {
//
//    fun uploadFile(bucket: String, objectName: String, filePath: String) {
//        minioClient.uploadObject { /* config */ }
//    }
//
//    // Các hàm download, delete tương tự
//}