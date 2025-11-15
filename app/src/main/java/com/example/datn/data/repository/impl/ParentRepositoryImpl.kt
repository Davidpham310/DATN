package com.example.datn.data.repository.impl

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ClassEnrollmentInfo
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.models.Parent
import com.example.datn.domain.models.Student
import com.example.datn.domain.repository.IParentRepository
import com.example.datn.core.network.datasource.FirebaseDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ParentRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource
) : IParentRepository {

    override fun getParentProfile(parentId: String): Flow<Resource<Parent?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getUserById(parentId)
            when (result) {
                is Resource.Success -> {
                    val user = result.data
                    if (user != null) {
                        val parent = Parent(
                            id = user.id,
                            userId = user.id,
                            createdAt = user.createdAt,
                            updatedAt = user.updatedAt
                        )
                        emit(Resource.Success(parent))
                    } else {
                        emit(Resource.Success(null))
                    }
                }
                is Resource.Error -> emit(Resource.Error(result.message ?: "Lỗi lấy hồ sơ phụ huynh"))
                else -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy hồ sơ phụ huynh"))
        }
    }

    override fun getLinkedStudents(parentId: String): Flow<Resource<List<Student>>> = flow {
        emit(Resource.Loading())
        try {
            // Lấy danh sách học sinh liên kết với phụ huynh (từ FirebaseDataSource helper)
            val result = firebaseDataSource.getStudentsByParentId(parentId)
            when (result) {
                is Resource.Success -> emit(Resource.Success(result.data ?: emptyList()))
                is Resource.Error -> emit(Resource.Error(result.message ?: "Lỗi lấy danh sách học sinh"))
                else -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy danh sách học sinh"))
        }
    }

    override fun updateParentProfile(parent: Parent): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Hiện tại Parent chỉ chứa id + userId + timestamps, không có thông tin profile riêng
            // nên không thể map trực tiếp sang User để update. Tạm thời trả về lỗi rõ ràng.
            emit(Resource.Error("Chức năng cập nhật hồ sơ phụ huynh chưa được hỗ trợ"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi cập nhật hồ sơ phụ huynh"))
        }
    }

    override fun unlinkStudent(parentId: String, studentId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Ngắt liên kết phụ huynh với học sinh qua FirebaseDataSource helper
            when (val result = firebaseDataSource.unlinkParentStudent(parentId, studentId)) {
                is Resource.Success -> emit(Resource.Success(Unit))
                is Resource.Error -> emit(Resource.Error(result.message ?: "Lỗi ngắt liên kết"))
                else -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi ngắt liên kết"))
        }
    }

    override fun getStudentClassesForParent(
        parentId: String,
        studentId: String?,
        enrollmentStatus: EnrollmentStatus?
    ): Flow<Resource<List<ClassEnrollmentInfo>>> = flow {
        emit(Resource.Loading())
        try {
            // Dùng helper mới trong FirebaseDataSource để build ClassEnrollmentInfo
            val result = firebaseDataSource.getStudentClassesForParent(
                parentId = parentId,
                studentId = studentId,
                enrollmentStatus = enrollmentStatus
            )
            when (result) {
                is Resource.Success -> emit(Resource.Success(result.data ?: emptyList()))
                is Resource.Error -> emit(Resource.Error(result.message ?: "Lỗi lấy danh sách lớp"))
                else -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy danh sách lớp"))
        }
    }
}
