package com.example.datn.core.network.datasource

import com.example.datn.core.base.BaseDataSource
import com.example.datn.core.network.service.classroom.ClassService
import com.example.datn.core.network.service.lesson.LessonContentService
import com.example.datn.core.network.service.lesson.LessonService
import com.example.datn.core.network.service.user.UserService
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import javax.inject.Inject

class FirebaseDataSource @Inject constructor(
    private val userService: UserService,
    private val classService: ClassService,
    private val lessonService: LessonService,
    private val lessonContentService: LessonContentService
) : BaseDataSource() {

    // ==================== USER OPERATIONS ====================

    suspend fun getUserById(userId: String): Resource<User?> = safeCallWithResult {
        userService.getUserById(userId)
    }.toResource()

    suspend fun getUserByEmail(email: String): Resource<User?> = safeCallWithResult {
        userService.getUserByEmail(email)
    }.toResource()

    suspend fun getUsersByRole(role: String): Resource<List<User>> = safeCallWithResult {
        userService.getUsersByRole(role)
    }.toResource()

    suspend fun getAllUsers(): Resource<List<User>> = safeCallWithResult {
        userService.getAll()
    }.toResource()

    suspend fun addUser(user: User, id: String? = null): Resource<String> = safeCallWithResult {
        userService.add(id, user)
    }.toResource()

    suspend fun updateUser(id: String, user: User): Resource<Unit> = safeCallWithResult {
        userService.update(id, user)
    }.toResource()

    suspend fun deleteUser(userId: String): Resource<Unit> = safeCallWithResult {
        userService.delete(userId)
    }.toResource()

    suspend fun updateAvatar(userId: String, avatarUrl: String): Resource<Unit> = safeCallWithResult {
        userService.updateAvatar(userId, avatarUrl)
    }.toResource()

    // ==================== CLASS OPERATIONS ====================

    /**
     * Lấy tất cả lớp học
     */
    suspend fun getAllClasses(): Resource<List<Class>> = safeCallWithResult {
        classService.getAll()
    }.toResource()

    /**
     * Lấy lớp theo ID
     */
    suspend fun getClassById(classId: String): Resource<Class?> = safeCallWithResult {
        classService.getClassById(classId)
    }.toResource()

    /**
     * Lấy tất cả lớp của giáo viên
     */
    suspend fun getClassesByTeacher(teacherId: String): Resource<List<Class>> = safeCallWithResult {
        classService.getClassesByTeacher(teacherId)
    }.toResource()

    /**
     * Lấy tất cả lớp học sinh tham gia (status = APPROVED)
     */
    suspend fun getClassesByStudent(studentId: String): Resource<List<Class>> = safeCallWithResult {
        classService.getClassesByStudent(studentId)
    }.toResource()

    /**
     * Thêm lớp mới
     */
    suspend fun addClass(classObj: Class): Resource<Class?> = safeCallWithResult {
        classService.addClass(classObj)
    }.toResource()

    /**
     * Cập nhật lớp
     */
    suspend fun updateClass(classId: String, classObj: Class): Resource<Boolean> = safeCallWithResult {
        classService.updateClass(classId, classObj)
    }.toResource()

    /**
     * Xóa lớp (và tất cả enrollments liên quan)
     */
    suspend fun deleteClass(classId: String): Resource<Boolean> = safeCallWithResult {
        classService.deleteClass(classId)
    }.toResource()

    // ==================== ENROLLMENT OPERATIONS ====================

    /**
     * Thêm học sinh vào lớp (tạo enrollment request)
     * @param status Trạng thái ban đầu (PENDING hoặc APPROVED)
     */
    suspend fun addStudentToClass(
        classId: String,
        studentId: String,
        status: EnrollmentStatus = EnrollmentStatus.PENDING,
        approvedBy: String = ""
    ): Resource<Boolean> = safeCallWithResult {
        classService.addStudentToClass(
            classId = classId,
            studentId = studentId,
            status = status,
            approvedBy = approvedBy
        )
    }.toResource()

    /**
     * Xóa học sinh khỏi lớp
     */
    suspend fun removeStudentFromClass(
        classId: String,
        studentId: String
    ): Resource<Boolean> = safeCallWithResult {
        classService.removeStudentFromClass(classId, studentId)
    }.toResource()

    /**
     * Approve enrollment request
     */
    suspend fun approveEnrollment(
        classId: String,
        studentId: String,
        approvedBy: String
    ): Resource<Boolean> = safeCallWithResult {
        classService.updateEnrollmentStatus(
            classId = classId,
            studentId = studentId,
            status = EnrollmentStatus.APPROVED,
            approvedBy = approvedBy
        )
    }.toResource()

    /**
     * Reject enrollment request
     */
    suspend fun rejectEnrollment(
        classId: String,
        studentId: String,
        rejectionReason: String,
        rejectedBy: String
    ): Resource<Boolean> = safeCallWithResult {
        classService.updateEnrollmentStatus(
            classId = classId,
            studentId = studentId,
            status = EnrollmentStatus.REJECTED,
            approvedBy = rejectedBy,
            rejectionReason = rejectionReason
        )
    }.toResource()

    /**
     * Cập nhật trạng thái enrollment
     */
    suspend fun updateEnrollmentStatus(
        classId: String,
        studentId: String,
        status: EnrollmentStatus,
        approvedBy: String = "",
        rejectionReason: String = ""
    ): Resource<Boolean> = safeCallWithResult {
        classService.updateEnrollmentStatus(
            classId = classId,
            studentId = studentId,
            status = status,
            approvedBy = approvedBy,
            rejectionReason = rejectionReason
        )
    }.toResource()

    /**
     * Lấy danh sách học sinh trong lớp
     * @param status Filter theo trạng thái (null = tất cả)
     */
    suspend fun getStudentsInClass(
        classId: String,
        status: EnrollmentStatus? = null
    ): Resource<List<ClassStudent>> = safeCallWithResult {
        classService.getStudentsInClass(classId, status)
    }.toResource()

    /**
     * Lấy danh sách học sinh đã được approve trong lớp
     */
    suspend fun getApprovedStudentsInClass(classId: String): Resource<List<ClassStudent>> =
        getStudentsInClass(classId, EnrollmentStatus.APPROVED)

    /**
     * Lấy danh sách enrollment requests đang pending
     */
    suspend fun getPendingEnrollments(classId: String): Resource<List<ClassStudent>> =
        getStudentsInClass(classId, EnrollmentStatus.PENDING)

    /**
     * Lấy enrollment của một học sinh trong lớp cụ thể
     */
    suspend fun getEnrollment(
        classId: String,
        studentId: String
    ): Resource<ClassStudent?> = safeCallWithResult {
        classService.getEnrollment(classId, studentId)
    }.toResource()

    /**
     * Kiểm tra xem học sinh có trong lớp không (và đã được approve)
     */
    suspend fun isStudentInClass(
        classId: String,
        studentId: String
    ): Resource<Boolean> = safeCallWithResult {
        val enrollment = classService.getEnrollment(classId, studentId)
        enrollment != null && enrollment.enrollmentStatus == EnrollmentStatus.APPROVED
    }.toResource()

    /**
     * Kiểm tra xem học sinh có enrollment request pending không
     */
    suspend fun hasPendingEnrollment(
        classId: String,
        studentId: String
    ): Resource<Boolean> = safeCallWithResult {
        val enrollment = classService.getEnrollment(classId, studentId)
        enrollment != null && enrollment.enrollmentStatus == EnrollmentStatus.PENDING
    }.toResource()

    // ==================== BATCH OPERATIONS ====================

    /**
     * Approve nhiều enrollment requests cùng lúc
     */
    suspend fun batchApproveEnrollments(
        classId: String,
        studentIds: List<String>,
        approvedBy: String
    ): Resource<List<Boolean>> = safeCallWithResult {
        studentIds.map { studentId ->
            classService.updateEnrollmentStatus(
                classId = classId,
                studentId = studentId,
                status = EnrollmentStatus.APPROVED,
                approvedBy = approvedBy
            )
        }
    }.toResource()

    /**
     * Xóa nhiều học sinh khỏi lớp cùng lúc
     */
    suspend fun batchRemoveStudentsFromClass(
        classId: String,
        studentIds: List<String>
    ): Resource<List<Boolean>> = safeCallWithResult {
        studentIds.map { studentId ->
            classService.removeStudentFromClass(classId, studentId)
        }
    }.toResource()


    // ==================== LESSON OPERATIONS ====================

    suspend fun addLesson(lesson: Lesson): Resource<Lesson?> = safeCallWithResult {
        lessonService.addLesson(lesson)
    }.toResource()

    suspend fun getLessonsByClass(classId: String): Resource<List<Lesson>> = safeCallWithResult {
        lessonService.getLessonsByClass(classId)
    }.toResource()

    suspend fun getLessonById(lessonId: String): Resource<Lesson?> = safeCallWithResult {
        lessonService.getLessonById(lessonId)
    }.toResource()

    suspend fun updateLesson(lessonId: String, lesson: Lesson): Resource<Boolean> = safeCallWithResult {
        lessonService.updateLesson(lessonId, lesson)
    }.toResource()

    suspend fun deleteLesson(lessonId: String): Resource<Boolean> = safeCallWithResult {
        lessonService.deleteLesson(lessonId)
    }.toResource()

// ==================== LESSON CONTENT OPERATIONS ====================

    suspend fun getLessonContent(lessonId: String): Resource<List<LessonContent>> = safeCallWithResult {
        lessonContentService.getContentByLesson(lessonId)
    }.toResource()

    suspend fun addLessonContent(content: LessonContent): Resource<LessonContent?> = safeCallWithResult {
        lessonContentService.addContent(content)
    }.toResource()

    suspend fun updateLessonContent(contentId: String , content: LessonContent): Resource<Boolean> = safeCallWithResult {
        lessonContentService.updateContent(contentId, content)
    }.toResource()

    suspend fun deleteLessonContent(contentId: String): Resource<Boolean> = safeCallWithResult {
        lessonContentService.deleteContent(contentId)
    }.toResource()

    // ==================== HELPER ====================

    /**
     * Helper để chuyển Result<T> thành Resource<T>
     */
    private fun <T> Result<T>.toResource(): Resource<T> {
        return if (this.isSuccess) {
            Resource.Success(this.getOrThrow())
        } else {
            Resource.Error(this.exceptionOrNull()?.message ?: "Unknown Firebase Error")
        }
    }
}