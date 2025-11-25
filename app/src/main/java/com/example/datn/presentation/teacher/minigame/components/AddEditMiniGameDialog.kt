package com.example.datn.presentation.teacher.minigame.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.GameType
import com.example.datn.domain.models.Level
import com.example.datn.domain.models.MiniGame
import com.example.datn.core.utils.validation.rules.minigame.ValidateMiniGameTitle

@Composable
fun AddEditMiniGameDialog(
    game: MiniGame?,
    onDismiss: () -> Unit,
    onConfirmAdd: (title: String, description: String, gameType: GameType, level: Level, contentUrl: String?) -> Unit,
    onConfirmEdit: (id: String, title: String, description: String, gameType: GameType, level: Level, contentUrl: String?) -> Unit
) {
    val isEditing = game != null

    var title by remember { mutableStateOf(game?.title ?: "") }
    var description by remember { mutableStateOf(game?.description ?: "") }
    var selectedGameType by remember { mutableStateOf(game?.gameType ?: GameType.QUIZ) }
    var selectedLevel by remember { mutableStateOf(game?.level ?: Level.EASY) }
    var contentUrl by remember { mutableStateOf(game?.contentUrl ?: "") }

    val titleValidator = remember { ValidateMiniGameTitle() }

    var titleError by remember { mutableStateOf<String?>(null) }
    var isGameTypeExpanded by remember { mutableStateOf(false) }
    var isLevelExpanded by remember { mutableStateOf(false) }

    fun validateFields(): Boolean {
        val titleResult = titleValidator.validate(title)
        titleError = if (!titleResult.successful) titleResult.errorMessage else null
        return titleError == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Chỉnh sửa mini game" else "Thêm mini game",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError != null) titleError = null
                    },
                    label = { Text("Tiêu đề *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = titleError != null,
                    supportingText = {
                        titleError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Game Type Dropdown
                Box {
                    OutlinedTextField(
                        value = selectedGameType.displayName,
                        onValueChange = {},
                        label = { Text("Loại game *") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                Modifier.clickable { isGameTypeExpanded = !isGameTypeExpanded }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGameTypeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = isGameTypeExpanded,
                        onDismissRequest = { isGameTypeExpanded = false }
                    ) {
                        GameType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedGameType = type
                                    isGameTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Level Dropdown
                Box {
                    OutlinedTextField(
                        value = selectedLevel.displayName,
                        onValueChange = {},
                        label = { Text("Độ khó *") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                Modifier.clickable { isLevelExpanded = !isLevelExpanded }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLevelExpanded = true }
                    )
                    DropdownMenu(
                        expanded = isLevelExpanded,
                        onDismissRequest = { isLevelExpanded = false }
                    ) {
                        Level.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.displayName) },
                                onClick = {
                                    selectedLevel = level
                                    isLevelExpanded = false
                                }
                            )
                        }
                    }
                }

                // Content URL (optional)
                OutlinedTextField(
                    value = contentUrl,
                    onValueChange = { contentUrl = it },
                    label = { Text("Link nội dung (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            "URL đến tài nguyên bổ sung",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateFields()) {
                        if (isEditing) {
                            onConfirmEdit(
                                game!!.id,
                                title,
                                description,
                                selectedGameType,
                                selectedLevel,
                                contentUrl.ifBlank { null }
                            )
                        } else {
                            onConfirmAdd(
                                title,
                                description,
                                selectedGameType,
                                selectedLevel,
                                contentUrl.ifBlank { null }
                            )
                        }
                    }
                }
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
