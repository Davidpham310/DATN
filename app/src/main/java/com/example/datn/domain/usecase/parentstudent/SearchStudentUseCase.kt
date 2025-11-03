package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.student.StudentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class StudentSearchResult(
    val student: Student,
    val user: User
)

class SearchStudentUseCase @Inject constructor(
    private val studentService: StudentService,
    private val firebaseDataSource: FirebaseDataSource
) : BaseUseCase<String, Flow<Resource<List<StudentSearchResult>>>> {
    
    override fun invoke(params: String): Flow<Resource<List<StudentSearchResult>>> = flow {
        emit(Resource.Loading())
        try {
            if (params.isBlank()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            // Lấy tất cả students
            val allStudents = studentService.getAll()
            
            // Lấy thông tin user cho mỗi student và filter theo tên
            val searchResults = allStudents.mapNotNull { student ->
                try {
                    val userResult = firebaseDataSource.getUserById(student.userId)
                    when (userResult) {
                        is Resource.Success -> {
                            val user = userResult.data
                            if (user != null && user.name.contains(params, ignoreCase = true)) {
                                StudentSearchResult(student, user)
                            } else null
                        }
                        else -> null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(Resource.Success(searchResults))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi tìm kiếm học sinh"))
        }
    }
}
