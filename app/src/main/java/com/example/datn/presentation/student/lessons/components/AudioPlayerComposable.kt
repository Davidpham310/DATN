package com.example.datn.presentation.student.lessons.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
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
import kotlinx.coroutines.delay

/**
 * Audio Player Component sử dụng ExoPlayer
 * @param audioUrl: URL của audio
 * @param title: Tiêu đề audio
 * @param onPositionChanged: Callback khi vị trí audio thay đổi (position, duration)
 * @param onPlaybackStateChanged: Callback khi trạng thái phát lại thay đổi
 * @param isLoading: Trạng thái đang tải
 * @param error: Thông báo lỗi nếu có
 */
@Composable
fun AudioPlayerComposable(
    audioUrl: String,
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
                .height(150.dp)
                .background(Color(0xFF1a1a1a)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Đang tải audio...", color = Color.White)
            }
        }
        return
    }

    if (error != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color(0xFF330000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("✗ Lỗi tải audio", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = Color.Red, fontSize = 12.sp)
            }
        }
        return
    }

    if (audioUrl.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color(0xFF1a1a1a)),
            contentAlignment = Alignment.Center
        ) {
            Text("Chưa tải audio", color = Color.White)
        }
        return
    }

    // ExoPlayer instance
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Update media item when URL changes
    LaunchedEffect(audioUrl) {
        if (audioUrl.isNotEmpty()) {
            try {
                Log.d("AudioPlayerComposable", "Loading audio: $audioUrl")
                val mediaItem = MediaItem.fromUri(audioUrl)
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
                        Log.d("AudioPlayerComposable", "Playback state changed: $stateName")
                        if (playbackState == Player.STATE_READY) {
                            exoPlayer.play()
                            Log.d("AudioPlayerComposable", "Audio is ready, starting playback")
                        }
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("AudioPlayerComposable", "Player error: ${error.message}", error)
                        Log.e("AudioPlayerComposable", "Error code: ${error.errorCode}")
                        Log.e("AudioPlayerComposable", "Cause: ${error.cause}")
                    }
                }
                exoPlayer.addListener(listener)
                
                // Try to play immediately
                delay(500) // Wait for prepare to complete
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    exoPlayer.play()
                    Log.d("AudioPlayerComposable", "Audio ready, playing now")
                } else {
                    Log.d("AudioPlayerComposable", "Audio not ready yet, state: ${exoPlayer.playbackState}")
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerComposable", "Error loading audio: ${e.message}", e)
            }
        }
    }

    // Track audio position
    var audioPosition by remember { mutableStateOf(0L) }
    var audioDuration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    // Update position periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            audioPosition = exoPlayer.currentPosition
            audioDuration = exoPlayer.duration.takeIf { it > 0 } ?: 100L
            isPlaying = exoPlayer.isPlaying
            
            onPositionChanged(audioPosition, audioDuration)
            onPlaybackStateChanged(isPlaying)
            
            Log.d("AudioPlayerComposable", "Position: $audioPosition / $audioDuration, Playing: $isPlaying")
            
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
            .background(Color(0xFF1a1a1a))
            .border(1.dp, Color(0xFF424242))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Audio icon and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Audio",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "🔊 Audio",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        title,
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            // Time display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${formatTime(audioPosition)}", color = Color.White, fontSize = 12.sp)
                Text("${formatTime(audioDuration)}", color = Color.White, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress slider
            Slider(
                value = audioPosition.toFloat(),
                onValueChange = {
                    exoPlayer.seekTo(it.toLong())
                    audioPosition = it.toLong()
                },
                valueRange = 0f..audioDuration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Play/Pause button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Completion status
            val listenPercentage = if (audioDuration > 0) {
                ((audioPosition.toFloat() / audioDuration) * 100).toInt()
            } else {
                0
            }

            if (listenPercentage >= 98) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B5E20))
                        .border(1.dp, Color(0xFF4CAF50))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓ Bạn đã nghe đủ 98% để hoàn thành audio này", color = Color(0xFFC8E6C9), fontSize = 12.sp)
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
                    Text("Tiến độ: $listenPercentage% (cần 98% để hoàn thành)", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                }
            }
        }
    }
}
