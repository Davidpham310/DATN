package com.example.datn.presentation.parent.studentprofile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.domain.usecase.parentstudent.ParentStudentUseCases
import com.example.datn.domain.usecase.progress.GetStudentDashboardUseCase
import com.example.datn.domain.usecase.progress.GetStudyTimeStatisticsUseCase
import com.example.datn.domain.usecase.progress.GetStudentAllLessonProgressUseCase
import com.example.datn.domain.usecase.progress.StudentDashboard
import com.example.datn.domain.usecase.progress.StudentLessonProgressItem
import com.example.datn.domain.models.StudyTimeStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentDetailState(
    val isLoading: Boolean = false,
    val studentInfo: LinkedStudentInfo? = null,
    val dashboard: StudentDashboard? = null,
    val studyTime: StudyTimeStatistics? = null,
    val lessonProgressItems: List<StudentLessonProgressItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val parentStudentUseCases: ParentStudentUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentDashboard: GetStudentDashboardUseCase,
    private val getStudyTimeStatistics: GetStudyTimeStatisticsUseCase,
    private val getStudentAllLessonProgress: GetStudentAllLessonProgressUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StudentDetailState())
    val state: StateFlow<StudentDetailState> = _state.asStateFlow()
    
    // Giống như ClassManagerViewModel - tạo StateFlow cho parentId
    private val currentParentIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun loadStudentDetail(studentId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                // Lấy parent ID từ StateFlow (đợi giá trị hợp lệ)
                var parentId = currentParentIdFlow.value
                if (parentId.isBlank()) {
                    currentParentIdFlow
                        .filter { it.isNotBlank() }
                        .take(1)
                        .collect { id ->
                            parentId = id
                        }
                }
                
                Log.d("StudentDetailViewModel", "Current parent ID: $parentId")

                // Get all linked students
                parentStudentUseCases.getLinkedStudents(parentId).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _state.value = _state.value.copy(isLoading = true)
                        }
                        is Resource.Success -> {
                            val studentInfo = resource.data?.find { it.student.id == studentId }
                            if (studentInfo != null) {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    studentInfo = studentInfo,
                                    error = null
                                )

                                // Sau khi load xong thông tin cơ bản, tải thêm tiến độ học tập
                                loadProgress(studentId)
                            } else {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = "Không tìm thấy thông tin học sinh"
                                )
                            }
                        }
                        is Resource.Error -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = resource.message ?: "Lỗi tải thông tin học sinh"
                            )
                        }
                    }
                }

                // Danh sách chi tiết từng bài học trên tất cả lớp của học sinh
                getStudentAllLessonProgress(studentId).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            // Không thay đổi trạng thái loading chung
                        }
                        is Resource.Success -> {
                            _state.value = _state.value.copy(
                                lessonProgressItems = result.data ?: emptyList()
                            )
                        }
                        is Resource.Error -> {
                            // Ghi log, không chặn màn hình nếu phần danh sách lỗi
                            Log.e("StudentDetailViewModel", "Lỗi tải danh sách tiến độ từng bài: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Đã xảy ra lỗi"
                )
            }
        }
    }

    private fun loadProgress(studentId: String) {
        viewModelScope.launch {
            try {
                // Dashboard tổng quan: bài học, kiểm tra, minigame...
                getStudentDashboard(studentId).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            _state.value = _state.value.copy(isLoading = true)
                        }
                        is Resource.Success -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                dashboard = result.data,
                                error = null
                            )
                        }
                        is Resource.Error -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = result.message ?: "Lỗi tải tiến độ học tập"
                            )
                        }
                    }
                }

                // Thống kê thời gian học
                getStudyTimeStatistics(studentId).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            // Giữ nguyên state loading hiện tại
                        }
                        is Resource.Success -> {
                            _state.value = _state.value.copy(
                                studyTime = result.data
                            )
                        }
                        is Resource.Error -> {
                            // Không chặn màn hình nếu thống kê thời gian học lỗi
                            Log.e("StudentDetailViewModel", "Lỗi tải thống kê thời gian học: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Lỗi tải tiến độ học tập"
                )
            }
        }
    }
}
