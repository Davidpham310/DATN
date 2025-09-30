package com.example.datn.core.base

import com.example.datn.core.utils.AppException
/**
 * Lớp trừu tượng cơ sở cho các nguồn dữ liệu (Data Source).
 * Mục đích chính của lớp này là cung cấp một cơ chế chung để xử lý lỗi một cách an toàn
 * khi thực hiện các cuộc gọi mạng hoặc truy vấn cơ sở dữ liệu.
 */
abstract class BaseDataSource {
    /**
     * Hàm `safeCall` là một hàm bậc cao (higher-order function) dùng để bọc các khối lệnh bất đồng bộ (suspend block).
     *
     * @param block Khối lệnh (lambda function) bất đồng bộ cần được thực thi. Đây thường là một cuộc gọi API hoặc một thao tác với cơ sở dữ liệu.
     * @return Trả về kết quả của khối lệnh nếu thực thi thành công.
     * @throws AppException Ném ra một ngoại lệ tùy chỉnh `AppException` nếu có bất kỳ lỗi nào xảy ra trong quá trình thực thi khối lệnh.
     * Điều này giúp đồng nhất cách xử lý lỗi trên toàn bộ ứng dụng.
     */
    protected suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            throw AppException(e.message ?: "Data source error")
        }
    }

    protected suspend fun <T> safeCallWithResult(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(AppException(e.message ?: "Data source error"))
        }
    }
}