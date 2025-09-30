package com.example.datn.core.utils

/**
 * Lớp `Resource` là một lớp sealed (sealed class) được sử dụng để quản lý trạng thái của các hoạt động bất đồng bộ,
 * chẳng hạn như gọi API mạng hoặc truy vấn cơ sở dữ liệu. Nó giúp đóng gói dữ liệu cùng với trạng thái của nó.
 *
 * @param T Kiểu dữ liệu (generic type) của dữ liệu được trả về khi thành công.
 *
 * Các trạng thái có thể có:
 * - `Loading`: Biểu thị rằng hoạt động đang được thực hiện. Thường được sử dụng để hiển thị một chỉ báo tải (loading spinner).
 * - `Success`: Biểu thị rằng hoạt động đã hoàn thành thành công. Nó chứa dữ liệu kết quả (`data`) có kiểu `T`.
 * - `Error`: Biểu thị rằng đã xảy ra lỗi trong quá trình thực hiện. Nó chứa một thông báo lỗi (`message`) kiểu `String` để
 *   giúp gỡ lỗi hoặc hiển thị cho người dùng.
 *
 * Việc sử dụng sealed class đảm bảo rằng chúng ta phải xử lý tất cả các trạng thái (Loading, Success, Error) khi sử dụng biểu thức `when`, giúp mã nguồn an toàn và dễ quản lý hơn.
 */
sealed class Resource<out T> {
    class Loading<out T> : Resource<T>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error<out T>(val message: String) : Resource<T>()
}