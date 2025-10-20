package com.example.datn.presentation.teacher.lessons.components

import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.bumptech.glide.Glide
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import java.time.format.DateTimeFormatter
import java.time.ZoneId

@Composable
fun LessonContentItem(
    content: LessonContent,
    contentUrl: String, // URL media từ state
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getContentIcon(content.contentType),
                        contentDescription = content.contentType.name,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = content.contentType.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = content.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Media Preview
            if (content.contentType != ContentType.TEXT) {
                Spacer(modifier = Modifier.height(12.dp))
                MediaPreview(content, contentUrl)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Text preview or file name
            if (content.contentType == ContentType.TEXT) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content.content.take(100) + if (content.content.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (content.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tệp tin: ${content.content.substringAfterLast('/')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Updated time
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Cập nhật: ${formatInstant(content.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun MediaPreview(content: LessonContent, contentUrl: String) {
    val context = LocalContext.current
    when (content.contentType) {
        ContentType.IMAGE -> {
            AsyncImage(
                model = contentUrl,
                contentDescription = "Ảnh nội dung",
                modifier = Modifier.height(200.dp).fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }
        ContentType.VIDEO -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { /* mở màn hình xem video */ },
                contentAlignment = Alignment.Center
            ) {
                val heightPx = (200.dp.value * LocalContext.current.resources.displayMetrics.density).toInt()
                AndroidView(
                    factory = {
                        ImageView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            Glide.with(context).asBitmap().load(contentUrl).frame(0).into(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        ContentType.AUDIO -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { /* mở màn hình nghe audio */ },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Headphones, contentDescription = "Audio Icon", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(text = content.title, style = MaterialTheme.typography.bodyLarge)
            }
        }
        ContentType.PDF -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Icon", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Tài liệu PDF: ${content.title}", style = MaterialTheme.typography.bodyLarge)
            }
        }
        else -> Spacer(modifier = Modifier.height(0.dp))
    }
}
private fun getContentIcon(type: ContentType): ImageVector {
    return when (type) {
        ContentType.TEXT -> Icons.Default.Description
        ContentType.VIDEO -> Icons.Default.PlayCircle
        ContentType.PDF -> Icons.Default.PictureAsPdf
        ContentType.IMAGE -> Icons.Default.Image
        ContentType.AUDIO -> Icons.Default.Headphones
        ContentType.MINIGAME -> Icons.Default.Extension
    }
}

private fun formatInstant(instant: java.time.Instant): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "N/A"
    }
}
