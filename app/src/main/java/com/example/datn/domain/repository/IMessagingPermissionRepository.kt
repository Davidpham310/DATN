package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository xử lý logic phân quyền nhắn tin
 * Quyết định ai được phép nhắn tin với ai dựa trên mối quan hệ
 */
interface IMessagingPermissionRepository {
    
    // ==================== TEACHER PERMISSIONS ====================
    
    /**
     * Lấy danh sách học sinh trong các lớp giáo viên đang dạy
     * Logic: Class.teacherId = teacherId
     *        ClassStudent.classId = Class.id
     *        ClassStudent.enrollmentStatus = APPROVED
     * @return List<User> - Danh sách học sinh
     */
    fun getStudentsInMyClasses(teacherId: String): Flow<Resource<List<User>>>
    
    /**
     * Lấy danh sách phụ huynh của học sinh trong các lớp giáo viên đang dạy
     * Logic: Class.teacherId = teacherId
     *        ClassStudent.classId = Class.id (APPROVED)
     *        ParentStudent.studentId = ClassStudent.studentId
     * @return List<User> - Danh sách phụ huynh (loại bỏ trùng lặp)
     */
    fun getParentsOfMyStudents(teacherId: String): Flow<Resource<List<User>>>
    
    // ==================== STUDENT PERMISSIONS ====================
    
    /**
     * Lấy danh sách giáo viên đang dạy học sinh
     * Logic: ClassStudent.studentId = studentId (APPROVED)
     *        Class.id = ClassStudent.classId
     *        User.id = Class.teacherId
     * @return List<User> - Danh sách giáo viên (loại bỏ trùng lặp)
     */
    fun getMyTeachers(studentId: String): Flow<Resource<List<User>>>
    
    /**
     * Lấy danh sách phụ huynh của học sinh
     * Logic: ParentStudent.studentId = studentId
     * @return List<User> - Danh sách phụ huynh
     */
    fun getMyParents(studentId: String): Flow<Resource<List<User>>>
    
    /**
     * Lấy danh sách bạn cùng lớp (TÙY CHỌN - có thể bật/tắt)
     * Logic: ClassStudent.studentId = studentId
     *        Tìm tất cả học sinh khác trong cùng các lớp đó (APPROVED)
     * @return List<User> - Danh sách bạn cùng lớp (loại bỏ trùng lặp)
     */
    fun getMyClassmates(studentId: String): Flow<Resource<List<User>>>
    
    // ==================== PARENT PERMISSIONS ====================
    
    /**
     * Lấy danh sách con của phụ huynh
     * Logic: ParentStudent.parentId = parentId
     * @return List<User> - Danh sách con
     */
    fun getMyChildren(parentId: String): Flow<Resource<List<User>>>
    
    /**
     * Lấy danh sách giáo viên đang dạy các con của phụ huynh
     * Logic: ParentStudent.parentId = parentId
     *        ClassStudent.studentId = ParentStudent.studentId (APPROVED)
     *        Class.id = ClassStudent.classId
     *        User.id = Class.teacherId
     * @return List<User> - Danh sách giáo viên (loại bỏ trùng lặp)
     */
    fun getTeachersOfMyChildren(parentId: String): Flow<Resource<List<User>>>
    
    /**
     * Lấy danh sách phụ huynh khác có con cùng lớp (TÙY CHỌN - có thể bật/tắt)
     * Logic: ParentStudent.parentId = parentId → lấy các con
     *        ClassStudent.studentId = các con → lấy các lớp
     *        Tìm học sinh khác trong các lớp đó
     *        Tìm phụ huynh của các học sinh đó
     * @return List<User> - Danh sách phụ huynh (loại bỏ trùng lặp, trừ bản thân)
     */
    fun getParentsOfClassmates(parentId: String): Flow<Resource<List<User>>>
    
    // ==================== UTILITY FUNCTIONS ====================
    
    /**
     * Kiểm tra xem user1 có được phép nhắn tin với user2 không
     * @return Boolean - true nếu được phép
     */
    suspend fun canMessageUser(user1Id: String, user2Id: String): Resource<Boolean>
    
    /**
     * Lấy tất cả người được phép nhắn tin (dựa trên role của current user)
     * @param userId - ID người dùng hiện tại
     * @return List<User> - Danh sách tất cả người được phép nhắn
     */
    fun getAllAllowedRecipients(userId: String): Flow<Resource<List<User>>>
}
