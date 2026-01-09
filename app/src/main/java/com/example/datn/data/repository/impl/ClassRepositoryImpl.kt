package com.example.datn.data.repository.impl

import com.example.datn.data.remote.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.data.local.dao.ClassDao
import com.example.datn.data.local.entities.ClassStudentEntity
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.repository.IClassRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ClassRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val classDao: ClassDao
) : IClassRepository {

    // ==================== CLASS OPERATIONS ====================

    /**
     * Lấy tất cả các lớp (Admin hoặc debug).
     */
    override fun getAllClasses(): Flow<Resource<List<Class>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getAllClasses()
            if (result is Resource.Success) {
                // Cache vào Room
                result.data.forEach { classDao.insertClass(it.toEntity()) }
            }
            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Fallback: lấy từ local cache
            val local = classDao.getAllClasses().map { it.toDomain() }
            emit(Resource.Success(local))
        }
    }

    /**
     * Lấy lớp theo ID.
     */
    override fun getClassById(classId: String): Flow<Resource<Class?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getClassById(classId)

            if (result is Resource.Success && result.data != null) {
                // Cache vào Room
                classDao.insertClass(result.data.toEntity())
            }

            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Fallback: lấy từ local cache
            val local = classDao.getClassById(classId)?.toDomain()
            if (local != null) {
                emit(Resource.Success(local))
            } else {
                emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
            }
        }
    }

    /**
     * Tìm kiếm lớp theo mã lớp (classCode).
     */
    override fun getClassByCode(classCode: String): Flow<Resource<Class?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getClassByCode(classCode)

            if (result is Resource.Success && result.data != null) {
                // Cache vào Room
                classDao.insertClass(result.data.toEntity())
            }

            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Lấy danh sách lớp theo giáo viên.
     */
    override fun getClassesByTeacher(teacherId: String): Flow<Resource<List<Class>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getClassesByTeacher(teacherId)
            if (result is Resource.Success) {
                // Cache vào Room
                result.data.forEach { classDao.insertClass(it.toEntity()) }
            }
            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Fallback: lấy từ local cache
            val local = classDao.getClassesByTeacher(teacherId).map { it.toDomain() }
            emit(Resource.Success(local))
        }
    }

    /**
     * Lấy danh sách lớp theo học sinh tham gia (chỉ approved).
     */
    override fun getClassesByStudent(studentId: String): Flow<Resource<List<Class>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getClassesByStudent(studentId)
            if (result is Resource.Success) {
                // Cache vào Room
                result.data.forEach { classDao.insertClass(it.toEntity()) }
            }
            emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Fallback: lấy từ local cache
            val local = classDao.getClassesByStudent(studentId).map { it.toDomain() }
            emit(Resource.Success(local))
        }
    }

    /**
     * Thêm lớp mới.
     */
    override fun addClass(classObj: Class): Flow<Resource<Class?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.addClass(classObj)

            when (result) {
                is Resource.Success -> {
                    if (result.data != null) {
                        // Cache vào Room
                        classDao.insertClass(result.data.toEntity())
                        emit(Resource.Success(result.data))
                    } else {
                        emit(Resource.Error("Failed to create class"))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(result.message))
                }
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Cập nhật lớp học.
     */
    override fun updateClass(classId: String, classObj: Class): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.updateClass(classId, classObj)

            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        // Cập nhật Room
                        classDao.updateClass(classObj.toEntity())
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to update class"))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(result.message))
                }
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Xóa lớp học (và tất cả enrollments).
     */
    override fun deleteClass(classId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.deleteClass(classId)

            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        // Xóa khỏi Room
                        classDao.getClassById(classId)?.let { classDao.deleteClass(it) }
                        // Xóa tất cả class_students liên quan
                        classDao.deleteAllStudentsFromClass(classId)
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to delete class"))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(result.message))
                }
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    fun addStudentToClass(
        classId: String,
        studentId: String
    ): Flow<Resource<Unit>> {
        TODO("Not yet implemented")
    }

    // ==================== ENROLLMENT OPERATIONS ====================

    /**
     * Thêm học sinh vào lớp (tạo enrollment request).
     */
    override fun addStudentToClass(
        classId: String,
        studentId: String,
        status: EnrollmentStatus,
        approvedBy: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.addStudentToClass(
                classId = classId,
                studentId = studentId,
                status = status,
                approvedBy = approvedBy
            )

            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        // Cache vào Room (chỉ cache nếu APPROVED)
                        if (status == EnrollmentStatus.APPROVED) {
                            val classStudent = ClassStudentEntity(classId, studentId)
                            classDao.addStudentToClass(classStudent)
                        }
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to add student to class"))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(result.message))
                }
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Xóa học sinh khỏi lớp.
     */
    override fun removeStudentFromClass(
        classId: String,
        studentId: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.removeStudentFromClass(classId, studentId)

            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        // Xóa khỏi Room
                        val classStudent = ClassStudentEntity(classId, studentId)
                        classDao.removeStudentFromClass(classStudent)
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to remove student from class"))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(result.message))
                }
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Approve enrollment request.
     */
    override fun approveEnrollment(
        classId: String,
        studentId: String,
        approvedBy: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.approveEnrollment(classId, studentId, approvedBy)

            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        // Thêm vào Room khi approved
                        val classStudent = ClassStudentEntity(classId, studentId)
                        classDao.addStudentToClass(classStudent)
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to approve enrollment"))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(result.message))
                }
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Reject enrollment request.
     */
    override fun rejectEnrollment(
        classId: String,
        studentId: String,
        rejectionReason: String,
        rejectedBy: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.rejectEnrollment(
                classId,
                studentId,
                rejectionReason,
                rejectedBy
            )
            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Cập nhật trạng thái enrollment.
     */
    override fun updateEnrollmentStatus(
        classId: String,
        studentId: String,
        status: EnrollmentStatus,
        approvedBy: String,
        rejectionReason: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.updateEnrollmentStatus(
                classId = classId,
                studentId = studentId,
                status = status,
                approvedBy = approvedBy,
                rejectionReason = rejectionReason
            )

            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        // ✅ KHÔNG cache vào Room vì parent sẽ query trực tiếp từ Firebase
                        // Room cache chỉ local trên một thiết bị, không giúp gì cho devices khác
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to update enrollment status"))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(result.message))
                }
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Lấy danh sách học sinh trong lớp.
     */
    override fun getStudentsInClass(
        classId: String,
        status: EnrollmentStatus?
    ): Flow<Resource<List<ClassStudent>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getStudentsInClass(classId, status)
            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Lấy danh sách học sinh đã được approve.
     */
    override fun getApprovedStudentsInClass(classId: String): Flow<Resource<List<ClassStudent>>> =
        getStudentsInClass(classId, EnrollmentStatus.APPROVED)

    /**
     * Lấy danh sách enrollment requests đang pending.
     */
    override fun getPendingEnrollments(classId: String): Flow<Resource<List<ClassStudent>>> =
        getStudentsInClass(classId, EnrollmentStatus.PENDING)

    /**
     * Lấy enrollment của một học sinh trong lớp cụ thể.
     */
    override fun getEnrollment(
        classId: String,
        studentId: String
    ): Flow<Resource<ClassStudent?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getEnrollment(classId, studentId)
            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Kiểm tra xem học sinh có trong lớp không (approved).
     */
    override fun isStudentInClass(
        classId: String,
        studentId: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.isStudentInClass(classId, studentId)
            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Fallback: kiểm tra từ local cache
            val localExists = classDao.isStudentInClass(classId, studentId)
            if (localExists) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
            }
        }
    }

    /**
     * Kiểm tra xem học sinh có enrollment request pending không.
     */
    override fun hasPendingEnrollment(
        classId: String,
        studentId: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.hasPendingEnrollment(classId, studentId)
            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    // ==================== BATCH OPERATIONS ====================

    /**
     * Approve nhiều enrollment requests cùng lúc.
     */
    override fun batchApproveEnrollments(
        classId: String,
        studentIds: List<String>,
        approvedBy: String
    ): Flow<Resource<List<Boolean>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.batchApproveEnrollments(classId, studentIds, approvedBy)

            if (result is Resource.Success) {
                // Cache tất cả approved students vào Room
                studentIds.forEach { studentId ->
                    val classStudent = ClassStudentEntity(classId, studentId)
                    classDao.addStudentToClass(classStudent)
                }
            }

            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    /**
     * Xóa nhiều học sinh khỏi lớp cùng lúc.
     */
    override fun batchRemoveStudentsFromClass(
        classId: String,
        studentIds: List<String>
    ): Flow<Resource<List<Boolean>>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.batchRemoveStudentsFromClass(classId, studentIds)

            if (result is Resource.Success) {
                // Xóa tất cả khỏi Room
                studentIds.forEach { studentId ->
                    val classStudent = ClassStudentEntity(classId, studentId)
                    classDao.removeStudentFromClass(classStudent)
                }
            }

            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }
}