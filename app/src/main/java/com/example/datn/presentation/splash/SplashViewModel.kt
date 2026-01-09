package com.example.datn.presentation.splash

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.BuildConfig
import com.example.datn.core.base.BaseViewModel
import com.example.datn.data.remote.service.minio.MinIOService
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.usecase.splash.SplashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val splashUseCase: SplashUseCase,
    private val notificationManager: NotificationManager
) : BaseViewModel<SplashState, SplashEvent>(SplashState() , notificationManager) {

    init {
        // 1. Kiểm tra MinIO
//        testMinIOConnection()
        onEvent(SplashEvent.CheckCurrentUser)
    }

    private fun checkCurrentUser() {
        viewModelScope.launch{
            delay(300)
            splashUseCase().onEach { result ->
                when (result) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true, error = null) }
                    }

                    is Resource.Success -> {
                        val user = result.data
                        setState { copy(isLoading = false, user = user, error = null) }

                        if (user != null) {
                            sendEvent(SplashEvent.NavigateToHome(user.role))
                        } else {
                            sendEvent(SplashEvent.NavigateToLogin)
                        }
                    }

                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        sendEvent(SplashEvent.NavigateToLogin)
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
//    private fun testMinIOConnection() {
//        viewModelScope.launch {
//            withContext(Dispatchers.IO) {
//                try {
//                    val bucketName = MinIOConfig.bucketName
//                    Log.d("SplashViewModel", "Testing MinIO connection to bucket: $bucketName")
//                    Log.d("SplashViewModel", "➡️ Endpoint: ${BuildConfig.MINIO_ENDPOINT}")
//                    Log.d("SplashViewModel", "➡️ Bucket: $bucketName")
//
//                    val bucketExists = client.bucketExists(
//                        BucketExistsArgs.builder()
//                            .bucket(bucketName)
//                            .build()
//                    )
//                    if (!bucketExists) {
//                        Log.w("SplashViewModel", "⚠️ Bucket '$bucketName' không tồn tại. Tạo mới...")
//                        client.makeBucket(
//                            MakeBucketArgs.builder()
//                                .bucket(bucketName)
//                                .build()
//                        )
//                    } else {
//                        Log.d("SplashViewModel", "✅ Bucket '$bucketName' đã tồn tại")
//                    }
//
//                    // Tạo file thử nghiệm nhỏ
//                    val testFileName = "minio_test.txt"
//                    val testContent = "Hello MinIO!".byteInputStream()
//                    val size = testContent.available().toLong()
//
//                    // Upload file thử nghiệm
//                    MinIOService.uploadFile(testFileName, testContent, size, "text/plain")
//
//                    Log.d("SplashViewModel", "✅ Uploaded test file to MinIO")
//
//                    // Kiểm tra file tồn tại
//                    val exists = MinIOService.fileExists(testFileName)
//                    Log.d("SplashViewModel", "MinIO file '$testFileName' exists: $exists")
//
////                    // Xóa file thử nghiệm
//                    MinIOService.deleteFile(testFileName)
//                    Log.d("SplashViewModel", "🗑️ Deleted test file from MinIO")
//
//                } catch (e: Exception) {
//                    Log.e("SplashViewModel", "❌ Lỗi kết nối MinIO: ${e.message}")
//                }
//            }
//        }
//    }


    override fun onEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.CheckCurrentUser -> checkCurrentUser()
            else -> Unit
        }
    }
}
