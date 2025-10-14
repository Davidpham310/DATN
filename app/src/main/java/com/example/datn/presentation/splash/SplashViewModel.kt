package com.example.datn.presentation.splash

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.BuildConfig
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.network.config.MinIOConfig
import com.example.datn.core.network.config.MinIOConfig.client
import com.example.datn.core.network.service.minio.MinIOService
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.usecase.splash.SplashUseCase
import com.example.datn.presentation.common.splash.SplashEvent
import com.example.datn.presentation.common.splash.SplashState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val splashUseCase: SplashUseCase
) : BaseViewModel<SplashState, SplashEvent>(SplashState()) {

    init {
        // 1. Kiểm tra MinIO
        testMinIOConnection()
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            splashUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }

                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        val user = result.data
                        if (user != null) {
                            sendEvent(SplashEvent.NavigateToHome(user.role))
                        } else {
                            sendEvent(SplashEvent.NavigateToLogin)
                        }
                    }

                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        sendEvent(SplashEvent.NavigateToLogin)
                    }
                }
            }
        }
    }
    private fun testMinIOConnection() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val bucketName = MinIOConfig.bucketName
                    Log.d("SplashViewModel", "Testing MinIO connection to bucket: $bucketName")
                    Log.d("SplashViewModel", "➡️ Endpoint: ${BuildConfig.MINIO_ENDPOINT}")
                    Log.d("SplashViewModel", "➡️ Bucket: $bucketName")

                    val bucketExists = client.bucketExists(
                        BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
                    )
                    if (!bucketExists) {
                        Log.w("SplashViewModel", "⚠️ Bucket '$bucketName' không tồn tại. Tạo mới...")
                        client.makeBucket(
                            MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                        )
                    } else {
                        Log.d("SplashViewModel", "✅ Bucket '$bucketName' đã tồn tại")
                    }

                    // Tạo file thử nghiệm nhỏ
                    val testFileName = "minio_test.txt"
                    val testContent = "Hello MinIO!".byteInputStream()
                    val size = testContent.available().toLong()

                    // Upload file thử nghiệm
                    MinIOService.uploadFile(testFileName, testContent, size, "text/plain")

                    Log.d("SplashViewModel", "✅ Uploaded test file to MinIO")

                    // Kiểm tra file tồn tại
                    val exists = MinIOService.fileExists(testFileName)
                    Log.d("SplashViewModel", "MinIO file '$testFileName' exists: $exists")

//                    // Xóa file thử nghiệm
                    MinIOService.deleteFile(testFileName)
                    Log.d("SplashViewModel", "🗑️ Deleted test file from MinIO")

                } catch (e: Exception) {
                    Log.e("SplashViewModel", "❌ Lỗi kết nối MinIO: ${e.message}")
                }
            }
        }
    }


    override fun onEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.CheckCurrentUser -> checkCurrentUser()
            else -> Unit
        }
    }
}
