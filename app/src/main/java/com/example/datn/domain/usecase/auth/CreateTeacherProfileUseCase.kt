package com.example.datn.domain.usecase.auth

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.core.network.service.teacher.TeacherService
import com.example.datn.domain.models.Teacher
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class CreateTeacherProfileParams(
    val userId: String,
    val specialization: String = "",
    val level: String = "",
    val experienceYears: Int = 0
)

class CreateTeacherProfileUseCase @Inject constructor(
    private val teacherService: TeacherService
) : BaseUseCase<CreateTeacherProfileParams, Flow<Resource<Unit>>> {

    override fun invoke(params: CreateTeacherProfileParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val now = Instant.now()
            val teacherId = params.userId

            val teacher = Teacher(
                id = teacherId,
                userId = params.userId,
                specialization = params.specialization,
                level = params.level,
                experienceYears = params.experienceYears,
                createdAt = now,
                updatedAt = now
            )

            // Lưu Teacher vào collection "teachers" với id cố định = teacherId
            teacherService.add(teacherId, teacher)

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi tạo hồ sơ giáo viên"))
        }
    }
}
