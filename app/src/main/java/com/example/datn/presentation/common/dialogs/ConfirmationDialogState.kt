package com.example.datn.presentation.common.dialogs

/**
 * Trạng thái dùng chung cho mọi hộp thoại xác nhận.
 * @param isShowing Cờ xác định hộp thoại có đang hiển thị hay không.
 * @param title Tiêu đề của hộp thoại (Ví dụ: "Xác nhận xóa").
 * @param message Nội dung chính cần xác nhận.
 * @param data Đối tượng (hoặc ID) cần được xác nhận, có thể là null.
 */
data class ConfirmationDialogState<T>(
    val isShowing: Boolean = false,
    val title: String = "",
    val message: String = "",
    val data: T? = null
) {
    // Trạng thái rỗng/mặc định để reset
    companion object {
        fun <T> empty() = ConfirmationDialogState<T>()
    }
}