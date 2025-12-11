package com.example.datn.domain.usecase.avatar

import com.example.datn.domain.repository.IFileRepository
import javax.inject.Inject

class CreateAvatarFolderUseCase @Inject constructor(
    private val repository: IFileRepository
) {
    suspend operator fun invoke(userId: String) {
        // MinIO tự động tạo thư mục khi upload file vào path
        // Tạo file marker để đảm bảo thư mục tồn tại
        val markerPath = "avatars/$userId/.folder"
        val markerContent = "".byteInputStream()
        
        try {
            repository.uploadFile(
                objectName = markerPath,
                inputStream = markerContent,
                size = 0L,
                contentType = "application/octet-stream"
            )
        } catch (e: Exception) {
            // Nếu tạo marker thất bại, không cần báo lỗi
            // Thư mục sẽ được tạo khi upload avatar thực tế
        }
    }
}
