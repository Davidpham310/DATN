package com.example.datn.presentation.student.games.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.Level
import com.example.datn.presentation.student.games.viewmodel.MiniGameListViewModel
import com.example.datn.presentation.student.games.event.MiniGameListEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGameListScreen(
    lessonId: String? = null,
    lessonTitle: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToGame: (String) -> Unit,
    viewModel: MiniGameListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(lessonId) {
        if (lessonId != null && lessonId.isNotBlank()) {
            android.util.Log.d("MiniGameListScreen", "🎮 Loading minigames for lesson: $lessonId")
            viewModel.onEvent(MiniGameListEvent.LoadMiniGamesByLesson(lessonId, lessonTitle))
        } else {
            android.util.Log.w("MiniGameListScreen", "⚠️ No lessonId provided - minigames should only be accessed from lessons")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = if (state.lessonTitle != null) "Trò chơi - ${state.lessonTitle}" else "Trò chơi nhỏ",
                            fontWeight = FontWeight.Bold
                        )
                        if (state.lessonTitle != null) {
                            Text(
                                text = "${state.miniGames.size} trò chơi có sẵn",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Đang tải trò chơi...")
                    }
                }
            }
            
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { 
                                if (lessonId != null && lessonId.isNotBlank()) {
                                    viewModel.onEvent(MiniGameListEvent.LoadMiniGamesByLesson(lessonId, lessonTitle))
                                }
                            }
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            
            state.miniGames.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Games,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Chưa có trò chơi nào",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Hãy quay lại sau để khám phá các trò chơi mới!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.miniGames) { miniGame ->
                        MiniGameCard(
                            miniGame = miniGame,
                            onPlayGame = { onNavigateToGame(miniGame.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniGameCard(
    miniGame: MiniGame,
    onPlayGame: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPlayGame,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = miniGame.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                LevelBadge(level = miniGame.level)
            }
            
            if (miniGame.description.isNotEmpty()) {
                Text(
                    text = miniGame.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val gameIcon = when (miniGame.gameType) {
                        com.example.datn.domain.models.GameType.QUIZ -> Icons.Default.Quiz
                        com.example.datn.domain.models.GameType.PUZZLE -> Icons.Default.Extension
                        com.example.datn.domain.models.GameType.MATCHING -> Icons.Default.SwapHoriz
                    }
                    
                    Icon(
                        gameIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = miniGame.gameType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Button(
                    onClick = onPlayGame,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chơi")
                }
            }
        }
    }
}

@Composable
private fun LevelBadge(level: Level) {
    val (color, text) = when (level) {
        Level.EASY -> MaterialTheme.colorScheme.tertiary to "Dễ"
        Level.MEDIUM -> MaterialTheme.colorScheme.primary to "Trung bình"
        Level.HARD -> MaterialTheme.colorScheme.error to "Khó"
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
