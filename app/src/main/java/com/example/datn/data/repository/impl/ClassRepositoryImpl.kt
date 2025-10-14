package com.example.datn.data.repository.impl

import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.data.local.dao.ClassDao
import com.example.datn.data.local.entities.ClassStudentEntity
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.Class
import com.example.datn.domain.repository.IClassRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ClassRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val classDao: ClassDao
) : IClassRepository {

    // ----------------- Class -----------------
    override fun getClassById(classId: String): Flow<Resource<Class?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getClassById(classId)
            if (result is Resource.Success) {
                result.data?.let { classEntity ->
                    classDao.insertClass(classEntity.toEntity())
                }
            }
            emit(result)
        } catch (e: Exception) {
            // fallback: lấy từ local nếu Firebase lỗi
            val local = classDao.getClassById(classId)?.toDomain()
            if (local != null) emit(Resource.Success(local))
            else emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun getClassesByTeacher(teacherId: String): Flow<Resource<List<Class>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getClassesByTeacher(teacherId)
            if (result is Resource.Success) {
                // lưu tất cả vào Room
                result.data.forEach { classDao.insertClass(it.toEntity()) }
            }
            emit(result)
        } catch (e: Exception) {
            // fallback: lấy từ local
            val local = classDao.getClassesByTeacher(teacherId).map { it.toDomain() }
            emit(Resource.Success(local))
        }
    }

    override fun getClassesByStudent(studentId: String): Flow<Resource<List<Class>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getClassesByStudent(studentId)
            if (result is Resource.Success) {
                result.data.forEach { classDao.insertClass(it.toEntity()) }
            }
            emit(result)
        } catch (e: Exception) {
            val local = classDao.getClassesByStudent(studentId).map { it.toDomain() }
            emit(Resource.Success(local))
        }
    }

    override fun addClass(classObj: Class): Flow<Resource<Class>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.addClass(classObj)
            if (result is Resource.Success) {
                classDao.insertClass(result.data.toEntity())
            }
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun updateClass(classId: String, classObj: Class): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.updateClass(classId, classObj)
            if (result is Resource.Success) {
                classDao.updateClass(classObj.toEntity())
            }
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun deleteClass(classId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.deleteClass(classId)
            if (result is Resource.Success) {
                classDao.getClassById(classId)?.let { classDao.deleteClass(it) }
            }
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    // ----------------- ClassStudent -----------------
    override fun addStudentToClass(classId: String, studentId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.addStudentToClass(classId, studentId)
            if (result is Resource.Success) {
                val classStudent = ClassStudentEntity(classId, studentId)
                classDao.addStudentToClass(classStudent)
            }
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun removeStudentFromClass(classId: String, studentId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.removeStudentFromClass(classId, studentId)
            if (result is Resource.Success) {
                val classStudent = ClassStudentEntity(classId, studentId)
                classDao.removeStudentFromClass(classStudent)
            }
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun getAllClasses(): Flow<Resource<List<Class>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getAllClasses()
            if (result is Resource.Success) {
                result.data.forEach { classDao.insertClass(it.toEntity()) }
            }
            emit(result)
        } catch (e: Exception) {
            val local = classDao.getClassesByTeacher("").map { it.toDomain() }
            emit(Resource.Success(local))
        }
    }
}
