package com.example.datn.presentation.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState

@Composable
fun <T> ConfirmationDialog(
    state: ConfirmationDialogState<T>,
    confirmText: String = "Xác nhận",
    dismissText: String = "Hủy",
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit,
) {
    // Chỉ hiển thị nếu cờ isShowing là TRUE và dữ liệu tồn tại
    if (state.isShowing && state.data != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(state.title) },
            text = {
                Text(text = state.message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(state.data) // Truyền dữ liệu trở lại ViewModel
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(dismissText)
                }
            }
        )
    }
}