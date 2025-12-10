package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.usecase.auth.RegisterParams
import com.example.datn.domain.usecase.auth.RegisterUseCase
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.student.StudentService
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

data class CreateStudentAccountParams(
    val parentId: String,  // Parent's id from parents collection (not userId)
    val email: String,
    val password: String,
    val name: String,
    val dateOfBirth: LocalDate,
    val gradeLevel: String,
    val relationship: String,
    val isPrimaryGuardian: Boolean = true
)

class CreateStudentAccountForParentUseCase @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val studentService: StudentService,
    private val firebaseDataSource: FirebaseDataSource
) : BaseUseCase<CreateStudentAccountParams, Flow<Resource<Unit>>> {

    override fun invoke(params: CreateStudentAccountParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            var createdUser: User? = null

            // 1. Tạo tài khoản User với role STUDENT
            registerUseCase(
                RegisterParams(
                    email = params.email,
                    password = params.password,
                    name = params.name,
                    role = UserRole.STUDENT
                )
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        emit(Resource.Loading())
                    }
                    is Resource.Success -> {
                        createdUser = result.data
                    }
                    is Resource.Error -> {
                        emit(Resource.Error(result.message ?: "Không thể tạo tài khoản học sinh"))
                        return@collect
                    }
                }
            }

            val user = createdUser
            if (user == null) {
                emit(Resource.Error("Không thể tạo tài khoản học sinh"))
                return@flow
            }

            // 2. Tạo hồ sơ Student tương ứng
            val now = Instant.now()
            val studentId = user.id.ifBlank { user.id }

            val student = Student(
                id = studentId,
                userId = user.id,
                dateOfBirth = params.dateOfBirth,
                gradeLevel = params.gradeLevel,
                createdAt = now,
                updatedAt = now
            )

            // Lưu Student vào collection "students" với id cố định = studentId
            studentService.add(studentId, student)

            // 3. Liên kết phụ huynh với học sinh
            // parentId là ID từ collection parents (không phải userId)
            firebaseDataSource.linkParentToStudent(
                studentId = studentId,
                parentId = params.parentId,
                relationship = params.relationship
            )

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi tạo tài khoản học sinh"))
        }
    }
}
