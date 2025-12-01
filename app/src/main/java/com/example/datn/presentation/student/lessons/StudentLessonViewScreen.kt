package com.example.datn.presentation.student.lessons

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.datn.presentation.navigation.Screen
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentLessonViewScreen(
    lessonId: String,
    contentId: String,
    lessonTitle: String,
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: StudentLessonViewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(lessonId, contentId) {
        viewModel.onEvent(
            StudentLessonViewEvent.LoadLesson(
                lessonId = lessonId,
                initialContentId = contentId
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lessonTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.lessonContents.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${state.progressPercentage}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    if (state.lessonContents.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.onEvent(StudentLessonViewEvent.ShowProgressDialog)
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Tiến độ bài học"
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (state.showProgressDialog) {
            val totalContents = state.lessonContents.size
            val viewedContents = state.viewedContentIds.size.coerceAtMost(totalContents)
            val progress = state.progress
            val lastContentTitle = progress?.lastAccessedContentId?.let { lastId ->
                state.lessonContents.find { it.id == lastId }?.title
            }

            LessonProgressDialog(
                lessonTitle = lessonTitle,
                percentage = state.progressPercentage,
                viewedCount = viewedContents,
                totalCount = totalContents,
                timeSpentSeconds = progress?.timeSpentSeconds ?: 0L,
                lastAccessedAt = progress?.lastAccessedAt,
                lastContentTitle = lastContentTitle,
                onDismiss = {
                    viewModel.onEvent(StudentLessonViewEvent.DismissProgressDialog)
                },
                onContinue = {
                    viewModel.onEvent(StudentLessonViewEvent.DismissProgressDialog)
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error ?: "Đã xảy ra lỗi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                state.lessonContents.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chưa có nội dung",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Giáo viên sẽ sớm thêm nội dung bài học",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val currentContent = state.currentContent
                    val totalContents = state.lessonContents.size
                    val currentIndex = state.currentContentIndex

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Lesson header
                        LessonHeaderCard(
                            lesson = state.lesson,
                            onViewAllGames = {
                                // Start the first MINIGAME content directly instead of showing list
                                val virtualGameId = "lesson_$lessonId"
                                android.util.Log.d(
                                    "StudentLessonView",
                                    "🎯 Starting lesson-based minigame from header, lessonId: $lessonId, gameId: $virtualGameId"
                                )

                                navController.navigate(
                                    Screen.StudentMiniGamePlay.createRoute(virtualGameId, lessonId)
                                )
                            }
                        )

                        if (currentContent != null && totalContents > 0) {
                            // Current content header
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Nội dung ${currentIndex + 1}/$totalContents",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = currentContent.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (state.lessonContents.isNotEmpty()) {
                                    LinearProgressIndicator(
                                        progress = (state.progressPercentage.coerceIn(0, 100)) / 100f,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                    )
                                }
                            }

                            // Current content card (full-screen detail)
                            val isMiniGame = currentContent.contentType == ContentType.MINIGAME
                            val resolvedUrl = state.contentUrls[currentContent.id]
                            LessonContentCard(
                                content = currentContent,
                                resolvedContent = resolvedUrl,
                                onOpenContent = {
                                    // chỉ cập nhật tracking, viewer hiển thị trực tiếp trong card
                                    viewModel.onEvent(StudentLessonViewEvent.MarkCurrentAsViewed)
                                    viewModel.onEvent(StudentLessonViewEvent.SaveProgress)
                                },
                                onPlayGame = if (isMiniGame) { gameId ->
                                    viewModel.onEvent(StudentLessonViewEvent.MarkCurrentAsViewed)
                                    viewModel.onEvent(StudentLessonViewEvent.SaveProgress)
                                    android.util.Log.d(
                                        "StudentLessonView",
                                        "🎯 Navigating to lesson minigame from content with lessonId: $lessonId"
                                    )
                                    navController.navigate(
                                        Screen.StudentMiniGamePlay.createRoute(gameId, lessonId)
                                    )
                                } else { _ -> },
                                onVideoForceExit = {
                                    onNavigateBack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonProgressDialog(
    lessonTitle: String,
    percentage: Int,
    viewedCount: Int,
    totalCount: Int,
    timeSpentSeconds: Long,
    lastAccessedAt: java.time.Instant?,
    lastContentTitle: String?,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    val minutes = (timeSpentSeconds / 60).coerceAtLeast(0)
    val lastTimeText = lastAccessedAt?.toString() ?: "Chưa học"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "TIẾN ĐỘ BÀI HỌC",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = lessonTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = (percentage.coerceIn(0, 100)) / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    text = "${percentage.coerceIn(0, 100)}% hoàn thành",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "✓ Đã xem: ${viewedCount.coerceAtLeast(0)}/${totalCount.coerceAtLeast(0)} nội dung",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "⏱️ Thời gian học: ${minutes} phút",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "📅 Học lần cuối: ${lastTimeText}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (!lastContentTitle.isNullOrBlank()) {
                    Text(
                        text = "📍 Dừng lại ở: ${lastContentTitle}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("Tiếp tục học")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
private fun LessonHeaderCard(
    lesson: com.example.datn.domain.models.Lesson?,
    onViewAllGames: () -> Unit = {}
) {
    lesson?.let {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${it.order}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!it.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = it.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Mini Games Button
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onViewAllGames,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Games,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Xem tất cả trò chơi của bài học",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonContentCard(
    content: LessonContent,
    resolvedContent: String? = null,
    onOpenContent: () -> Unit = {},
    onPlayGame: (String) -> Unit = {},
    onVideoForceExit: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Content type badge and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContentTypeBadge(contentType = content.contentType)
                
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content based on type
            when (content.contentType) {
                ContentType.TEXT -> {
                    Text(
                        text = content.content,
                        modifier = Modifier.clickable { onOpenContent() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ContentType.VIDEO -> {
                    VideoContentView(
                        content = resolvedContent ?: content.content,
                        onClick = onOpenContent,
                        onForceExit = onVideoForceExit
                    )
                }
                ContentType.AUDIO -> {
                    AudioContentView(
                        content = resolvedContent ?: content.content,
                        onClick = onOpenContent
                    )
                }
                ContentType.IMAGE -> {
                    ImageContentView(
                        content = resolvedContent ?: content.content,
                        onClick = onOpenContent
                    )
                }
                ContentType.PDF -> {
                    PdfContentView(
                        content = resolvedContent ?: content.content,
                        onClick = onOpenContent
                    )
                }
                ContentType.MINIGAME -> {
                    MinigameContentView(
                        content = content.content,
                        onPlayGame = onPlayGame
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentTypeBadge(contentType: ContentType) {
    val (icon, color) = when (contentType) {
        ContentType.TEXT -> Icons.Default.Description to MaterialTheme.colorScheme.primary
        ContentType.VIDEO -> Icons.Default.VideoLibrary to MaterialTheme.colorScheme.error
        ContentType.AUDIO -> Icons.Default.AudioFile to MaterialTheme.colorScheme.tertiary
        ContentType.IMAGE -> Icons.Default.Image to MaterialTheme.colorScheme.secondary
        ContentType.PDF -> Icons.Default.PictureAsPdf to MaterialTheme.colorScheme.error
        ContentType.MINIGAME -> Icons.Default.Games to MaterialTheme.colorScheme.tertiary
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = contentType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun VideoContentView(
    content: String,
    onClick: () -> Unit = {},
    onForceExit: () -> Unit = {}
) {
    StudentVideoPlayer(
        videoUrl = content,
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        onUserInteraction = {},
        onForceExit = onForceExit,
        onCompleted = onClick
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun StudentVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onUserInteraction: () -> Unit = {},
    onForceExit: () -> Unit = {},
    onCompleted: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasInteracted by remember { mutableStateOf(false) }
    var showCompletionPrompt by remember { mutableStateOf(false) }
    var hasCompleted by remember { mutableStateOf(false) }
    var maxWatchedPositionMs by remember { mutableStateOf(0L) }

    val exoPlayer = remember {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                    setMediaItem(mediaItem)

                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying && !hasInteracted) {
                                hasInteracted = true
                                onUserInteraction()
                            }
                        }

                        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                            if (playWhenReady && !hasInteracted) {
                                hasInteracted = true
                                onUserInteraction()
                            }
                        }
                    })

                    prepare()
                } catch (e: Exception) {
                    android.util.Log.e("StudentVideoPlayer", "Error initializing video: ${e.message}", e)
                }
            }
    }

    LaunchedEffect(videoUrl) {
        val timeoutMillis = 30_000L
        delay(timeoutMillis)
        if (!hasInteracted) {
            onForceExit()
        }
    }

    LaunchedEffect(videoUrl, hasInteracted) {
        if (!hasInteracted) return@LaunchedEffect

        val completionThreshold = 0.8f
        val seekLeewayMs = 10_000L

        while (true) {
            val duration = exoPlayer.duration
            val position = exoPlayer.currentPosition

            if (duration > 0 && position >= 0) {
                if (exoPlayer.isPlaying && position > maxWatchedPositionMs) {
                    maxWatchedPositionMs = position
                }

                val maxAllowed = (maxWatchedPositionMs + seekLeewayMs).coerceAtMost(duration)
                if (position > maxAllowed) {
                    exoPlayer.seekTo(maxAllowed)
                }

                if (!hasCompleted) {
                    val progress = if (duration > 0) {
                        maxWatchedPositionMs.toFloat() / duration.toFloat()
                    } else {
                        0f
                    }
                    if (progress >= completionThreshold && !showCompletionPrompt) {
                        showCompletionPrompt = true
                    }
                }
            }

            delay(500L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.matchParentSize()
        )

        if (showCompletionPrompt) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bạn đã xem gần hết video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Bấm \"Xác nhận đã xem\" để tiếp tục học",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            if (!hasCompleted) {
                                hasCompleted = true
                                showCompletionPrompt = false
                                onCompleted()
                            }
                        }
                    ) {
                        Text("Xác nhận đã xem")
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun StudentAudioPlayer(
    audioUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(audioUrl))
                    setMediaItem(mediaItem)
                    prepare()
                } catch (e: Exception) {
                    android.util.Log.e("StudentAudioPlayer", "Error initializing audio: ${e.message}", e)
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            AndroidView(
                factory = { ctx ->
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
private fun AudioContentView(
    content: String,
    onClick: () -> Unit = {}
) {
    // Sử dụng ExoPlayer giống Teacher để phát audio ngay trong app
    StudentAudioPlayer(
        audioUrl = content,
        title = "File âm thanh",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}

@Composable
private fun ImageContentView(
    content: String,
    onClick: () -> Unit = {}
) {
    // Hiển thị hình ảnh trực tiếp giống Teacher (AsyncImage)
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        AsyncImage(
            model = content,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Composable
private fun PdfContentView(
    content: String,
    onClick: () -> Unit = {}
) {
    // Embed PDF viewer giống Teacher (sử dụng WebView + Google Docs Viewer)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
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
                    loadUrl("https://docs.google.com/viewer?url=$content&embedded=true")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MinigameContentView(
    content: String,
    onPlayGame: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayGame(content) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Games,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Column {
                Text(
                    text = "Trò chơi nhỏ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Nhấn để chơi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
