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
        Log.d(TAG, "🔍 getStudentClassesForParent CALLED: parentId=$parentId, studentId=$studentId, status=$enrollmentStatus")
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting classes for parent: $parentId, studentId: $studentId, status: $enrollmentStatus")
            
            // 1. Validate input
            if (parentId.isBlank()) {
                Log.e(TAG, "❌ Parent ID is blank!")
                emit(Resource.Error("Parent ID không được rỗng"))
                return@flow
            }
            
            // 2. ✅ Lấy danh sách con của phụ huynh từ FIREBASE
            Log.d(TAG, "📚 Loading students from FIREBASE for parent: $parentId")
            val parentStudents = parentStudentService.getParentStudentLinks(parentId)
            Log.i(TAG, "✅ Found ${parentStudents.size} students from FIREBASE")
            
            if (parentStudents.isEmpty()) {
                Log.w(TAG, "⚠️ Parent has no linked students in FIREBASE - returning empty list")
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
                // ✅ Lấy enrollments từ FIREBASE thay vì Room
                Log.d(TAG, "📋 Loading enrollments from Firebase for student: $stdId, filter status: $enrollmentStatus")
                val enrollments = classService.getEnrollmentsByStudent(
                    studentId = stdId,
                    enrollmentStatus = enrollmentStatus
                )
                
                Log.i(TAG, "✅ Found ${enrollments.size} enrollments from Firebase for student $stdId")
                enrollments.forEachIndexed { index, enrollment ->
                    Log.d(TAG, "  [$index] ClassID: ${enrollment.classId}, Status: ${enrollment.enrollmentStatus}, Date: ${enrollment.enrolledDate}")
                }
                
                // 4. ✅ Lấy thông tin học sinh từ FIREBASE
                Log.d(TAG, "👤 Loading student info from FIREBASE for: $stdId")
                val studentUser = studentService.getStudentById(stdId)?.let { student ->
                    userService.getUserById(student.userId)
                }
                
                if (studentUser == null) {
                    Log.w(TAG, "  ⚠️ Student or user info not found from FIREBASE for $stdId, skipping")
                    continue
                }
                Log.d(TAG, "  ✓ Student loaded: ${studentUser.name}")
                
                // 5. Xử lý từng enrollment
                for (enrollment in enrollments) {
                    Log.d(TAG, "  📚 Processing enrollment: ClassID=${enrollment.classId}, Status=${enrollment.enrollmentStatus}")
                    
                    // ✅ Lấy thông tin lớp từ FIREBASE
                    val classObj = classService.getClassById(enrollment.classId)
                    if (classObj == null) {
                        Log.w(TAG, "  ⚠️ Class ${enrollment.classId} not found from Firebase, skipping")
                        continue
                    }
                    Log.d(TAG, "  ✓ Class loaded: ${classObj.name} (${classObj.classCode})")
                    
                    // ✅ Lấy thông tin giáo viên từ FIREBASE
                    val teacherUser = userService.getUserById(classObj.teacherId)
                    
                    val teacherName = teacherUser?.name ?: "(Đã rời)"
                    val teacherAvatar = teacherUser?.avatarUrl
                    val teacherSpecialization = "" // Teacher specialization không cần thiết
                    
                    Log.d(TAG, "  ✓ Teacher: $teacherName")
                    
                    // Tạo ClassEnrollmentInfo
                    val enrollmentInfo = ClassEnrollmentInfo(
                        classId = classObj.id,
                        className = classObj.name,
                        classCode = classObj.classCode,
                        subject = classObj.subject,
                        gradeLevel = classObj.gradeLevel,
                        teacherId = classObj.teacherId,
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
                        classCreatedAt = classObj.createdAt,
                        classUpdatedAt = classObj.updatedAt
                    )
                    
                    classEnrollments.add(enrollmentInfo)
                }
            }
            
            // 6. Sắp xếp kết quả
            Log.d(TAG, "📊 Sorting ${classEnrollments.size} class enrollments...")
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
            
            Log.i(TAG, "🎉 Successfully retrieved ${sortedEnrollments.size} class enrollments for parent $parentId")
            Log.d(TAG, "📋 Final class list:")
            sortedEnrollments.forEachIndexed { index, info ->
                Log.d(TAG, "  [$index] ${info.className} (${info.classCode}) - ${info.enrollmentStatus} - Student: ${info.studentName}")
            }
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

