package com.example.datn.presentation.common.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image

@Composable
fun AvatarPickerDialog(
    currentAvatarUrl: String?,
    isUploading: Boolean = false,
    uploadProgress: Float = 0f,
    onDismiss: () -> Unit,
    onSelectFile: () -> Unit,
    onConfirm: () -> Unit,
    selectedFileName: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cập nhật avatar",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hiển thị avatar hiện tại hoặc hình ảnh mặc định
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentAvatarUrl != null) {
                        AsyncImage(
                            model = currentAvatarUrl,
                            contentDescription = "Avatar hiện tại",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            onError = {
                                // Nếu tải ảnh thất bại, hiển thị icon mặc định
                            }
                        )
                    } else {
                        // Hiển thị icon mặc định khi không có URL
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar mặc định",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Nút chọn file
                Button(
                    onClick = onSelectFile,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Text(if (selectedFileName != null) "Chọn file khác" else "Chọn avatar")
                }

                // Hiển thị tên file đã chọn
                if (selectedFileName != null) {
                    Text(
                        text = "Đã chọn: $selectedFileName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Progress bar khi uploading
                if (isUploading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                        Text(
                            text = "${(uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isUploading && selectedFileName != null
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) {
                Text("Hủy")
            }
        }
    )
}
