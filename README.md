# DATN (Android)

Ứng dụng Android (Jetpack Compose) hỗ trợ học tập theo mô hình lớp học, nội dung bài học, mini game, bài kiểm tra và nhắn tin giữa các vai trò.

## Công nghệ sử dụng

- **UI**: Jetpack Compose + Material 3
- **Kiến trúc**: MVVM + (Clean-ish) layered architecture (`presentation` → `domain` → `data` → `core`)
- **DI**: Hilt (`@HiltAndroidApp`, `AppModule`)
- **Backend/Storage**:
  - Firebase: Auth, Firestore, Realtime Database, Storage, FCM, Crashlytics, Analytics
  - MinIO: lưu trữ file (ví dụ: avatar, tài liệu bài học)
- **Local cache**: Room
- **Network**: Retrofit + OkHttp (một số feature)

## Các tác nhân (vai trò) và chức năng

Hệ thống chia theo 3 **vai trò chính** (tương ứng điều hướng và UI trong `presentation/teacher`, `presentation/student`, `presentation/parent`).

### 1) Teacher (Giáo viên)

- **Trang chủ**: tổng quan và điều hướng nhanh tới các chức năng
- **Quản lý lớp học**: danh sách lớp, tạo/cập nhật/xóa lớp
- **Quản lý duyệt vào lớp (Enrollment)**: duyệt/từ chối yêu cầu tham gia lớp
- **Quản lý thành viên lớp**: xem danh sách học sinh đã duyệt
- **Chi tiết học sinh**: xem thông tin học sinh theo lớp
- **Quản lý bài học theo lớp**: tạo/cập nhật/xóa bài học trong lớp
- **Quản lý nội dung bài học**:
  - Danh sách nội dung theo bài học
  - Xem chi tiết nội dung (mở tài liệu theo URL)
  - Upload tài liệu (MinIO) khi tạo/cập nhật nội dung
- **Quản lý MiniGame theo bài học**: tạo/cập nhật/xóa mini game, quản lý câu hỏi và đáp án
- **Quản lý bài kiểm tra theo bài học**:
  - Tạo/cập nhật/xóa bài kiểm tra
  - Quản lý câu hỏi và đáp án
  - Xem danh sách bài nộp
  - Chấm bài tự luận
- **Nhắn tin**:
  - Danh sách hội thoại
  - Nhắn tin 1-1 (tạo mới / tiếp tục)
  - Tạo nhóm chat, xem chi tiết nhóm, thêm thành viên
- **Thông báo**: xem danh sách thông báo (màn TeacherNotification)
- **Tài khoản**: đổi mật khẩu, chỉnh sửa hồ sơ (Edit Profile)

### 2) Student (Học sinh)

- **Trang chủ**: tổng quan và điều hướng
- **Lớp học**:
  - Xem danh sách lớp đã tham gia
  - Xin tham gia lớp bằng mã lớp
  - Xem chi tiết lớp
- **Bài học & nội dung**:
  - Xem danh sách nội dung theo bài học
  - Xem nội dung bài học (Lesson View)
- **MiniGame**:
  - Xem danh sách mini game theo bài học
  - Chơi mini game
  - Xem kết quả mini game
- **Bài kiểm tra**:
  - Xem danh sách bài kiểm tra
  - Làm bài
  - Xem kết quả
- **Nhắn tin**:
  - Danh sách hội thoại
  - Nhắn tin với giáo viên (chọn giáo viên để tạo hội thoại mới)
  - Nhắn tin nhóm: tạo nhóm, xem chi tiết nhóm, thêm thành viên
- **Thông báo**: xem danh sách thông báo
- **Tài khoản**: đổi mật khẩu, chỉnh sửa hồ sơ (Edit Profile)

### 3) Parent (Phụ huynh)

- **Trang chủ**: xem nhanh thông tin con và điều hướng
- **Quản lý con em**:
  - Xem danh sách con đã liên kết
  - Cập nhật mối quan hệ / người giám hộ chính
  - Hủy liên kết
- **Liên kết học sinh đã có tài khoản**: tìm kiếm học sinh theo tên và thực hiện liên kết
- **Tạo tài khoản học sinh cho con**: tạo tài khoản học sinh mới và tự động liên kết quan hệ
- **Theo dõi lớp của con**:
  - Xem danh sách lớp của con
  - Xin tham gia lớp cho con
- **Xem chi tiết học sinh** (ParentStudentDetail)
- **Nhắn tin**:
  - Danh sách hội thoại
  - Nhắn tin 1-1 (chọn người nhận)
  - Nhắn tin nhóm: tạo nhóm, xem chi tiết nhóm, thêm thành viên
- **Thông báo**: xem danh sách thông báo
- **Tài khoản**: đổi mật khẩu, chỉnh sửa hồ sơ (Edit Profile)

## Kiến trúc & luồng dữ liệu

### Tổng quan

