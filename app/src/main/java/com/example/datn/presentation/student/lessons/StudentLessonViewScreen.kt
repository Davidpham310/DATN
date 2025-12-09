package com.example.datn.presentation.student.lessons

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.datn.presentation.navigation.Screen
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    LaunchedEffect(state.shouldAutoExitLesson) {
        if (state.shouldAutoExitLesson) {
            onNavigateBack()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    
    // State để theo dõi video player
    var videoPlayerRef: ExoPlayer? by remember { mutableStateOf(null) }
    var audioPlayerRef: ExoPlayer? by remember { mutableStateOf(null) }
    
    // Khởi tạo lifecycle monitoring và media callbacks
    DisposableEffect(lifecycleOwner) {
        // Thiết lập callbacks để tạm dừng/tiếp tục video khi screen off/on
        viewModel.setMediaPlayerCallbacks(
            onPause = {
                // Tạm dừng video
                videoPlayerRef?.pause()
                audioPlayerRef?.pause()
            },
            onResume = {
                // Tiếp tục video (nếu cần)
                // Không auto play - để user quyết định
            }
        )
        
        // Khởi tạo lifecycle monitoring
        viewModel.startLifecycleMonitoring(lifecycleOwner.lifecycle)
        
        onDispose {
            // Dừng lifecycle monitoring khi screen bị destroy
            viewModel.stopLifecycleMonitoring()
        }
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
        if (state.showInactivityWarning) {
            InactivityWarningDialog(
                warningCount = state.inactivityWarningCount,
                maxWarnings = 3,
                isMaxWarning = state.inactivityWarningCount >= 3,
                onContinue = {
                    viewModel.onEvent(StudentLessonViewEvent.ContinueLesson)
                },
                onExit = {
                    viewModel.onEvent(StudentLessonViewEvent.ExitLessonWithoutSaving)
                }
            )
        }

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
                studySeriousnessScore = state.studySeriousnessScore,
                studySeriousnessLevel = state.studySeriousnessLevel,
                fastForwardCount = state.totalFastForwardCount,
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
                        LessonHeaderCard(
                            lesson = state.lesson,
                            onViewAllGames = {
                                val virtualGameId = "lesson_$lessonId"
                                navController.navigate(
                                    Screen.StudentMiniGamePlay.createRoute(virtualGameId, lessonId)
                                )
                            }
                        )

                        if (currentContent != null && totalContents > 0) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Nội dung ${currentIndex + 1}/$totalContents",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )

                                    val isCompleted = state.isCurrentContentEligibleForCompletion
                                    if (isCompleted) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Đã hoàn thành",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = currentContent.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val completionRule = viewModel.getCompletionRule(currentContent.contentType.name)
                                Text(
                                    text = "Yêu cầu: $completionRule",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (currentContent.contentType == ContentType.TEXT || currentContent.contentType == ContentType.IMAGE) {
                                    Text(
                                        text = "Thời gian xem: ${state.currentContentElapsedSeconds}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (state.lessonContents.isNotEmpty()) {
                                    LinearProgressIndicator(
                                        progress = (state.progressPercentage.coerceIn(0, 100)) / 100f,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                    )
                                }
                            }

                            val isMiniGame = currentContent.contentType == ContentType.MINIGAME
                            val resolvedUrl = state.contentUrls[currentContent.id]

                            LessonContentCard(
                                content = currentContent,
                                resolvedContent = resolvedUrl,
                                onOpenContent = {
                                    viewModel.onEvent(StudentLessonViewEvent.RecordInteraction("CLICK"))
                                    viewModel.onEvent(StudentLessonViewEvent.MarkCurrentAsViewed)
                                    viewModel.onEvent(StudentLessonViewEvent.SaveProgress)
                                },
                                onPlayGame = if (isMiniGame) { gameId ->
                                    viewModel.onEvent(StudentLessonViewEvent.RecordInteraction("CLICK"))
                                    viewModel.onEvent(StudentLessonViewEvent.MarkCurrentAsViewed)
                                    viewModel.onEvent(StudentLessonViewEvent.SaveProgress)
                                    navController.navigate(
                                        Screen.StudentMiniGamePlay.createRoute(gameId, lessonId)
                                    )
                                } else { _ -> },
                                onVideoForceExit = {
                                    onNavigateBack()
                                },
                                onRecordInteraction = {
                                    viewModel.onEvent(StudentLessonViewEvent.RecordInteraction("CLICK"))
                                },
                                shouldPauseMedia = state.showInactivityWarning,
                                onMediaProgress = { duration, position ->
                                    viewModel.onEvent(StudentLessonViewEvent.OnMediaProgress(duration, position))
                                },
                                onPlaybackStateChanged = { isPlaying ->
                                    viewModel.onEvent(StudentLessonViewEvent.OnMediaStateChanged(isPlaying, currentContent.contentType))
                                },
                                onContentViewTimeUpdate = { elapsedSeconds ->
                                    viewModel.onEvent(StudentLessonViewEvent.UpdateContentViewTime(
                                        contentId = currentContent.id,
                                        elapsedSeconds = elapsedSeconds,
                                        contentType = currentContent.contentType.name
                                    ))
                                },
                                onPdfScrollProgress = { scrollPercentage ->
                                    viewModel.onEvent(StudentLessonViewEvent.UpdatePdfScrollProgress(
                                        contentId = currentContent.id,
                                        scrollPercentage = scrollPercentage
                                    ))
                                },
                                onVideoPlayerCreated = { player ->
                                    videoPlayerRef = player
                                },
                                onAudioPlayerCreated = { player ->
                                    audioPlayerRef = player
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
    studySeriousnessScore: Int = 100,
    studySeriousnessLevel: String = "Rất nghiêm túc",
    fastForwardCount: Int = 0,
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
                    text = "Đã xem: ${viewedCount.coerceAtLeast(0)}/${totalCount.coerceAtLeast(0)} nội dung",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Thời gian học: ${minutes} phút",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Học lần cuối: ${lastTimeText}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (!lastContentTitle.isNullOrBlank()) {
                    Text(
                        text = "Dừng lại ở: ${lastContentTitle}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "ĐÁNH GIÁ HỌC TẬP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Điểm nghiêm túc:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "$studySeriousnessScore/100",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            studySeriousnessScore >= 80 -> MaterialTheme.colorScheme.primary
                            studySeriousnessScore >= 60 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mức độ:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = studySeriousnessLevel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (fastForwardCount > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Phát hiện $fastForwardCount lần tua nhanh",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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

// ... existing code for LessonHeaderCard ...
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
    onVideoForceExit: () -> Unit = {},
    onRecordInteraction: () -> Unit = {},
    shouldPauseMedia: Boolean = false,
    onMediaProgress: (Long, Long) -> Unit = { _, _ -> },
    onPlaybackStateChanged: (Boolean) -> Unit = {},
    onContentViewTimeUpdate: (Long) -> Unit = {},  // Callback mới
    onPdfScrollProgress: (Int) -> Unit = {},        // Callback mới
    onVideoPlayerCreated: (ExoPlayer?) -> Unit = {},  // Callback mới
    onAudioPlayerCreated: (ExoPlayer?) -> Unit = {}   // Callback mới
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

            when (content.contentType) {
                ContentType.TEXT -> {
                    StudentTextPlayer(
                        textContent = content.content,
                        modifier = Modifier.fillMaxWidth(),
                        onRecordInteraction = onRecordInteraction,
                        onViewTimeUpdate = onContentViewTimeUpdate  // Truyền callback
                    )
                }
                ContentType.VIDEO -> {
                    VideoContentView(
                        content = resolvedContent ?: content.content,
                        onClick = onOpenContent,
                        onForceExit = onVideoForceExit,
                        onRecordInteraction = onRecordInteraction,
                        shouldPause = shouldPauseMedia,
                        onMediaProgress = onMediaProgress,
                        onPlaybackStateChanged = onPlaybackStateChanged,
                        onPlayerCreated = onVideoPlayerCreated
                    )
                }
                ContentType.AUDIO -> {
                    AudioContentView(
                        content = resolvedContent ?: content.content,
                        onClick = onOpenContent,
                        onRecordInteraction = onRecordInteraction,
                        onMediaProgress = onMediaProgress,
                        onPlaybackStateChanged = onPlaybackStateChanged,
                        onPlayerCreated = onAudioPlayerCreated
                    )
                }
                ContentType.IMAGE -> {
                    ImageContentView(
                        content = resolvedContent ?: content.content,
                        onClick = onOpenContent,
                        onRecordInteraction = onRecordInteraction,
                        onViewTimeUpdate = onContentViewTimeUpdate  // Truyền callback
                    )
                }
                ContentType.PDF -> {
                    PdfContentView(
                        content = resolvedContent ?: content.content,
                        onClick = onOpenContent,
                        onRecordInteraction = onRecordInteraction,
                        onScrollProgress = onPdfScrollProgress  // Truyền callback
                    )
                }
                ContentType.MINIGAME -> {
                    MinigameContentView(
                        content = content.content,
                        onPlayGame = onPlayGame,
                        onRecordInteraction = onRecordInteraction
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
private fun StudentTextPlayer(
    textContent: String,
    modifier: Modifier = Modifier,
    onRecordInteraction: () -> Unit = {},
    onViewTimeUpdate: (Long) -> Unit = {}  // Callback mới
) {
    var hasInitialized by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var hasScrolled by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        // Ghi nhận tương tác khi load nội dung
        if (!hasInitialized) {
            hasInitialized = true
            Log.d("StudentTextPlayer", "👆 Text content loaded - Recording START_VIEW interaction")
            onRecordInteraction()
        }

        while (true) {
            delay(1000)
            elapsedSeconds++
            onViewTimeUpdate(elapsedSeconds)  // Báo về ViewModel mỗi giây
        }
    }

    // Bắt sự kiện scroll
    LaunchedEffect(scrollState.value) {
        if (scrollState.value > 0 && !hasScrolled) {
            hasScrolled = true
            // Ghi nhận tương tác SCROLL
            Log.d("StudentTextPlayer", "👆 User scrolled text content - Recording SCROLL interaction")
            onRecordInteraction()
        }
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = textContent,
                modifier = Modifier.clickable {
                    Log.d("StudentTextPlayer", "👆 User clicked text content - Recording CLICK interaction")
                    onRecordInteraction()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudentImagePlayer(
    imageUrl: String,
    modifier: Modifier = Modifier,
    onRecordInteraction: () -> Unit = {},
    onViewTimeUpdate: (Long) -> Unit = {}  // Callback mới
) {
    var hasInitialized by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var hasLongPressed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Ghi nhận tương tác khi load image
        if (!hasInitialized) {
            hasInitialized = true
            Log.d("StudentImagePlayer", "👆 Image loaded - Recording START_VIEW interaction")
            onRecordInteraction()
        }

        while (true) {
            delay(1000)
            elapsedSeconds++
            onViewTimeUpdate(elapsedSeconds)  // Báo về ViewModel mỗi giây
        }
    }

    Card(
        modifier = modifier.clickable {
            Log.d("StudentImagePlayer", "👆 User clicked image - Recording CLICK interaction")
            onRecordInteraction()
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (!hasLongPressed) {
                                hasLongPressed = true
                                Log.d("StudentImagePlayer", "👆 User long pressed image - Recording LONG_PRESS interaction")
                                onRecordInteraction()
                            }
                        }
                    )
                },
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Composable
private fun ImageContentView(
    content: String,
    onClick: () -> Unit = {},
    onRecordInteraction: () -> Unit = {},
    onViewTimeUpdate: (Long) -> Unit = {}  // Callback mới
) {
    StudentImagePlayer(
        imageUrl = content,
        modifier = Modifier.fillMaxWidth(),
        onRecordInteraction = onRecordInteraction,
        onViewTimeUpdate = onViewTimeUpdate
    )
}

@Composable
private fun StudentPdfPlayer(
    pdfUrl: String,
    modifier: Modifier = Modifier,
    onRecordInteraction: () -> Unit = {},
    onScrollProgress: (Int) -> Unit = {}  // Callback mới
) {
    var hasInitialized by remember { mutableStateOf(false) }
    var hasScrolled by remember { mutableStateOf(false) }
    var currentScrollProgress by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Ghi nhận tương tác khi load PDF
        if (!hasInitialized) {
            hasInitialized = true
            Log.d("StudentPdfPlayer", "👆 PDF loaded - Recording START_VIEW interaction")
            onRecordInteraction()
        }

        // Giả lập tiến độ cuộn tăng dần theo thời gian
        while (currentScrollProgress < 100) {
            delay(2000)  // Mỗi 2 giây tăng 5%
            currentScrollProgress = (currentScrollProgress + 5).coerceAtMost(100)
            
            // Ghi nhận tương tác SCROLL khi scroll lần đầu
            if (currentScrollProgress > 0 && !hasScrolled) {
                hasScrolled = true
                Log.d("StudentPdfPlayer", "👆 User scrolled PDF - Recording SCROLL interaction")
                onRecordInteraction()
            }
            
            onScrollProgress(currentScrollProgress)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .clickable {
                Log.d("StudentPdfPlayer", "👆 User clicked PDF - Recording CLICK interaction")
                onRecordInteraction()
            },
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
                    loadUrl("https://docs.google.com/viewer?url=$pdfUrl&embedded=true")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PdfContentView(
    content: String,
    onClick: () -> Unit = {},
    onRecordInteraction: () -> Unit = {},
    onScrollProgress: (Int) -> Unit = {}  // Callback mới
) {
    StudentPdfPlayer(
        pdfUrl = content,
        modifier = Modifier.fillMaxWidth(),
        onRecordInteraction = onRecordInteraction,
        onScrollProgress = onScrollProgress
    )
}

// ... existing code for VideoContentView, AudioContentView, MinigameContentView, InactivityWarningDialog ...
@Composable
private fun VideoContentView(
    content: String,
    onClick: () -> Unit = {},
    onForceExit: () -> Unit = {},
    onRecordInteraction: () -> Unit = {},
    shouldPause: Boolean = false,
    onMediaProgress: (Long, Long) -> Unit = { _, _ -> },
    onPlaybackStateChanged: (Boolean) -> Unit = {},
    onPlayerCreated: (ExoPlayer?) -> Unit = {}
) {
    StudentVideoPlayer(
        videoUrl = content,
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        onUserInteraction = onRecordInteraction,
        onForceExit = onForceExit,
        onCompleted = onClick,
        shouldPause = shouldPause,
        onMediaProgress = onMediaProgress,
        onPlaybackStateChanged = onPlaybackStateChanged,
        onPlayerCreated = onPlayerCreated
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun StudentVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onUserInteraction: () -> Unit = {},
    onForceExit: () -> Unit = {},
    onCompleted: () -> Unit = {},
    shouldPause: Boolean = false,
    onMediaProgress: (Long, Long) -> Unit = { _, _ -> },
    onPlaybackStateChanged: (Boolean) -> Unit = {},
    onPlayerCreated: (ExoPlayer?) -> Unit = {}
) {
    val context = LocalContext.current
    var hasInteracted by remember { mutableStateOf(false) }
    var hasCompleted by remember { mutableStateOf(false) }
    var showCompletionPrompt by remember { mutableStateOf(false) }
    var maxWatchedPositionMs by remember { mutableStateOf(0L) }

    val exoPlayer = remember(videoUrl) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
                repeatMode = Player.REPEAT_MODE_OFF

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying && !hasInteracted) {
                            hasInteracted = true
                            Log.d("StudentVideoPlayer", "👆 Video loaded - Recording START_VIEW interaction")
                            onUserInteraction()
                        }
                        if (isPlaying) {
                            Log.d("StudentVideoPlayer", "👆 User played video - Recording MEDIA_PLAY interaction")
                        } else {
                            Log.d("StudentVideoPlayer", "⏸️ Video paused - Recording MEDIA_PAUSE interaction")
                        }
                        onPlaybackStateChanged(isPlaying)
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            playWhenReady = false
                            pause()
                            onPlaybackStateChanged(false)
                            if (!hasCompleted) {
                                showCompletionPrompt = true
                            }
                        }
                    }
                })

                prepare()
            }
    }
    
    // Thông báo cho screen về player được tạo
    LaunchedEffect(exoPlayer) {
        onPlayerCreated(exoPlayer)
    }

    LaunchedEffect(videoUrl) {
        delay(30_000)
        if (!hasInteracted) onForceExit()
    }

    LaunchedEffect(shouldPause) {
        if (shouldPause) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState != Player.STATE_ENDED) {
                exoPlayer.play()
            }
        }
    }

    LaunchedEffect(hasInteracted, videoUrl) {
        if (!hasInteracted) return@LaunchedEffect

        while (true) {
            val duration = exoPlayer.duration
            val position = exoPlayer.currentPosition

            if (duration > 0) {
                onMediaProgress(duration, position)

                if (exoPlayer.isPlaying && position > maxWatchedPositionMs) {
                    maxWatchedPositionMs = position
                }

                val seekLeeway = 10_000L
                val maxAllowed = (maxWatchedPositionMs + seekLeeway).coerceAtMost(duration)
                if (position > maxAllowed) {
                    exoPlayer.seekTo(maxAllowed)
                }

                if (!hasCompleted) {
                    val progressPercentage = ((position * 100) / duration).toInt()
                    if (progressPercentage >= 98) {
                        showCompletionPrompt = true
                    }
                }
            }

            delay(500)
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Bạn đã xem gần hết video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Bấm \"Xác nhận\" để tiếp tục")

                    Button(
                        onClick = {
                            hasCompleted = true
                            showCompletionPrompt = false
                            onCompleted()
                        }
                    ) {
                        Text("Xác nhận đã xem")
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioContentView(
    content: String,
    onClick: () -> Unit = {},
    onRecordInteraction: () -> Unit = {},
    onMediaProgress: (Long, Long) -> Unit = { _, _ -> },
    onPlaybackStateChanged: (Boolean) -> Unit = {},
    onPlayerCreated: (ExoPlayer?) -> Unit = {}
) {
    StudentAudioPlayer(
        audioUrl = content,
        title = "File âm thanh",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        onUserInteraction = onRecordInteraction,
        onMediaProgress = onMediaProgress,
        onPlaybackStateChanged = onPlaybackStateChanged,
        onPlayerCreated = onPlayerCreated
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun StudentAudioPlayer(
    audioUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    onUserInteraction: () -> Unit = {},
    onMediaProgress: (Long, Long) -> Unit = { _, _ -> },
    onPlaybackStateChanged: (Boolean) -> Unit = {},
    onPlayerCreated: (ExoPlayer?) -> Unit = {}
) {
    val context = LocalContext.current
    var hasInteracted by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(audioUrl))
                    setMediaItem(mediaItem)

                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying && !hasInteracted) {
                                hasInteracted = true
                                Log.d("StudentAudioPlayer", "👆 Audio loaded - Recording START_VIEW interaction")
                                onUserInteraction()
                            }
                            if (isPlaying) {
                                Log.d("StudentAudioPlayer", "👆 User played audio - Recording MEDIA_PLAY interaction")
                            } else {
                                Log.d("StudentAudioPlayer", "⏸️ Audio paused - Recording MEDIA_PAUSE interaction")
                            }
                            onPlaybackStateChanged(isPlaying)
                        }
                    })

                    prepare()
                } catch (e: Exception) {
                    android.util.Log.e("StudentAudioPlayer", "Error initializing audio: ${e.message}", e)
                }
            }
    }
    
    // Thông báo cho screen về player được tạo
    LaunchedEffect(exoPlayer) {
        onPlayerCreated(exoPlayer)
    }

    LaunchedEffect(hasInteracted, audioUrl) {
        if (!hasInteracted) return@LaunchedEffect

        while (true) {
            val duration = exoPlayer.duration
            val position = exoPlayer.currentPosition

            if (duration > 0) {
                onMediaProgress(duration, position)
            }

            delay(500)
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
private fun MinigameContentView(
    content: String,
    onPlayGame: (String) -> Unit = {},
    onRecordInteraction: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onRecordInteraction()
                onPlayGame(content)
            },
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

@Composable
private fun InactivityWarningDialog(
    warningCount: Int,
    maxWarnings: Int = 3,
    isMaxWarning: Boolean = false,
    onContinue: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = { },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Bạn vẫn đang học chứ?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isMaxWarning) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Bạn đã không tương tác quá lâu. Tiến trình học tập sẽ không được lưu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Hãy tương tác để tiếp tục! Nếu không có tương tác, bài học sẽ bị thoát.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cảnh báo: $warningCount/$maxWarnings",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tôi đang học")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thoát")
            }
        }
    )
}
