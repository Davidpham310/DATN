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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.Level
import com.example.datn.domain.models.MiniGame
import com.example.datn.core.utils.validation.rules.minigame.ValidateMiniGameTitle
import com.example.datn.core.utils.validation.rules.minigame.ValidateMiniGameDescription

@Composable
fun AddEditMiniGameDialog(
    game: MiniGame?,
    onDismiss: () -> Unit,
    onConfirmAdd: (title: String, description: String, level: Level) -> Unit,
    onConfirmEdit: (id: String, title: String, description: String, level: Level) -> Unit
) {
    val isEditing = game != null

    var title by remember { mutableStateOf(game?.title ?: "") }
    var description by remember { mutableStateOf(game?.description ?: "") }
    var selectedLevel by remember { mutableStateOf(game?.level ?: Level.EASY) }

    val titleValidator = remember { ValidateMiniGameTitle() }
    val descriptionValidator = remember { ValidateMiniGameDescription() }

    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var isLevelExpanded by remember { mutableStateOf(false) }

    fun validateFields(): Boolean {
        val titleResult = titleValidator.validate(title)
        titleError = if (!titleResult.successful) titleResult.errorMessage else null

        val descriptionResult = descriptionValidator.validate(description)
        descriptionError = if (!descriptionResult.successful) descriptionResult.errorMessage else null

        return titleError == null && descriptionError == null
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
                    onValueChange = {
                        description = it
                        if (descriptionError != null) descriptionError = null
                    },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = descriptionError != null,
                    supportingText = {
                        descriptionError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    minLines = 3,
                    maxLines = 5
                )

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
                                selectedLevel
                            )
                        } else {
                            onConfirmAdd(
                                title,
                                description,
                                selectedLevel
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
