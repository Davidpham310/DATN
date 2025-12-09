package com.example.datn.presentation.student.lessons.managers

/**
 * Cấu hình tập trung cho hệ thống giám sát quá trình học tập
 * Dễ dàng tùy chỉnh các thông số mà không cần sửa code logic
 *
 * 📋 QUY TẮC HOÀN THÀNH NỘI DUNG:
 * ================================
 *
 * 1️⃣ TEXT (Văn bản):
 *    - Yêu cầu: Xem ≥ 5 giây
 *    - Phát hiện: Tự động ghi nhận sau 5s
 *    - Không cần tương tác
 *
 * 2️⃣ IMAGE (Hình ảnh):
 *    - Yêu cầu: Xem ≥ 5 giây
 *    - Phát hiện: Tự động ghi nhận sau 5s
 *    - Không cần tương tác
 *
 * 3️⃣ PDF (Tài liệu):
 *    - Yêu cầu: Cuộn ≥ 95% + xem ≥ 5 giây
 *    - Phát hiện: Theo dõi thanh cuộn
 *    - Không cần tương tác
 *
 * 4️⃣ VIDEO (Video):
 *    - Yêu cầu: Xem ≥ 98% + KHÔNG tua nhanh
 *    - Phát hiện: Theo dõi vị trí phát + phát hiện nhảy > 5s
 *    - Nếu tua nhanh: Không được hoàn thành
 *    - Bắt buộc có tương tác (play/pause)
 *
 * 5️⃣ AUDIO (Âm thanh):
 *    - Yêu cầu: Nghe ≥ 98% + KHÔNG tua nhanh
 *    - Phát hiện: Theo dõi vị trí phát + phát hiện nhảy > 5s
 *    - Nếu tua nhanh: Không được hoàn thành
 *    - Bắt buộc có tương tác (play/pause)
 *
 * ⚠️ PHÁT HIỆN TUA NHANH:
 * - Nếu nhảy > 5 giây trong video/audio → đánh dấu là tua nhanh
 * - Nội dung tua nhanh KHÔNG được hoàn thành
 * - Phải xem lại từ đầu hoặc từ điểm hiện tại
 *
 * 🔄 CẬP NHẬT FIREBASE:
 * - Tự động lưu mỗi 10 giây
 * - Lưu ngay khi nội dung hoàn thành
 * - Lưu khi chuyển sang nội dung khác
 * - Lưu khi kết thúc phiên học
 * - Retry tự động nếu lỗi mạng
 *
 * 📱 XỬ LÝ BACKGROUND/SCREEN OFF:
 * - Tự động tạm dừng khi app vào background
 * - Lưu tiến độ khẩn cấp trước khi thoát
 * - Tự động thoát khi màn hình tắt (có thể cấu hình)
 * - Giới hạn thời gian ở background
 */
object LearningProgressConfig {
    // ===== Thời gian tối thiểu xem nội dung (giây) =====
    const val TEXT_MIN_VIEW_TIME_SECONDS = 5
    const val IMAGE_MIN_VIEW_TIME_SECONDS = 5
    const val PDF_MIN_VIEW_TIME_SECONDS = 5

    // ===== Yêu cầu hoàn thành nội dung =====
    const val PDF_MIN_SCROLL_PERCENTAGE = 95  // Phải xem đến 95% trang PDF
    const val VIDEO_MIN_COMPLETION_PERCENTAGE = 98  // Phải xem 98% video
    const val AUDIO_MIN_COMPLETION_PERCENTAGE = 98  // Phải nghe 98% audio

    // ===== Giám sát không hoạt động =====
    const val INACTIVITY_WARNING_TIMEOUT_SECONDS = 60  // Cảnh báo sau 60s không hoạt động
    const val MAX_INACTIVITY_WARNINGS = 3  // Tối đa 3 cảnh báo

    // ===== Reset Warning Count khi có tương tác =====
    const val RESET_WARNING_ON_INTERACTION = true  // Cho phép reset warning count khi tương tác
    const val RESET_WARNING_ON_ANY_INTERACTION = true  // Reset khi có BẤT KỲ tương tác nào (không cần kiểm tra loại)
    const val RESET_WARNING_INTERACTION_TYPES = "CLICK,SCROLL,SWIPE,TAP,LONG_PRESS,TEXT_INPUT,MEDIA_PLAY,MEDIA_PAUSE,NAVIGATION"  // Các loại tương tác được phép reset (nếu RESET_WARNING_ON_ANY_INTERACTION = false)
    const val PARTIAL_RESET_WARNING = false  // Nếu true: giảm 1 warning, false: reset về 0
    const val MIN_INTERACTION_INTERVAL_MS = 500L  // Khoảng cách tối thiểu giữa các tương tác (tránh spam)

    // ===== Tự động lưu tiến độ =====
    const val AUTO_SAVE_INTERVAL_SECONDS = 10  // Lưu mỗi 10 giây

    // ===== Phát hiện tua nhanh video =====
    const val VIDEO_FAST_FORWARD_THRESHOLD_MS = 5000  // Nếu nhảy > 5s là tua nhanh

    // ===== Yêu cầu tương tác =====
    const val REQUIRE_INTERACTION_FOR_VIDEO = true  // Bắt buộc có tương tác khi xem video
    const val REQUIRE_INTERACTION_FOR_AUDIO = true  // Bắt buộc có tương tác khi nghe audio

    // ===== Giám sát vòng đời ứng dụng =====
    const val APP_BACKGROUND_TIMEOUT_MS = 30000L  // Thoát nếu ở nền > 30 giây
    const val SCREEN_OFF_AUTO_EXIT = true  // Tự động thoát khi màn hình tắt

    // ===== Xử lý khi chạy nền (Background) =====
    const val SAVE_PROGRESS_ON_BACKGROUND = true  // Lưu tiến độ khi vào background
    const val PAUSE_MEDIA_ON_BACKGROUND = true  // Tạm dừng video/audio khi vào background
    const val BACKGROUND_GRACE_PERIOD_MS = 5000L  // Thời gian chờ trước khi bắt đầu tính background (5s)
    const val BACKGROUND_WARNING_THRESHOLD_MS = 15000L  // Hiển thị cảnh báo sau 15s ở background

    // ===== Xử lý khi tắt màn hình/nguồn =====
    const val SAVE_PROGRESS_ON_SCREEN_OFF = true  // Lưu tiến độ khi màn hình tắt
    const val SCREEN_OFF_GRACE_PERIOD_MS = 3000L  // Thời gian chờ trước khi xử lý screen off (3s)
    const val AUTO_EXIT_ON_POWER_OFF = true  // Tự động thoát khi pin yếu (<5%)
    const val LOW_BATTERY_THRESHOLD = 5  // Ngưỡng pin yếu (%)

    // ===== Emergency Save =====
    const val EMERGENCY_SAVE_RETRY_COUNT = 3  // Số lần thử lưu khẩn cấp
    const val EMERGENCY_SAVE_RETRY_DELAY_MS = 1000L  // Thời gian chờ giữa các lần retry (1s)

    // ===== Session Recovery =====
    const val ENABLE_SESSION_RECOVERY = true  // Cho phép khôi phục phiên học
    const val SESSION_RECOVERY_TIMEOUT_MS = 300000L  // Timeout khôi phục phiên (5 phút)
}
