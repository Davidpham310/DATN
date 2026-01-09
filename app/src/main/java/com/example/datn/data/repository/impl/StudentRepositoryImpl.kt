package com.example.datn.data.repository.impl

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IStudentRepository
import com.example.datn.data.remote.datasource.FirebaseDataSource
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StudentRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource
) : IStudentRepository {

    override fun getStudentProfile(studentId: String): Flow<Resource<Student?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getStudentById(studentId)
            when (result) {
                is Resource.Success -> emit(Resource.Success(result.data))
                is Resource.Error -> emit(Resource.Error(result.message ?: "Lỗi lấy hồ sơ học sinh"))
                else -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Lỗi lấy hồ sơ học sinh"))
        }
    }

    override fun getStudentProfileByUserId(userId: String): Flow<Resource<Student?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getStudentByUserId(userId)
            when (result) {
                is Resource.Success -> emit(Resource.Success(result.data))
                is Resource.Error -> emit(Resource.Error(result.message ?: "Lỗi lấy hồ sơ học sinh"))
                else -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Lỗi lấy hồ sơ học sinh"))
        }
    }

    override fun getStudentUser(studentId: String): Flow<Resource<User?>> = flow {
        emit(Resource.Loading())
        try {
            val studentResult = firebaseDataSource.getStudentById(studentId)
            when (studentResult) {
                is Resource.Success -> {
                    val student = studentResult.data
                    if (student != null) {
                        val userResult = firebaseDataSource.getUserById(student.userId)
                        when (userResult) {
                            is Resource.Success -> emit(Resource.Success(userResult.data))
                            is Resource.Error -> emit(Resource.Error(userResult.message ?: "Lỗi lấy thông tin người dùng"))
                            else -> emit(Resource.Loading())
                        }
                    } else {
                        emit(Resource.Success(null))
                    }
                }
                is Resource.Error -> emit(Resource.Error(studentResult.message ?: "Lỗi lấy hồ sơ học sinh"))
                else -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Lỗi lấy thông tin người dùng"))
        }
    }

    override fun updateStudentProfile(student: Student): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firebaseDataSource.updateStudent(student.id, student)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Lỗi cập nhật hồ sơ học sinh"))
        }
    }

    override fun linkParentToStudent(
        studentId: String,
        parentId: String,
        relationship: String,
        isPrimaryGuardian: Boolean
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firebaseDataSource.linkParentToStudent(
                studentId = studentId,
                parentId = parentId,
                relationship = relationship,
                isPrimaryGuardian = isPrimaryGuardian
            )
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Lỗi liên kết phụ huynh"))
        }
    }
}
