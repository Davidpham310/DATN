package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ClassEnrollmentInfo
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.models.Parent
import com.example.datn.domain.models.Student
import kotlinx.coroutines.flow.Flow

interface IParentRepository {
    /**
     * Lấy hồ sơ chi tiết của phụ huynh.
     */
    fun getParentProfile(parentId: String): Flow<Resource<Parent?>>

    /**
     * Lấy danh sách tất cả học sinh được liên kết với phụ huynh này.
     */
    fun getLinkedStudents(parentId: String): Flow<Resource<List<Student>>>

    /**
     * Cập nhật thông tin hồ sơ phụ huynh.
     */
    fun updateParentProfile(parent: Parent): Flow<Resource<Unit>>

    /**
     * Ngắt liên kết với một học sinh.
     */
    fun unlinkStudent(parentId: String, studentId: String): Flow<Resource<Unit>>
    
    /**
     * Lấy danh sách lớp học mà con của phụ huynh đang tham gia.
     * @param parentId ID phụ huynh
     * @param studentId ID học sinh (optional - lọc theo con cụ thể)
     * @param enrollmentStatus Trạng thái enrollment (optional - lọc theo trạng thái)
     * @return Flow<Resource<List<ClassEnrollmentInfo>>> Danh sách thông tin lớp học
     */
    fun getStudentClassesForParent(
        parentId: String,
        studentId: String? = null,
        enrollmentStatus: EnrollmentStatus? = null
    ): Flow<Resource<List<ClassEnrollmentInfo>>>
}