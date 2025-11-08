package com.example.datn.data.repository.impl

import android.util.Log
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.classroom.ClassService
import com.example.datn.core.network.service.parent.ParentService
import com.example.datn.core.network.service.parent.ParentStudentService
import com.example.datn.core.network.service.student.StudentService
import com.example.datn.core.network.service.user.UserService
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.data.local.dao.ClassDao
import com.example.datn.data.local.dao.ClassStudentDao
import com.example.datn.data.local.dao.ParentDao
import com.example.datn.data.local.dao.ParentStudentDao
import com.example.datn.data.local.dao.StudentDao
import com.example.datn.data.local.dao.TeacherDao
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.ClassEnrollmentInfo
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.models.Parent
import com.example.datn.domain.models.Student
import com.example.datn.domain.repository.IParentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val TAG = "ParentRepositoryImpl"

class ParentRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val parentService: ParentService,
    private val parentStudentService: ParentStudentService,
    private val studentService: StudentService,
    private val classService: ClassService,
    private val userService: UserService,
    private val parentDao: ParentDao,
    private val studentDao: StudentDao,
    private val parentStudentDao: ParentStudentDao,
    private val classDao: ClassDao,
    private val classStudentDao: ClassStudentDao,
    private val teacherDao: TeacherDao,
    private val userDao: UserDao
) : IParentRepository {

    override fun getParentProfile(parentId: String): Flow<Resource<Parent?>> = flow {
        emit(Resource.Loading())
        try {
            // Implementation: Get parent from Firestore or local
            emit(Resource.Success(null))
        } catch (e: Exception) {
            val local = parentDao.getParentById(parentId)?.toDomain()
            emit(Resource.Success(local))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun getLinkedStudents(parentId: String): Flow<Resource<List<Student>>> = flow {
        emit(Resource.Loading())
        try {
            val links = parentStudentService.getParentStudentLinks(parentId)
            val studentIds = links.map { it.studentId }
            val students = studentIds.mapNotNull { studentId ->
                studentService.getStudentById(studentId)
            }
            
            // Cache to local
            students.forEach { studentDao.insert(it.toEntity()) }
            
            emit(Resource.Success(students))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun updateParentProfile(parent: Parent): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            parentDao.update(parent.toEntity())
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun unlinkStudent(parentId: String, studentId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val success = parentStudentService.deleteParentStudentLink(parentId, studentId)
            if (success) {
                // Remove from local cache
                val link = parentStudentDao.getStudentsOfParent(parentId)
                    .find { it.studentId == studentId }
                link?.let { parentStudentDao.delete(it) }
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Không thể xóa liên kết"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun getStudentClassesForParent(
        parentId: String,
        studentId: String?,
        enrollmentStatus: EnrollmentStatus?
    ): Flow<Resource<List<ClassEnrollmentInfo>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting classes for parent: $parentId, studentId: $studentId, status: $enrollmentStatus")
            
            // 1. Validate input
            if (parentId.isBlank()) {
                emit(Resource.Error("Parent ID không được rỗng"))
                return@flow
            }
            
            // 2. Lấy danh sách con của phụ huynh
            val parentStudents = parentStudentDao.getStudentsOfParent(parentId)
            if (parentStudents.isEmpty()) {
                Log.i(TAG, "Parent has no linked students")
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            // Filter theo studentId nếu có
            val targetStudentIds = if (studentId != null) {
                if (parentStudents.any { it.studentId == studentId }) {
                    listOf(studentId)
                } else {
                    Log.w(TAG, "Student $studentId does not belong to parent $parentId")
                    emit(Resource.Error("Học sinh không thuộc về phụ huynh này"))
                    return@flow
                }
            } else {
                parentStudents.map { it.studentId }
            }
            
            Log.d(TAG, "Target student IDs: $targetStudentIds")
            
            // 3. Lấy danh sách lớp của các học sinh
            val classEnrollments = mutableListOf<ClassEnrollmentInfo>()
            
            for (stdId in targetStudentIds) {
                // Lấy enrollments của học sinh này
                var enrollments = classStudentDao.getClassesByStudentId(stdId)
                
                // Filter theo enrollment status nếu có
                if (enrollmentStatus != null) {
                    enrollments = enrollments.filter { it.enrollmentStatus == enrollmentStatus }
                }
                
                Log.d(TAG, "Found ${enrollments.size} enrollments for student $stdId")
                
                // 4. Lấy thông tin học sinh
                val studentEntity = studentDao.getStudentById(stdId)
                val studentUser = studentEntity?.let { userDao.getUserById(it.userId) }
                
                if (studentEntity == null || studentUser == null) {
                    Log.w(TAG, "Student or user info not found for $stdId, skipping")
                    continue
                }
                
                // 5. Xử lý từng enrollment
                for (enrollment in enrollments) {
                    // Lấy thông tin lớp
                    val classEntity = classDao.getClassById(enrollment.classId)
                    if (classEntity == null) {
                        Log.w(TAG, "Class ${enrollment.classId} not found, skipping")
                        continue
                    }
                    
                    // Lấy thông tin giáo viên
                    val teacherEntity = teacherDao.getTeacherById(classEntity.teacherId)
                    val teacherUser = teacherEntity?.let { userDao.getUserById(it.userId) }
                    
                    val teacherName = teacherUser?.name ?: "(Đã rời)"
                    val teacherAvatar = teacherUser?.avatarUrl
                    val teacherSpecialization = teacherEntity?.specialization ?: ""
                    
                    // Tạo ClassEnrollmentInfo
                    val enrollmentInfo = ClassEnrollmentInfo(
                        classId = classEntity.id,
                        className = classEntity.name,
                        classCode = classEntity.classCode,
                        subject = classEntity.subject,
                        gradeLevel = classEntity.gradeLevel,
                        teacherId = classEntity.teacherId,
                        teacherName = teacherName,
                        teacherAvatar = teacherAvatar,
                        teacherSpecialization = teacherSpecialization,
                        studentId = stdId,
                        studentName = studentUser.name,
                        studentAvatar = studentUser.avatarUrl,
                        enrollmentStatus = enrollment.enrollmentStatus,
                        enrolledDate = enrollment.enrolledDate,
                        approvedBy = enrollment.approvedBy.ifBlank { null },
                        rejectionReason = enrollment.rejectionReason.ifBlank { null },
                        classCreatedAt = classEntity.createdAt,
                        classUpdatedAt = classEntity.updatedAt
                    )
                    
                    classEnrollments.add(enrollmentInfo)
                }
            }
            
            // 6. Sắp xếp kết quả
            val sortedEnrollments = classEnrollments.sortedWith(
                compareBy<ClassEnrollmentInfo> {
                    // Sắp xếp theo status: APPROVED trước, PENDING sau, REJECTED/WITHDRAWN cuối
                    when (it.enrollmentStatus) {
                        EnrollmentStatus.APPROVED -> 1
                        EnrollmentStatus.PENDING -> 2
                        EnrollmentStatus.REJECTED -> 3
                        EnrollmentStatus.WITHDRAWN -> 4
                        EnrollmentStatus.NOT_ENROLLED -> 5
                    }
                }.thenByDescending {
                    // Trong cùng status, sắp xếp theo enrolledDate giảm dần
                    it.enrolledDate
                }
            )
            
            Log.i(TAG, "Successfully retrieved ${sortedEnrollments.size} class enrollments")
            emit(Resource.Success(sortedEnrollments))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student classes for parent", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }.catch { e ->
        Log.e(TAG, "Flow exception in getStudentClassesForParent", e)
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }
}

