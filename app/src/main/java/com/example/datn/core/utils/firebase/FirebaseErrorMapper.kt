package com.example.datn.core.utils.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*

object FirebaseErrorMapper {
    fun getErrorMessage(e: Throwable): String {  // <-- kiểu là Throwable
        return when (e) {
            is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu. Vui lòng chọn mật khẩu mạnh hơn."
            is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu không hợp lệ."
            is FirebaseAuthUserCollisionException -> "Email này đã được sử dụng cho tài khoản khác."
            is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa."
            is FirebaseNetworkException -> "Không có kết nối mạng. Vui lòng kiểm tra Internet."
            is FirebaseAuthException -> when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email đã được sử dụng cho tài khoản khác."
                "ERROR_INVALID_EMAIL" -> "Địa chỉ email không hợp lệ."
                "ERROR_USER_NOT_FOUND" -> "Không tìm thấy người dùng."
                "ERROR_WRONG_PASSWORD" -> "Sai mật khẩu, vui lòng thử lại."
                "ERROR_TOO_MANY_REQUESTS" -> "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau."
                else -> "Đã xảy ra lỗi: ${e.errorCode}"
            }
            else -> e.message ?: "Đã xảy ra lỗi không xác định."
        }
    }
}
