package com.example.datn.presentation.common.profile

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.usecase.student.GetStudentProfileUseCase
import com.example.datn.domain.usecase.student.UpdateStudentProfileParams
import com.example.datn.domain.usecase.student.UpdateStudentProfileUseCase
import com.example.datn.domain.usecase.teacher.GetTeacherProfileUseCase
import com.example.datn.domain.usecase.teacher.UpdateTeacherProfileParams
import com.example.datn.domain.usecase.teacher.UpdateTeacherProfileUseCase
import com.example.datn.domain.usecase.parent.GetParentProfileUseCase
import com.example.datn.domain.usecase.parent.UpdateParentProfileParams
import com.example.datn.domain.usecase.parent.UpdateParentProfileUseCase
import com.example.datn.domain.usecase.minio.MinIOUseCase
import com.example.datn.domain.usecase.user.UpdateUserProfileUseCase
import com.example.datn.domain.usecase.user.UpdateAvatarUseCase
import com.example.datn.domain.usecase.user.GetUserByIdUseCase
import com.example.datn.domain.usecase.avatar.CreateAvatarFolderUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getStudentProfile: GetStudentProfileUseCase,
    private val updateStudentProfile: UpdateStudentProfileUseCase,
    private val getTeacherProfile: GetTeacherProfileUseCase,
    private val updateTeacherProfile: UpdateTeacherProfileUseCase,
    private val getParentProfile: GetParentProfileUseCase,
    private val updateParentProfile: UpdateParentProfileUseCase,
    private val minIOUseCase: MinIOUseCase,
    private val updateUserProfile: UpdateUserProfileUseCase,
    private val updateAvatarUseCase: UpdateAvatarUseCase,
    private val getUserById: GetUserByIdUseCase,
    private val createAvatarFolder: CreateAvatarFolderUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<EditProfileState, EditProfileEvent>(
    initialState = EditProfileState(),
    notificationManager = notificationManager
) {
    
    companion object {
        private const val TAG = "EditProfileViewModel"
    }

    override fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.LoadProfile -> loadProfile(event.userId, event.role)
            is EditProfileEvent.UpdateName -> updateName(event.name)
            is EditProfileEvent.UpdateGradeLevel -> updateGradeLevel(event.gradeLevel)
            is EditProfileEvent.UpdateDateOfBirth -> updateDateOfBirth(event.dateOfBirth)
            is EditProfileEvent.UpdateSpecialization -> updateSpecialization(event.specialization)
            is EditProfileEvent.UpdateLevel -> updateLevel(event.level)
            is EditProfileEvent.UpdateExperienceYears -> updateExperienceYears(event.years)
            is EditProfileEvent.UploadAvatar -> uploadAvatar(event.inputStream, event.fileName, event.fileSize, event.contentType)
            is EditProfileEvent.UpdateAvatarProgress -> updateAvatarProgress(event.uploaded, event.total)
            EditProfileEvent.SaveProfile -> saveProfile()
            EditProfileEvent.ClearMessages -> clearMessages()
        }
    }

    private fun loadProfile(userId: String, role: String) {
        viewModelScope.launch {
            Log.d(TAG, "loadProfile() userId=$userId, role=$role")
            viewModelScope.launch {
                loadUserProfile(userId)
            }
            when (role.uppercase()) {
                UserRole.STUDENT.name -> loadStudentProfile(userId)
                UserRole.TEACHER.name -> loadTeacherProfile(userId)
                UserRole.PARENT.name -> loadParentProfile(userId)
                else -> {
                    setState { copy(error = "Vai trò không hợp lệ") }
                    showNotification("Vai trò không hợp lệ", NotificationType.ERROR)
                }
            }
        }
    }

    private suspend fun loadUserProfile(userId: String) {
        getUserById(userId).collect { result ->
            when (result) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    val user = result.data
                    if (user != null) {
                        setState {
                            copy(
                                user = user,
                                name = user.name,
                                avatarUrl = avatarUrl ?: user.avatarUrl
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    Log.w(TAG, "loadUserProfile() userId=$userId -> Error: ${result.message}")
                }
            }
        }
    }

    private suspend fun loadStudentProfile(studentId: String) {
        getStudentProfile(studentId).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    val student = result.data
                    setState {
                        copy(
                            student = student,
                            gradeLevel = student?.gradeLevel ?: "",
                            dateOfBirth = student?.dateOfBirth,
                            isLoading = false
                        )
                    }
                    student?.userId?.let { loadUserProfile(it) }
                }
                is Resource.Error -> {
                    Log.e(TAG, "loadStudentProfile() studentId=$studentId -> Error: ${result.message}")
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi tải hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private suspend fun loadTeacherProfile(teacherId: String) {
        Log.d(TAG, "loadTeacherProfile() teacherId=$teacherId")
        getTeacherProfile(teacherId).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    Log.d(TAG, "loadTeacherProfile() -> Loading")
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    val teacher = result.data
                    Log.d(TAG, "loadTeacherProfile() -> Success teacher=${teacher?.id}, userId=${teacher?.userId}")
                    setState {
                        copy(
                            teacher = teacher,
                            specialization = teacher?.specialization ?: "",
                            level = teacher?.level ?: "",
                            experienceYears = teacher?.experienceYears?.toString() ?: "0",
                            isLoading = false
                        )
                    }
                    teacher?.userId?.let { loadUserProfile(it) }
                }
                is Resource.Error -> {
                    Log.e(TAG, "loadTeacherProfile() teacherId=$teacherId -> Error: ${result.message}")
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi tải hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private suspend fun loadParentProfile(parentId: String) {
        getParentProfile(parentId).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    setState { copy(parent = result.data, isLoading = false) }
                    result.data?.userId?.let { loadUserProfile(it) }
                }
                is Resource.Error -> {
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi tải hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private fun updateName(name: String) {
        setState { copy(name = name) }
    }

    private fun updateGradeLevel(gradeLevel: String) {
        setState { copy(gradeLevel = gradeLevel) }
    }

    private fun updateDateOfBirth(dateOfBirth: java.time.LocalDate) {
        setState { copy(dateOfBirth = dateOfBirth) }
    }

    private fun updateSpecialization(specialization: String) {
        setState { copy(specialization = specialization) }
    }

    private fun updateLevel(level: String) {
        setState { copy(level = level) }
    }

    private fun updateExperienceYears(years: String) {
        setState { copy(experienceYears = years) }
    }

    private fun saveProfile() {
        val currentState = state.value

        viewModelScope.launch {
            val trimmedName = currentState.name.trim()
            if (trimmedName.isBlank()) {
                val message = "Tên không được để trống"
                setState { copy(error = message, isLoading = false, isSuccess = false) }
                showNotification(message, NotificationType.ERROR)
                return@launch
            }

            if (currentState.teacher != null) {
                val years = currentState.experienceYears.trim().toIntOrNull()
                if (years == null) {
                    val message = "Số năm kinh nghiệm không hợp lệ"
                    setState { copy(error = message, isLoading = false, isSuccess = false) }
                    showNotification(message, NotificationType.ERROR)
                    return@launch
                }
                if (years <= 0) {
                    val message = "Số năm kinh nghiệm không được âm"
                    setState { copy(error = message, isLoading = false, isSuccess = false) }
                    showNotification(message, NotificationType.ERROR)
                    return@launch
                }
            }

            if (currentState.student != null) {
                val dob = currentState.dateOfBirth
                if (dob == null || dob.isAfter(LocalDate.now())) {
                    val message = "Ngày sinh không hợp lệ"
                    setState { copy(error = message, isLoading = false, isSuccess = false) }
                    showNotification(message, NotificationType.ERROR)
                    return@launch
                }
            }

            val userId = extractUserId(currentState)
            val existingUser = currentState.user

            if (userId == null || existingUser == null) {
                val message = "Không tìm thấy thông tin người dùng"
                setState { copy(error = message, isLoading = false, isSuccess = false) }
                showNotification(message, NotificationType.ERROR)
                return@launch
            }

            setState { copy(isLoading = true, error = null, isSuccess = false) }

            val updatedUser = existingUser.copy(
                name = trimmedName,
                avatarUrl = currentState.avatarUrl ?: existingUser.avatarUrl,
                updatedAt = Instant.now()
            )

            var userUpdateFailed = false
            updateUserProfile(userId, updatedUser).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> setState { copy(user = updatedUser) }
                    is Resource.Error -> {
                        userUpdateFailed = true
                        setState { copy(error = result.message, isLoading = false) }
                        showNotification(result.message ?: "Lỗi cập nhật hồ sơ", NotificationType.ERROR)
                    }
                }
            }

            if (userUpdateFailed) return@launch

            when {
                currentState.student != null -> saveStudentProfile()
                currentState.teacher != null -> saveTeacherProfile()
                currentState.parent != null -> saveParentProfile()
                else -> {
                    showNotification("Không tìm thấy hồ sơ", NotificationType.ERROR)
                }
            }
        }
    }

    private fun extractUserId(currentState: EditProfileState): String? {
        return currentState.user?.id
            ?: currentState.student?.userId
            ?: currentState.teacher?.userId
            ?: currentState.parent?.userId
    }

    private suspend fun saveStudentProfile() {
        val currentState = state.value
        val studentId = currentState.student?.id
        
        if (studentId == null) {
            showNotification("Không tìm thấy ID học sinh", NotificationType.ERROR)
            return
        }

        updateStudentProfile(
            UpdateStudentProfileParams(
                studentId = studentId,
                gradeLevel = currentState.gradeLevel.ifBlank { null },
                dateOfBirth = currentState.dateOfBirth
            )
        ).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    setState { copy(isLoading = false, isSuccess = true, error = null) }
                    showNotification(
                        "Cập nhật hồ sơ thành công",
                        NotificationType.SUCCESS
                    )
                }
                is Resource.Error -> {
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi cập nhật hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private suspend fun saveTeacherProfile() {
        val currentState = state.value
        val teacherId = currentState.teacher?.id
        
        if (teacherId == null) {
            showNotification("Không tìm thấy ID giáo viên", NotificationType.ERROR)
            return
        }

        val experienceYears = currentState.experienceYears.toIntOrNull() ?: 0

        updateTeacherProfile(
            UpdateTeacherProfileParams(
                teacherId = teacherId,
                specialization = currentState.specialization.ifBlank { null },
                level = currentState.level.ifBlank { null },
                experienceYears = experienceYears
            )
        ).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    setState { copy(isLoading = false, isSuccess = true, error = null) }
                    showNotification(
                        "Cập nhật hồ sơ thành công",
                        NotificationType.SUCCESS
                    )
                }
                is Resource.Error -> {
                    setState { copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi cập nhật hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private suspend fun saveParentProfile() {
        val currentState = state.value
        val parentId = currentState.parent?.id
        
        if (parentId == null) {
            showNotification("Không tìm thấy ID phụ huynh", NotificationType.ERROR)
            return
        }

        updateParentProfile(
            UpdateParentProfileParams(parentId = parentId)
        ).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    setState { copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    setState { copy(isLoading = false, isSuccess = true, error = null) }
                    showNotification(
                        "Cập nhật hồ sơ thành công",
                        NotificationType.SUCCESS
                    )
                }
                is Resource.Error -> {
                    setState {  copy(error = result.message, isLoading = false) }
                    showNotification(
                        result.message ?: "Lỗi cập nhật hồ sơ",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    private fun clearMessages() {
        setState { copy(error = null, isSuccess = false) }
    }

    private fun uploadAvatar(inputStream: java.io.InputStream, fileName: String, fileSize: Long, contentType: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔹 START uploadAvatar - fileName: $fileName, fileSize: $fileSize bytes")
                setState { copy(isUploadingAvatar = true, error = null) }
                
                // Lấy userId từ profile hiện tại
                val currentState = state.value
                val userId = when {
                    currentState.student != null -> currentState.student.userId
                    currentState.teacher != null -> currentState.teacher.userId
                    currentState.parent != null -> currentState.parent.userId
                    else -> null
                }
                
                Log.d(TAG, "📱 Extracted userId: $userId")
                
                if (userId == null) {
                    Log.e(TAG, "❌ userId is null - cannot proceed")
                    setState { copy(error = "Không tìm thấy ID người dùng", isUploadingAvatar = false) }
                    showNotification("Không tìm thấy ID người dùng", NotificationType.ERROR)
                    return@launch
                }
                
                // 🔹 Tự động tạo thư mục avatar cho user (nếu chưa tồn tại)
                Log.d(TAG, "📁 Creating avatar folder for userId: $userId")
                try {
                    createAvatarFolder(userId)
                    Log.d(TAG, "✅ Avatar folder created successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to create avatar folder, will continue - ${e.message}")
                    // Nếu tạo folder thất bại, vẫn tiếp tục upload
                    // MinIO sẽ tự động tạo folder khi upload file
                }
                
                // 🔹 Xác định extension và MIME type (giống cách lesson content)
                val (extension, mimeType) = when {
                    fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> ".jpg" to "image/jpeg"
                    fileName.endsWith(".png", ignoreCase = true) -> ".png" to "image/png"
                    fileName.endsWith(".gif", ignoreCase = true) -> ".gif" to "image/gif"
                    fileName.endsWith(".webp", ignoreCase = true) -> ".webp" to "image/webp"
                    else -> ".jpg" to "image/jpeg"
                }
                
                // 🔹 Tạo tên file duy nhất: avatars/{userId}/avatar_{timestamp}{extension}
                val objectName = "avatars/$userId/avatar_${System.currentTimeMillis()}$extension"
                
                Log.d(TAG, "📝 Generated objectName: $objectName")
                Log.d(TAG, "🎨 Determined MIME type: $mimeType")
                
                // 🔹 Upload file lên MinIO thư mục avatars
                Log.d(TAG, "⬆️ Uploading file to MinIO - objectName: $objectName, size: $fileSize bytes")
                minIOUseCase.uploadFile(
                    objectName = objectName,
                    inputStream = inputStream,
                    size = fileSize,
                    contentType = mimeType
                )
                Log.d(TAG, "✅ File uploaded successfully to MinIO")
                
                // 🔹 Lấy URL trực tiếp của file từ MinIO
                Log.d(TAG, "🔗 Getting direct URL from MinIO")
                val avatarUrl = minIOUseCase.getDirectFileUrl(objectName)
                Log.d(TAG, "🌍 Avatar URL: $avatarUrl")
                
                setState { copy(avatarUrl = avatarUrl, isUploadingAvatar = false) }
                Log.d(TAG, "✅ State updated with avatarUrl")
                showNotification("Tải lên avatar thành công", NotificationType.SUCCESS)
                Log.d(TAG, "✅ SUCCESS uploadAvatar completed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR uploadAvatar - ${e.message}", e)
                setState { copy(error = e.message ?: "Lỗi tải lên avatar", isUploadingAvatar = false) }
                showNotification(
                    e.message ?: "Lỗi tải lên avatar",
                    NotificationType.ERROR
                )
            }
        }
    }

    private fun updateAvatarProgress(uploaded: Long, total: Long) {
        val progress = if (total > 0) (uploaded.toFloat() / total.toFloat()) else 0f
        val percent = (progress * 100).toInt()
        Log.d(TAG, "📊 Upload progress: $percent% ($uploaded / $total bytes)")
        setState { copy(avatarUploadProgress = progress) }
    }

    fun saveAvatarToProfile(userId: String) {
        Log.d(TAG, "🔹 START saveAvatarToProfile - userId: $userId")
        val avatarUrl = state.value.avatarUrl
        
        if (avatarUrl == null) {
            Log.e(TAG, "❌ avatarUrl is null - cannot save")
            showNotification("Chưa chọn avatar", NotificationType.ERROR)
            return
        }
        
        Log.d(TAG, "💾 Saving avatar URL to profile - avatarUrl: $avatarUrl")

        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, error = null) }
                Log.d(TAG, "⏳ Calling updateAvatarUseCase")
                
                updateAvatarUseCase(userId, avatarUrl).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            Log.d(TAG, "⏳ updateAvatarUseCase - Loading")
                            setState { copy(isLoading = true) }
                        }
                        is Resource.Success -> {
                            Log.d(TAG, "✅ updateAvatarUseCase - Success")
                            setState { copy(isLoading = false, error = null) }
                            showNotification("Cập nhật avatar thành công", NotificationType.SUCCESS)
                            Log.d(TAG, "✅ SUCCESS saveAvatarToProfile completed")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "❌ updateAvatarUseCase - Error: ${result.message}")
                            setState { copy(error = result.message, isLoading = false) }
                            showNotification(
                                result.message ?: "Lỗi cập nhật avatar",
                                NotificationType.ERROR
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR saveAvatarToProfile - ${e.message}", e)
                setState { copy(error = e.message, isLoading = false) }
                showNotification(
                    e.message ?: "Lỗi cập nhật avatar",
                    NotificationType.ERROR
                )
            }
        }
    }
}
