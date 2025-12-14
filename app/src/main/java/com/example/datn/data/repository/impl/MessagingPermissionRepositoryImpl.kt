package com.example.datn.data.repository.impl

import android.util.Log
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.ClassDao
import com.example.datn.data.local.dao.ClassStudentDao
import com.example.datn.data.local.dao.ParentStudentDao
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.repository.IMessagingPermissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingPermissionRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val classDao: ClassDao,
    private val classStudentDao: ClassStudentDao,
    private val parentStudentDao: ParentStudentDao,
    private val userDao: UserDao
) : IMessagingPermissionRepository {

    companion object {
        private const val TAG = "MessagingPermission"
    }

    // ==================== TEACHER PERMISSIONS ====================

    override fun getStudentsInMyClasses(teacherId: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting students for teacher: $teacherId")
            
            // Bước 1: Lấy tất cả lớp mà giáo viên đang dạy
            val classes = classDao.getClassesByTeacherId(teacherId)
            Log.d(TAG, "Teacher has ${classes.size} classes")
            
            if (classes.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            val classIds = classes.map { it.id }
            val students = mutableSetOf<User>() // Dùng Set để tránh trùng lặp
            
            // Bước 2: Với mỗi lớp, lấy học sinh đã APPROVED
            for (classId in classIds) {
                val classStudents = classStudentDao.getStudentsByClassId(classId)
                    .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                
                Log.d(TAG, "Class $classId has ${classStudents.size} approved students")
                
                // Bước 3: Lấy thông tin User từ studentId
                for (classStudent in classStudents) {
                    val userEntity = userDao.getUserById(classStudent.studentId)
                    if (userEntity != null) {
                        students.add(userEntity.toDomain())
                    }
                }
            }
            
            Log.d(TAG, "Total unique students: ${students.size}")
            emit(Resource.Success(students.toList()))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting students in teacher's classes", e)
            emit(Resource.Error("Không thể lấy danh sách học sinh: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getStudentsInMyClasses", e)
        emit(Resource.Error("Lỗi hệ thống: ${e.message}"))
    }

    override fun getParentsOfMyStudents(teacherId: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting parents for teacher's students: $teacherId")
            
            // Bước 1: Lấy tất cả lớp
            val classes = classDao.getClassesByTeacherId(teacherId)
            
            if (classes.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            val classIds = classes.map { it.id }
            val studentIds = mutableSetOf<String>()
            
            // Bước 2: Lấy tất cả studentId (APPROVED)
            for (classId in classIds) {
                val classStudents = classStudentDao.getStudentsByClassId(classId)
                    .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                    .map { it.studentId }
                studentIds.addAll(classStudents)
            }
            
            Log.d(TAG, "Found ${studentIds.size} unique students")
            
            if (studentIds.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            val parents = mutableSetOf<User>()
            
            // Bước 3: Với mỗi studentId, tìm parentId
            for (studentId in studentIds) {
                val parentStudentLinks = parentStudentDao.getParentsByStudentId(studentId)
                Log.d(TAG, "Student $studentId has ${parentStudentLinks.size} parents")
                
                // Bước 4: Lấy thông tin User của parent
                for (link in parentStudentLinks) {
                    val parentEntity = userDao.getUserById(link.parentId)
                    if (parentEntity != null) {
                        parents.add(parentEntity.toDomain())
                    }
                }
            }
            
            Log.d(TAG, "Total unique parents: ${parents.size}")
            emit(Resource.Success(parents.toList()))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting parents of teacher's students", e)
            emit(Resource.Error("Không thể lấy danh sách phụ huynh: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getParentsOfMyStudents", e)
        emit(Resource.Error("Lỗi hệ thống: ${e.message}"))
    }

    // ==================== STUDENT PERMISSIONS ====================

    override fun getMyTeachers(studentId: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting teachers for student: $studentId")
            
            // Bước 1: Lấy tất cả lớp mà học sinh đang học (APPROVED)
            val classStudents = classStudentDao.getClassesByStudentId(studentId)
                .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
            
            Log.d(TAG, "Student is in ${classStudents.size} approved classes")
            
            if (classStudents.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            val teachers = mutableSetOf<User>()
            
            // Bước 2: Với mỗi lớp, lấy teacherId
            for (classStudent in classStudents) {
                val classEntity = classDao.getClassById(classStudent.classId)
                
                if (classEntity != null) {
                    // Bước 3: Lấy thông tin User của teacher
                    val teacherEntity = userDao.getUserById(classEntity.teacherId)
                    if (teacherEntity != null) {
                        teachers.add(teacherEntity.toDomain())
                    }
                }
            }
            
            Log.d(TAG, "Total unique teachers: ${teachers.size}")
            emit(Resource.Success(teachers.toList()))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student's teachers", e)
            emit(Resource.Error("Không thể lấy danh sách giáo viên: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getMyTeachers", e)
        emit(Resource.Error("Lỗi hệ thống: ${e.message}"))
    }

    override fun getMyParents(studentId: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting parents for student: $studentId")
            
            // Lấy tất cả parent được liên kết
            val parentStudentLinks = parentStudentDao.getParentsByStudentId(studentId)
            Log.d(TAG, "Student has ${parentStudentLinks.size} parents")
            
            val parents = parentStudentLinks.mapNotNull { link ->
                userDao.getUserById(link.parentId)?.toDomain()
            }
            
            emit(Resource.Success(parents))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student's parents", e)
            emit(Resource.Error("Không thể lấy danh sách phụ huynh: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getMyParents", e)
        emit(Resource.Error("Lỗi hệ thống: ${e.message}"))
    }

    override fun getMyClassmates(studentId: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting classmates for student: $studentId")
            
            // Bước 1: Lấy tất cả lớp mà học sinh đang học (APPROVED)
            val myClasses = classStudentDao.getClassesByStudentId(studentId)
                .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                .map { it.classId }
            
            Log.d(TAG, "Student is in ${myClasses.size} classes")
            
            if (myClasses.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            val classmates = mutableSetOf<User>()
            
            // Bước 2: Với mỗi lớp, lấy tất cả học sinh khác (trừ bản thân)
            for (classId in myClasses) {
                val studentsInClass = classStudentDao.getStudentsByClassId(classId)
                    .filter { 
                        it.enrollmentStatus == EnrollmentStatus.APPROVED && 
                        it.studentId != studentId // Loại trừ bản thân
                    }
                
                // Lấy thông tin User
                for (classStudent in studentsInClass) {
                    val userEntity = userDao.getUserById(classStudent.studentId)
                    if (userEntity != null) {
                        classmates.add(userEntity.toDomain())
                    }
                }
            }
            
            Log.d(TAG, "Total unique classmates: ${classmates.size}")
            emit(Resource.Success(classmates.toList()))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student's classmates", e)
            emit(Resource.Error("Không thể lấy danh sách bạn cùng lớp: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getMyClassmates", e)
        emit(Resource.Error("Lỗi hệ thống: ${e.message}"))
    }

    // ==================== PARENT PERMISSIONS ====================

    override fun getMyChildren(parentId: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting children for parent: $parentId")
            
            // Lấy tất cả con được liên kết
            val parentStudentLinks = parentStudentDao.getStudentsByParentId(parentId)
            Log.d(TAG, "Parent has ${parentStudentLinks.size} children")
            
            val children = parentStudentLinks.mapNotNull { link ->
                userDao.getUserById(link.studentId)?.toDomain()
            }
            
            emit(Resource.Success(children))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting parent's children", e)
            emit(Resource.Error("Không thể lấy danh sách con: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getMyChildren", e)
        emit(Resource.Error("Lỗi hệ thống: ${e.message}"))
    }

    override fun getTeachersOfMyChildren(parentId: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting teachers for parent's children: $parentId")
            
            // Bước 1: Lấy tất cả con
            val parentStudentLinks = parentStudentDao.getStudentsByParentId(parentId)
            val childrenIds = parentStudentLinks.map { it.studentId }
            
            Log.d(TAG, "Parent has ${childrenIds.size} children")
            
            if (childrenIds.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            val teachers = mutableSetOf<User>()
            
            // Bước 2: Với mỗi con, lấy các lớp đang học (APPROVED)
            for (childId in childrenIds) {
                val childClasses = classStudentDao.getClassesByStudentId(childId)
                    .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                
                // Bước 3: Lấy teacherId từ mỗi lớp
                for (classStudent in childClasses) {
                    val classEntity = classDao.getClassById(classStudent.classId)
                    
                    if (classEntity != null) {
                        val teacherEntity = userDao.getUserById(classEntity.teacherId)
                        if (teacherEntity != null) {
                            teachers.add(teacherEntity.toDomain())
                        }
                    }
                }
            }
            
            Log.d(TAG, "Total unique teachers: ${teachers.size}")
            emit(Resource.Success(teachers.toList()))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting teachers of parent's children", e)
            emit(Resource.Error("Không thể lấy danh sách giáo viên: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getTeachersOfMyChildren", e)
        emit(Resource.Error("Lỗi hệ thống: ${e.message}"))
    }

    override fun getParentsOfClassmates(parentId: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting parents of classmates for parent: $parentId")
            
            // Bước 1: Lấy tất cả con
            val myChildren = parentStudentDao.getStudentsByParentId(parentId)
                .map { it.studentId }
            
            if (myChildren.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            val classmateIds = mutableSetOf<String>()
            
            // Bước 2: Với mỗi con, lấy các lớp đang học
            for (childId in myChildren) {
                val childClasses = classStudentDao.getClassesByStudentId(childId)
                    .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                    .map { it.classId }
                
                // Bước 3: Với mỗi lớp, lấy tất cả học sinh khác
                for (classId in childClasses) {
                    val studentsInClass = classStudentDao.getStudentsByClassId(classId)
                        .filter { 
                            it.enrollmentStatus == EnrollmentStatus.APPROVED &&
                            !myChildren.contains(it.studentId) // Loại trừ con của mình
                        }
                        .map { it.studentId }
                    
                    classmateIds.addAll(studentsInClass)
                }
            }
            
            Log.d(TAG, "Found ${classmateIds.size} classmates")
            
            if (classmateIds.isEmpty()) {
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            val parents = mutableSetOf<User>()
            
            // Bước 4: Với mỗi học sinh, lấy phụ huynh
            for (classmateId in classmateIds) {
                val parentLinks = parentStudentDao.getParentsByStudentId(classmateId)
                
                for (link in parentLinks) {
                    // Loại trừ bản thân
                    if (link.parentId != parentId) {
                        val parentEntity = userDao.getUserById(link.parentId)
                        if (parentEntity != null) {
                            parents.add(parentEntity.toDomain())
                        }
                    }
                }
            }
            
            Log.d(TAG, "Total unique parents of classmates: ${parents.size}")
            emit(Resource.Success(parents.toList()))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting parents of classmates", e)
            emit(Resource.Error("Không thể lấy danh sách phụ huynh: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getParentsOfClassmates", e)
        emit(Resource.Error("Lỗi hệ thống: ${e.message}"))
    }

    // ==================== UTILITY FUNCTIONS ====================

    override suspend fun canMessageUser(user1Id: String, user2Id: String): Resource<Boolean> {
        return try {
            // Lấy thông tin 2 users
            val user1 = userDao.getUserById(user1Id)?.toDomain()
            val user2 = userDao.getUserById(user2Id)?.toDomain()
            
            if (user1 == null || user2 == null) {
                return Resource.Error("Không tìm thấy người dùng")
            }
            
            // Kiểm tra quyền dựa trên role
            val canMessage = when (user1.role) {
                UserRole.TEACHER -> canTeacherMessageUser(user1Id, user2)
                UserRole.STUDENT -> canStudentMessageUser(user1Id, user2)
                UserRole.PARENT -> canParentMessageUser(user1Id, user2)
                else -> false
            }
            
            Resource.Success(canMessage)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking message permission", e)
            Resource.Error("Lỗi kiểm tra quyền: ${e.message}")
        }
    }

    private suspend fun canTeacherMessageUser(teacherId: String, targetUser: User): Boolean {
        return when (targetUser.role) {
            UserRole.STUDENT -> {
                // Check if student is in teacher's class
                val classes = classDao.getClassesByTeacherId(teacherId)
                val classIds = classes.map { it.id }
                
                classStudentDao.getClassesByStudentId(targetUser.id)
                    .any { 
                        it.enrollmentStatus == EnrollmentStatus.APPROVED && 
                        classIds.contains(it.classId) 
                    }
            }
            UserRole.PARENT -> {
                // Check if parent has child in teacher's class
                val classes = classDao.getClassesByTeacherId(teacherId)
                val classIds = classes.map { it.id }
                
                val children = parentStudentDao.getStudentsByParentId(targetUser.id)
                    .map { it.studentId }
                
                children.any { childId ->
                    classStudentDao.getClassesByStudentId(childId)
                        .any { 
                            it.enrollmentStatus == EnrollmentStatus.APPROVED && 
                            classIds.contains(it.classId) 
                        }
                }
            }
            else -> false
        }
    }

    private suspend fun canStudentMessageUser(studentId: String, targetUser: User): Boolean {
        return when (targetUser.role) {
            UserRole.TEACHER -> {
                // Check if teacher teaches student
                val studentClasses = classStudentDao.getClassesByStudentId(studentId)
                    .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                    .map { it.classId }
                
                studentClasses.any { classId ->
                    val classEntity = classDao.getClassById(classId)
                    classEntity?.teacherId == targetUser.id
                }
            }
            UserRole.PARENT -> {
                // Check if parent is linked to student
                parentStudentDao.getParentsByStudentId(studentId)
                    .any { it.parentId == targetUser.id }
            }
            UserRole.STUDENT -> {
                // Check if same class (optional feature)
                val myClasses = classStudentDao.getClassesByStudentId(studentId)
                    .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                    .map { it.classId }
                
                val targetClasses = classStudentDao.getClassesByStudentId(targetUser.id)
                    .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                    .map { it.classId }
                
                myClasses.intersect(targetClasses.toSet()).isNotEmpty()
            }
            else -> false
        }
    }

    private suspend fun canParentMessageUser(parentId: String, targetUser: User): Boolean {
        return when (targetUser.role) {
            UserRole.STUDENT -> {
                // Check if student is parent's child
                parentStudentDao.getStudentsByParentId(parentId)
                    .any { it.studentId == targetUser.id }
            }
            UserRole.TEACHER -> {
                // Check if teacher teaches parent's child
                val children = parentStudentDao.getStudentsByParentId(parentId)
                    .map { it.studentId }
                
                children.any { childId ->
                    val childClasses = classStudentDao.getClassesByStudentId(childId)
                        .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                    
                    childClasses.any { classStudent ->
                        val classEntity = classDao.getClassById(classStudent.classId)
                        classEntity?.teacherId == targetUser.id
                    }
                }
            }
            UserRole.PARENT -> {
                // Check if children are in same class (optional feature)
                val myChildren = parentStudentDao.getStudentsByParentId(parentId)
                    .map { it.studentId }
                
                val targetChildren = parentStudentDao.getStudentsByParentId(targetUser.id)
                    .map { it.studentId }
                
                val myChildClasses = myChildren.flatMap { childId ->
                    classStudentDao.getClassesByStudentId(childId)
                        .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                        .map { it.classId }
                }.toSet()
                
                val targetChildClasses = targetChildren.flatMap { childId ->
                    classStudentDao.getClassesByStudentId(childId)
                        .filter { it.enrollmentStatus == EnrollmentStatus.APPROVED }
                        .map { it.classId }
                }.toSet()
                
                myChildClasses.intersect(targetChildClasses).isNotEmpty()
            }
            else -> false
        }
    }

    override fun getAllAllowedRecipients(userId: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        
        try {
            val userEntity = userDao.getUserById(userId)
            
            if (userEntity == null) {
                emit(Resource.Error("Không tìm thấy người dùng"))
                return@flow
            }
            
            val user = userEntity.toDomain()
            val allRecipients = mutableSetOf<User>()
            
            // Lấy tất cả người được phép dựa trên role
            when (user.role) {
                UserRole.TEACHER -> {
                    // Students + Parents
                    getStudentsInMyClasses(userId).collect { resource ->
                        if (resource is Resource.Success) {
                            resource.data?.let { allRecipients.addAll(it) }
                        }
                    }
                    getParentsOfMyStudents(userId).collect { resource ->
                        if (resource is Resource.Success) {
                            resource.data?.let { allRecipients.addAll(it) }
                        }
                    }
                }
                UserRole.STUDENT -> {
                    // Teachers + Parents + Classmates
                    getMyTeachers(userId).collect { resource ->
                        if (resource is Resource.Success) {
                            resource.data?.let { allRecipients.addAll(it) }
                        }
                    }
                    getMyParents(userId).collect { resource ->
                        if (resource is Resource.Success) {
                            resource.data?.let { allRecipients.addAll(it) }
                        }
                    }
                    // Tùy chọn: bật/tắt classmates
                    getMyClassmates(userId).collect { resource ->
                        if (resource is Resource.Success) {
                            resource.data?.let { allRecipients.addAll(it) }
                        }
                    }
                }
                UserRole.PARENT -> {
                    // Children + Teachers + Other Parents
                    // NOTE: current implementation reads from Room cache. If cache is empty, fallback to Firebase.
                    val cachedLinks = parentStudentDao.getStudentsByParentId(userId)

                    if (cachedLinks.isEmpty()) {
                        Log.w(TAG, "Parent cache is empty (parent_student) for userId=$userId. Falling back to Firebase...")

                        // 1) Children
                        val remoteChildrenResult = firebaseDataSource.getStudentsByParentId(userId)
                        val remoteChildren = (remoteChildrenResult as? Resource.Success)?.data ?: emptyList()

                        remoteChildren.forEach { student ->
                            val childUserId = student.userId.ifBlank { student.id }
                            val childUserResult = firebaseDataSource.getUserById(childUserId)
                            val childUser = (childUserResult as? Resource.Success)?.data
                            if (childUser != null) {
                                allRecipients.add(childUser)
                                userDao.insert(childUser.toEntity())
                            }
                        }

                        // 2) Teachers of my children (APPROVED classes)
                        val remoteClassesResult = firebaseDataSource.getStudentClassesForParent(
                            parentId = userId,
                            studentId = null,
                            enrollmentStatus = EnrollmentStatus.APPROVED
                        )

                        val enrollments = (remoteClassesResult as? Resource.Success)?.data ?: emptyList()
                        val teacherIds = enrollments.mapNotNull { it.teacherId }.distinct()

                        teacherIds.forEach { teacherId ->
                            val teacherUserResult = firebaseDataSource.getUserById(teacherId)
                            val teacherUser = (teacherUserResult as? Resource.Success)?.data
                            if (teacherUser != null) {
                                allRecipients.add(teacherUser)
                                userDao.insert(teacherUser.toEntity())
                            }
                        }

                        // 3) Other parents: keep empty in fallback for now (requires additional remote graph traversal)
                    } else {
                        // Normal path: use Room cached graph
                        getMyChildren(userId).collect { resource ->
                            if (resource is Resource.Success) {
                                resource.data?.let { allRecipients.addAll(it) }
                            }
                        }
                        getTeachersOfMyChildren(userId).collect { resource ->
                            if (resource is Resource.Success) {
                                resource.data?.let { allRecipients.addAll(it) }
                            }
                        }
                        // Tùy chọn: bật/tắt other parents
                        getParentsOfClassmates(userId).collect { resource ->
                            if (resource is Resource.Success) {
                                resource.data?.let { allRecipients.addAll(it) }
                            }
                        }
                    }
                }
                else -> {
                    // Unknown role
                }
            }
            
            emit(Resource.Success(allRecipients.toList()))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all allowed recipients", e)
            emit(Resource.Error("Không thể lấy danh sách người nhắn tin: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getAllAllowedRecipients", e)
        emit(Resource.Error("Lỗi hệ thống: ${e.message}"))
    }
}
