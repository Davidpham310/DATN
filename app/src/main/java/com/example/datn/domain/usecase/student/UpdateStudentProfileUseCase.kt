package com.example.datn.domain.usecase.student

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.data.remote.service.student.StudentService
import com.example.datn.domain.models.Student
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class UpdateStudentProfileParams(
    val studentId: String,
    val dateOfBirth: java.time.LocalDate? = null,
    val gradeLevel: String? = null
)

class UpdateStudentProfileUseCase @Inject constructor(
    private val studentService: StudentService
) : BaseUseCase<UpdateStudentProfileParams, Flow<Resource<Unit>>> {

    override fun invoke(params: UpdateStudentProfileParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val student = studentService.getById(params.studentId)
            if (student == null) {
                emit(Resource.Error("Không tìm thấy hồ sơ học sinh"))
                return@flow
            }

            val updatedStudent = student.copy(
                dateOfBirth = params.dateOfBirth ?: student.dateOfBirth,
                gradeLevel = params.gradeLevel ?: student.gradeLevel,
                updatedAt = java.time.Instant.now()
            )

            studentService.update(params.studentId, updatedStudent)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi cập nhật hồ sơ học sinh"))
        }
    }
}
