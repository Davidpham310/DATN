package com.example.datn.domain.usecase.teacher

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.core.network.service.teacher.TeacherService
import com.example.datn.domain.models.Teacher
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetTeacherProfileUseCase @Inject constructor(
    private val teacherService: TeacherService
) : BaseUseCase<String, Flow<Resource<Teacher>>> {

    override fun invoke(teacherId: String): Flow<Resource<Teacher>> = flow {
        emit(Resource.Loading())
        try {
            val teacher = teacherService.getById(teacherId)
            if (teacher == null) {
                emit(Resource.Error("Không tìm thấy hồ sơ giáo viên"))
            } else {
                emit(Resource.Success(teacher))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy hồ sơ giáo viên"))
        }
    }
}
