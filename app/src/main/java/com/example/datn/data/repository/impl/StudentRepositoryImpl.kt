package com.example.datn.data.repository.impl

import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.student.StudentService
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.data.local.dao.StudentDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IStudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class StudentRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val studentService: StudentService,
    private val studentDao: StudentDao
) : IStudentRepository {

    override fun getStudentProfile(studentId: String): Flow<Resource<Student?>> = flow {
        emit(Resource.Loading())
        try {
            val student = studentService.getStudentById(studentId)
            if (student != null) {
                studentDao.insert(student.toEntity())
            }
            emit(Resource.Success(student))
        } catch (e: Exception) {
            // Fallback to local
            val local = studentDao.getStudentById(studentId)?.toDomain()
            emit(Resource.Success(local))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun getStudentUser(studentId: String): Flow<Resource<User?>> = flow {
        emit(Resource.Loading())
        try {
            val student = studentService.getStudentById(studentId)
            if (student != null) {
                val userResult = firebaseDataSource.getUserById(student.userId)
                emit(userResult)
            } else {
                emit(Resource.Success(null))
            }
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun updateStudentProfile(student: Student): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val success = studentService.updateStudent(student.id, student)
            if (success) {
                studentDao.update(student.toEntity())
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Không thể cập nhật thông tin học sinh"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun linkParentToStudent(
        studentId: String,
        parentId: String,
        relationship: String
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        emit(Resource.Success(Unit))
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }
}