- **presentation**: UI Compose + ViewModel (state, events, navigation)
- **domain**: models + repository interface + use cases
- **data**: triển khai repository, mapper, Room entities/dao, cơ chế sync
- **core**: nền tảng dùng chung: DI, network services (Firebase/MinIO), utilities, theme

### Luồng chuẩn (gợi ý)

```
UI (Compose Screen)
  → ViewModel
    → UseCase (domain)
      → Repository interface (domain)
        → RepositoryImpl (data)
          → DataSource/Service (core.network) + Room DAO (data.local)
            → Firebase/MinIO/Local DB
```

## Cấu trúc thư mục

Root project (Gradle Kotlin DSL):

```
DATN/
  app/
  build.gradle.kts
  settings.gradle.kts
  gradle/libs.versions.toml
  local.properties              (gitignored)
```

Mã nguồn chính nằm ở:

```
app/src/main/java/com/example/datn/
  App.kt                        # Application (Hilt) + cấu hình XML factory cho MinIO
  MainActivity.kt               # Entry Activity, setContent + AppNavGraph

  core/
    di/                         # Hilt modules (AppModule)
    network/
      datasource/               # FirebaseDataSource, FirebaseAuthDataSource...
      service/                  # UserService, ClassService, LessonService, MinIOService...
    utils/                      # Resource, helpers, network checker...
    theme/                      # Compose theme

  domain/
    models/                     # Entities (User, Class, Lesson, Test, ...)
    repository/                 # Interfaces (IUserRepository, IClassRepository, ...)
    usecase/                    # UseCases theo feature

  data/
    local/                      # Room Database/DAO/Entities
    mapper/                     # Mapper giữa model/db
    repository/impl/            # Repository implementations
    sync/                       # Đồng bộ Firebase ↔ Room (nếu áp dụng)

  presentation/
    navigation/                 # AppNavGraph + Screen routes
    auth/                       # Login/Register/ForgotPassword
    splash/                     # Splash
    teacher/                    # Teacher UI flows
    student/                    # Student UI flows
    parent/                     # Parent UI flows
    common/                     # Component dùng chung (notification, profile, ...)
```

## Cấu hình quan trọng

### 1) Firebase

Repo đã có `app/google-services.json`. Bạn cần:

- **Mở project bằng Android Studio**
- Đảm bảo project Firebase của bạn khớp `applicationId`: `com.example.datn`
- Bật các dịch vụ đang dùng: Auth, Firestore, Storage, FCM, ...

### 2) MinIO (local)

Project đọc cấu hình MinIO từ `local.properties` để build `BuildConfig`:

- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET`

Vì `local.properties` bị `.gitignore`, bạn cần tự tạo/điền.

Ví dụ `local.properties` (ở **root project** `DATN/`):

```properties
sdk.dir=C\\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk

# MinIO
MINIO_ENDPOINT=http://10.0.2.2:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=datn
```

Ghi chú:

- Nếu chạy trên **Android Emulator**, `10.0.2.2` trỏ về máy host.
- Nếu chạy trên **điện thoại thật**, dùng IP LAN của máy chạy MinIO (ví dụ `http://192.168.1.10:9000`).
- App đã bật `usesCleartextTraffic=true` nên có thể dùng `http://`.

## Hướng dẫn chạy project

### Yêu cầu

- Android Studio (khuyến nghị bản mới)
- JDK 11 (project đặt `jvmTarget = 11`)
- Android SDK (compile/target SDK 36)
- Docker Desktop (nếu muốn chạy MinIO bằng docker)

### Bước 1: Mở project

- Mở thư mục: `DATN/DATN` (thư mục có `settings.gradle.kts`)
- Sync Gradle

### Bước 2: Chạy MinIO (tuỳ chọn nhưng khuyến nghị nếu feature upload dùng MinIO)

Trong repo có sẵn docker-compose tại:

- `app/src/main/java/com/example/datn/docker/docker-compose.yml`

Chạy MinIO (ví dụ):

```bash
docker compose up -d
```

Sau khi chạy:

- API: `http://localhost:9000`
- Console: `http://localhost:9001`
- User/Password mặc định theo compose: `minioadmin` / `minioadmin`

### Bước 3: Cấu hình `local.properties`

- Điền `MINIO_*` như phần trên
- Sync lại Gradle để `BuildConfig.MINIO_*` có giá trị

### Bước 4: Run app

- Chọn device/emulator
- Run module `app`

Entry point:

- `AndroidManifest.xml` → `MainActivity`
- `MainActivity` → `AppNavGraph` (startDestination: `splash`)

## Troubleshooting

- **Upload MinIO lỗi do endpoint**
  - Emulator: dùng `http://10.0.2.2:9000`
  - Device thật: dùng `http://<LAN_IP>:9000`

- **Bucket không tồn tại**
  - `MinIOService` có cơ chế tự tạo bucket khi khởi tạo (nếu credentials đủ quyền).

- **Firebase lỗi cấu hình**
  - Kiểm tra `google-services.json` đúng project
  - Kiểm tra `applicationId` là `com.example.datn`

---
