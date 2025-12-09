package com.example.datn.presentation.student.lessons.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay

/**
 * Video Player Component sử dụng ExoPlayer
 * @param videoUrl: URL của video
 * @param title: Tiêu đề video
 * @param onPositionChanged: Callback khi vị trí video thay đổi (position, duration)
 * @param onPlaybackStateChanged: Callback khi trạng thái phát lại thay đổi
 * @param isLoading: Trạng thái đang tải
 * @param error: Thông báo lỗi nếu có
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerComposable(
    videoUrl: String,
    title: String,
    onPositionChanged: (Long, Long) -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Đang tải video...", color = Color.White)
            }
        }
        return
    }

    if (error != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFF330000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("✗ Lỗi tải video", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = Color.Red, fontSize = 12.sp)
            }
        }
        return
    }

    if (videoUrl.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Chưa tải video", color = Color.White)
        }
        return
    }

    // ExoPlayer instance
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Update media item when URL changes
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            try {
                Log.d("VideoPlayerComposable", "Loading video: $videoUrl")
                val mediaItem = MediaItem.fromUri(videoUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                
                // Add listener to play when ready and catch errors
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val stateName = when (playbackState) {
                            Player.STATE_IDLE -> "IDLE"
                            Player.STATE_BUFFERING -> "BUFFERING"
                            Player.STATE_READY -> "READY"
                            Player.STATE_ENDED -> "ENDED"
                            else -> "UNKNOWN($playbackState)"
                        }
                        Log.d("VideoPlayerComposable", "Playback state changed: $stateName")
                        if (playbackState == Player.STATE_READY) {
                            exoPlayer.play()
                            Log.d("VideoPlayerComposable", "Video is ready, starting playback")
                        }
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("VideoPlayerComposable", "Player error: ${error.message}", error)
                        Log.e("VideoPlayerComposable", "Error code: ${error.errorCode}")
                        Log.e("VideoPlayerComposable", "Cause: ${error.cause}")
                    }
                }
                exoPlayer.addListener(listener)
                
                // Try to play immediately
                delay(500) // Wait for prepare to complete
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    exoPlayer.play()
                    Log.d("VideoPlayerComposable", "Video ready, playing now")
                } else {
                    Log.d("VideoPlayerComposable", "Video not ready yet, state: ${exoPlayer.playbackState}")
                }
            } catch (e: Exception) {
                Log.e("VideoPlayerComposable", "Error loading video: ${e.message}", e)
            }
        }
    }

    // Track video position
    var videoPosition by remember { mutableStateOf(0L) }
    var videoDuration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var lastPlayingState by remember { mutableStateOf(false) }
    var isVideoEnded by remember { mutableStateOf(false) }

    // Update position periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            videoPosition = exoPlayer.currentPosition
            videoDuration = exoPlayer.duration.takeIf { it > 0 } ?: 100L
            isPlaying = exoPlayer.isPlaying
            
            // Kiểm tra xem video đã kết thúc chưa
            val currentPlaybackState = exoPlayer.playbackState
            val videoEnded = currentPlaybackState == Player.STATE_ENDED
            
            // Chỉ gọi callback khi trạng thái thay đổi (không gọi liên tục)
            if (isPlaying != lastPlayingState) {
                Log.d("VideoPlayerComposable", "Playback state changed: $lastPlayingState → $isPlaying")
                onPlaybackStateChanged(isPlaying)
                lastPlayingState = isPlaying
            }
            
            // Nếu video kết thúc, không gọi callback nữa
            if (videoEnded && !isVideoEnded) {
                Log.d("VideoPlayerComposable", "Video ended, stopping playback state updates")
                isVideoEnded = true
            }
            
            onPositionChanged(videoPosition, videoDuration)
            
            Log.d("VideoPlayerComposable", "Position: $videoPosition / $videoDuration, Playing: $isPlaying, State: ${when(currentPlaybackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }}")
            
            delay(500)
        }
    }

    // Cleanup
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ExoPlayer View
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color(0xFF1a1a1a))
                    .border(2.dp, Color(0xFF424242))
            )

            // Completion status
            val viewPercentage = if (videoDuration > 0) {
                ((videoPosition.toFloat() / videoDuration) * 100).toInt()
            } else {
                0
            }

            if (viewPercentage >= 98) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B5E20))
                        .border(1.dp, Color(0xFF4CAF50))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓ Bạn đã xem đủ 98% để hoàn thành video này", color = Color(0xFFC8E6C9), fontSize = 12.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF263238))
                        .border(1.dp, Color(0xFF546E7A))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tiến độ: $viewPercentage% (cần 98% để hoàn thành)", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Format milliseconds to MM:SS format
 */
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
