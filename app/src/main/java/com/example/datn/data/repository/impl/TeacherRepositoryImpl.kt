package com.example.datn.data.repository.impl

import com.example.datn.core.utils.Resource
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.data.local.dao.TeacherDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.Teacher
import com.example.datn.domain.models.Class
import com.example.datn.domain.repository.ITeacherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TeacherRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val teacherDao: TeacherDao
) : ITeacherRepository {

    override fun getTeacherProfile(teacherId: String): Flow<Resource<Teacher?>> = flow {
        emit(Resource.Loading())
        try {
            val entity = teacherDao.getTeacherById(teacherId)
            val teacher = entity?.toDomain()
            emit(Resource.Success(teacher))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy hồ sơ giáo viên"))
        }
    }

    override fun updateTeacherProfile(teacher: Teacher): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Upsert hồ sơ giáo viên trong Room
            teacherDao.insert(teacher.toEntity())
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi cập nhật hồ sơ giáo viên"))
        }
    }

    override fun getClassesByTeacher(teacherId: String): Flow<Resource<List<Class>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getClassesByTeacher(teacherId)
            when (result) {
                is Resource.Success -> emit(Resource.Success(result.data ?: emptyList()))
                is Resource.Error -> emit(Resource.Error(result.message ?: "Lỗi lấy danh sách lớp"))
                else -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy danh sách lớp"))
        }
    }

    override fun deleteTeacherProfile(teacherId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val entity = teacherDao.getTeacherById(teacherId)
            if (entity != null) {
                teacherDao.delete(entity)
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi xóa hồ sơ giáo viên"))
        }
    }
}
