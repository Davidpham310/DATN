package com.example.datn.domain.usecase.minio

import javax.inject.Inject

data class MinIOUseCase @Inject constructor(
    val uploadFile: UploadFileUseCase,
    val deleteFile: DeleteFileUseCase,
    val downloadFile: DownloadFileUseCase,
    val getFileUrl: GetFileUrlUseCase,
    val fileExist: FileExistsUseCase,
    val getDirectFileUrl: GetDirectFileUrlUseCase
)