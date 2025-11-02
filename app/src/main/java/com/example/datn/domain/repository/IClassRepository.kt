package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.models.EnrollmentStatus
import kotlinx.coroutines.flow.Flow

interface IClassRepository {

    // ==================== CLASS OPERATIONS ====================

    /**
     * Lấy tất cả các lớp (Admin hoặc debug).
     */
    fun getAllClasses(): Flow<Resource<List<Class>>>

    /**
     * Lấy lớp theo ID.
     */
    fun getClassById(classId: String): Flow<Resource<Class?>>

    /**
     * Tìm kiếm lớp theo mã lớp (classCode).
     */
    fun getClassByCode(classCode: String): Flow<Resource<Class?>>

    /**
     * Lấy danh sách lớp theo giáo viên.
     */
    fun getClassesByTeacher(teacherId: String): Flow<Resource<List<Class>>>

    /**
     * Lấy danh sách lớp theo học sinh tham gia (chỉ approved).
     */
    fun getClassesByStudent(studentId: String): Flow<Resource<List<Class>>>

    /**
     * Thêm lớp mới.
     * @return Resource chứa Class với ID đã được tạo, hoặc null nếu thất bại
     */
    fun addClass(classObj: Class): Flow<Resource<Class?>>

    /**
     * Cập nhật lớp học.
     * @return Resource<Boolean> - true nếu thành công, false nếu thất bại
     */
    fun updateClass(classId: String, classObj: Class): Flow<Resource<Boolean>>

    /**
     * Xóa lớp học (và tất cả enrollments liên quan).
     * @return Resource<Boolean> - true nếu thành công, false nếu thất bại
     */
    fun deleteClass(classId: String): Flow<Resource<Boolean>>

    // ==================== ENROLLMENT OPERATIONS ====================

    /**
     * Thêm học sinh vào lớp (tạo enrollment request).
     * @param classId ID của lớp
     * @param studentId ID của học sinh
     * @param status Trạng thái enrollment (PENDING hoặc APPROVED)
     * @param approvedBy ID của người approve (nếu status = APPROVED)
     * @return Resource<Boolean> - true nếu thành công, false nếu thất bại
     */
    fun addStudentToClass(
        classId: String,
        studentId: String,
        status: EnrollmentStatus = EnrollmentStatus.PENDING,
        approvedBy: String = ""
    ): Flow<Resource<Boolean>>

    /**
     * Xóa học sinh khỏi lớp.
     * @return Resource<Boolean> - true nếu thành công, false nếu thất bại
     */
    fun removeStudentFromClass(
        classId: String,
        studentId: String
    ): Flow<Resource<Boolean>>

    /**
     * Approve enrollment request.
     * @param classId ID của lớp
     * @param studentId ID của học sinh
     * @param approvedBy ID của giáo viên/admin approve
     * @return Resource<Boolean> - true nếu thành công, false nếu thất bại
     */
    fun approveEnrollment(
        classId: String,
        studentId: String,
        approvedBy: String
    ): Flow<Resource<Boolean>>

    /**
     * Reject enrollment request.
     * @param classId ID của lớp
     * @param studentId ID của học sinh
     * @param rejectionReason Lý do từ chối
     * @param rejectedBy ID của người từ chối
     * @return Resource<Boolean> - true nếu thành công, false nếu thất bại
     */
    fun rejectEnrollment(
        classId: String,
        studentId: String,
        rejectionReason: String,
        rejectedBy: String
    ): Flow<Resource<Boolean>>

    /**
     * Cập nhật trạng thái enrollment.
     * @param status Trạng thái mới (PENDING, APPROVED, REJECTED, WITHDRAWN)
     * @return Resource<Boolean> - true nếu thành công, false nếu thất bại
     */
    fun updateEnrollmentStatus(
        classId: String,
        studentId: String,
        status: EnrollmentStatus,
        approvedBy: String = "",
        rejectionReason: String = ""
    ): Flow<Resource<Boolean>>

    /**
     * Lấy danh sách học sinh trong lớp.
     * @param classId ID của lớp
     * @param status Filter theo trạng thái (null = tất cả)
     * @return Resource chứa danh sách ClassStudent
     */
    fun getStudentsInClass(
        classId: String,
        status: EnrollmentStatus? = null
    ): Flow<Resource<List<ClassStudent>>>

    /**
     * Lấy danh sách học sinh đã được approve trong lớp.
     */
    fun getApprovedStudentsInClass(classId: String): Flow<Resource<List<ClassStudent>>>

    /**
     * Lấy danh sách enrollment requests đang pending.
     */
    fun getPendingEnrollments(classId: String): Flow<Resource<List<ClassStudent>>>

    /**
     * Lấy enrollment của một học sinh trong lớp cụ thể.
     * @return Resource chứa ClassStudent hoặc null nếu không tồn tại
     */
    fun getEnrollment(
        classId: String,
        studentId: String
    ): Flow<Resource<ClassStudent?>>

    /**
     * Kiểm tra xem học sinh có trong lớp không (approved).
     * @return Resource<Boolean> - true nếu học sinh đã trong lớp và approved
     */
    fun isStudentInClass(
        classId: String,
        studentId: String
    ): Flow<Resource<Boolean>>

    /**
     * Kiểm tra xem học sinh có enrollment request pending không.
     * @return Resource<Boolean> - true nếu có request pending
     */
    fun hasPendingEnrollment(
        classId: String,
        studentId: String
    ): Flow<Resource<Boolean>>

    // ==================== BATCH OPERATIONS ====================

    /**
     * Approve nhiều enrollment requests cùng lúc.
     * @param classId ID của lớp
     * @param studentIds Danh sách ID học sinh cần approve
     * @param approvedBy ID của người approve
     * @return Resource chứa danh sách Boolean (true = thành công cho từng student)
     */
    fun batchApproveEnrollments(
        classId: String,
        studentIds: List<String>,
        approvedBy: String
    ): Flow<Resource<List<Boolean>>>

    /**
     * Xóa nhiều học sinh khỏi lớp cùng lúc.
     * @param classId ID của lớp
     * @param studentIds Danh sách ID học sinh cần xóa
     * @return Resource chứa danh sách Boolean (true = thành công cho từng student)
     */
    fun batchRemoveStudentsFromClass(
        classId: String,
        studentIds: List<String>
    ): Flow<Resource<List<Boolean>>>
}