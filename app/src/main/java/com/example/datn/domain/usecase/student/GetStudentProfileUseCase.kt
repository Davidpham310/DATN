package com.example.datn.domain.usecase.student

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.data.remote.service.student.StudentService
import com.example.datn.domain.models.Student
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetStudentProfileUseCase @Inject constructor(
    private val studentService: StudentService
) : BaseUseCase<String, Flow<Resource<Student>>> {

    override fun invoke(studentId: String): Flow<Resource<Student>> = flow {
        emit(Resource.Loading())
        try {
            val student = studentService.getById(studentId)
            if (student == null) {
                emit(Resource.Error("Không tìm thấy hồ sơ học sinh"))
            } else {
                emit(Resource.Success(student))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy hồ sơ học sinh"))
        }
    }
}
