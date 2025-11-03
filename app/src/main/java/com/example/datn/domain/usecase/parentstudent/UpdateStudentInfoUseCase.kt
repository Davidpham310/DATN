package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.student.StudentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject

data class UpdateStudentInfoParams(
    val studentId: String,
    val student: Student,
    val user: User
)

class UpdateStudentInfoUseCase @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val studentService: StudentService
) : BaseUseCase<UpdateStudentInfoParams, Flow<Resource<Unit>>> {
    override fun invoke(params: UpdateStudentInfoParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Update User
            val userResult = firebaseDataSource.updateUser(params.user.id, params.user)
            if (userResult !is com.example.datn.core.utils.Resource.Success) {
                emit(Resource.Error("Không thể cập nhật thông tin người dùng"))
                return@flow
            }
            
            // Update Student
            val success = studentService.updateStudent(params.studentId, params.student)
            if (success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Không thể cập nhật thông tin học sinh"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể cập nhật thông tin"))
        }
    }
}

