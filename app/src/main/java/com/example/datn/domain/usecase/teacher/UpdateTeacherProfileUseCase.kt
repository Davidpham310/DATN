package com.example.datn.domain.usecase.teacher

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.core.network.service.teacher.TeacherService
import com.example.datn.domain.models.Teacher
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class UpdateTeacherProfileParams(
    val teacherId: String,
    val specialization: String? = null,
    val level: String? = null,
    val experienceYears: Int? = null
)

class UpdateTeacherProfileUseCase @Inject constructor(
    private val teacherService: TeacherService
) : BaseUseCase<UpdateTeacherProfileParams, Flow<Resource<Unit>>> {

    override fun invoke(params: UpdateTeacherProfileParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val teacher = teacherService.getById(params.teacherId)
            if (teacher == null) {
                emit(Resource.Error("Không tìm thấy hồ sơ giáo viên"))
                return@flow
            }

            val updatedTeacher = teacher.copy(
                specialization = params.specialization ?: teacher.specialization,
                level = params.level ?: teacher.level,
                experienceYears = params.experienceYears ?: teacher.experienceYears,
                updatedAt = java.time.Instant.now()
            )

            teacherService.update(params.teacherId, updatedTeacher)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi cập nhật hồ sơ giáo viên"))
        }
    }
}
