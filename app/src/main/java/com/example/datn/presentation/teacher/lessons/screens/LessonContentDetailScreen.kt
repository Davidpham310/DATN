package com.example.datn.presentation.teacher.lessons.screens

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import com.example.datn.presentation.teacher.lessons.LessonContentManagerViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonContentDetailScreen(
    contentId: String,
    contentUrl: String,
    onNavigateBack: () -> Unit,
    viewModel: LessonContentManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // ✅ Lấy ra nội dung cụ thể dựa trên contentId
    val content = state.lessonContents.find { it.id == contentId }
    Log.d("LessonContentDetailScreen", "Loaded content: $content")
    Log.d("LessonContentDetailScreen", "Content URL: $contentUrl")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(content?.title ?: "Chi tiết nội dung") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            content != null -> {
                LessonContentDetailBody(
                    content = content,
                    contentUrl = contentUrl,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không tìm thấy nội dung chi tiết!")
                }
            }
        }
    }
}

@Composable
private fun LessonContentDetailBody(
    content: LessonContent,
    contentUrl: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header - loại nội dung
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = content.contentType.displayName,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Hiển thị nội dung
        when (content.contentType) {
            ContentType.TEXT -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = content.content,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            ContentType.IMAGE -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    AsyncImage(
                        model = contentUrl,
                        contentDescription = content.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            ContentType.VIDEO -> {
                VideoPlayer(
                    videoUrl = contentUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(16.dp)
                )
            }

            ContentType.AUDIO -> {
                AudioPlayer(
                    audioUrl = contentUrl,
                    title = content.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            ContentType.PDF -> {
                PdfViewer(
                    pdfUrl = contentUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                        .padding(16.dp)
                )
            }

            ContentType.MINIGAME -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Mini Game",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { /* TODO: navigate mini game */ }) {
                            Text("Chơi ngay")
                        }
                    }
                }
            }
        }

        // Metadata
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Thông tin chi tiết",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("ID", content.id)
                InfoRow("Thứ tự", "#${content.order}")
                InfoRow("Ngày tạo", formatInstant(content.createdAt))
                InfoRow("Cập nhật", formatInstant(content.updatedAt))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Log.d("VideoPlayer", "🟢 Bắt đầu khởi tạo ExoPlayer với URL: $videoUrl")

    val exoPlayer = remember {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true) // ✅ Cho phép redirect 308/307
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                try {
                    Log.d("VideoPlayer", "🎬 Đang setMediaItem từ URL: $videoUrl")
                    val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                    setMediaItem(mediaItem)

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING ->
                                    Log.d("VideoPlayer", "⏳ Đang tải video...")
                                Player.STATE_READY ->
                                    Log.d("VideoPlayer", "✅ Video đã sẵn sàng để phát!")
                                Player.STATE_ENDED ->
                                    Log.d("VideoPlayer", "🏁 Video đã phát xong.")
                                Player.STATE_IDLE ->
                                    Log.d("VideoPlayer", "⚠️ ExoPlayer đang ở trạng thái IDLE.")
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e("VideoPlayer", "❌ Lỗi khi phát video: ${error.message}", error)
                        }
                    })

                    prepare()
                    playWhenReady = true // 🔥 Tự động phát
                    Log.d("VideoPlayer", "🧩 ExoPlayer đã prepare xong.")
                } catch (e: Exception) {
                    Log.e("VideoPlayer", "❌ Lỗi khi khởi tạo video: ${e.message}", e)
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("VideoPlayer", "🧹 Giải phóng ExoPlayer")
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            Log.d("VideoPlayer", "🧱 Tạo PlayerView")
            androidx.media3.ui.PlayerView(ctx).apply {
                player = exoPlayer
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun AudioPlayer(audioUrl: String, title: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Log.d("AudioPlayer", "🎧 Bắt đầu khởi tạo ExoPlayer cho audio: $audioUrl")

    val exoPlayer = remember {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true) // ✅ Cho phép chuyển hướng (HTTP 308)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                try {
                    Log.d("AudioPlayer", "🎶 SetMediaItem với URL: $audioUrl")
                    val mediaItem = MediaItem.fromUri(Uri.parse(audioUrl))
                    setMediaItem(mediaItem)

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> Log.d("AudioPlayer", "⏳ Đang tải audio...")
                                Player.STATE_READY -> Log.d("AudioPlayer", "✅ Audio đã sẵn sàng để phát!")
                                Player.STATE_ENDED -> Log.d("AudioPlayer", "🏁 Audio đã phát xong.")
                                Player.STATE_IDLE -> Log.d("AudioPlayer", "⚠️ ExoPlayer đang ở trạng thái IDLE.")
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e("AudioPlayer", "❌ Lỗi khi phát audio: ${error.message}", error)
                        }
                    })

                    prepare()
                    playWhenReady = true // 🔥 Tự động phát
                    Log.d("AudioPlayer", "🧩 ExoPlayer đã prepare xong.")
                } catch (e: Exception) {
                    Log.e("AudioPlayer", "❌ Lỗi khi khởi tạo audio: ${e.message}", e)
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("AudioPlayer", "🧹 Giải phóng ExoPlayer cho audio.")
            exoPlayer.release()
        }
    }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            AndroidView(
                factory = { ctx ->
                    Log.d("AudioPlayer", "🧱 Tạo PlayerView cho audio.")
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            200
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PdfViewer(pdfUrl: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                loadUrl("https://docs.google.com/viewer?url=$pdfUrl&embedded=true")
            }
        },
        modifier = modifier
    )
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
