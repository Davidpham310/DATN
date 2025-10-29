package com.example.datn.presentation.teacher.minigame.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.datn.domain.models.MiniGameOption

@Composable
fun MiniGameOptionList(
    options: List<MiniGameOption>,
    onEdit: (MiniGameOption) -> Unit,
    onDelete: (MiniGameOption) -> Unit,
    onClick: (MiniGameOption) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có đáp án", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(options, key = { it.id }) { option ->
            MiniGameOptionItem(
                option = option,
                onEdit = { onEdit(option) },
                onDelete = { onDelete(option) },
                onClick = { onClick(option) }
            )
        }
    }
}


