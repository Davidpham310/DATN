package com.example.datn.presentation.student.lessons.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

/**
 * Image Viewer Component sử dụng Coil
 * @param imageUrl: URL của hình ảnh
 * @param title: Tiêu đề hình ảnh
 * @param isLoading: Trạng thái đang tải
 * @param error: Thông báo lỗi nếu có
 */
@Composable
fun ImageViewerComposable(
    imageUrl: String,
    title: String,
    isLoading: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Đang tải hình ảnh...")
            }
        }
        return
    }

    if (error != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFFFFEBEE)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("✗ Lỗi tải hình ảnh", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, fontSize = 12.sp, color = Color.Red)
            }
        }
        return
    }

    if (imageUrl.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Chưa tải hình ảnh")
        }
        return
    }

    // Coil AsyncImage
    AsyncImage(
        model = imageUrl,
        contentDescription = "Hình ảnh: $title",
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 400.dp),
        contentScale = ContentScale.Fit,
        onState = { state ->
            when (state) {
                is AsyncImagePainter.State.Loading -> {
                    // Loading state handled by isLoading parameter
                }
                is AsyncImagePainter.State.Error -> {
                    // Error state handled by error parameter
                }
                is AsyncImagePainter.State.Success -> {
                    // Image loaded successfully
                }
                else -> {}
            }
        }
    )
}
