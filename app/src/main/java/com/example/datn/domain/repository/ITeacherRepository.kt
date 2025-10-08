package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Teacher
import com.example.datn.domain.models.Class
import kotlinx.coroutines.flow.Flow

interface ITeacherRepository {
    /**
     * Lấy hồ sơ chi tiết (đặc thù) của giáo viên.
     */
    fun getTeacherProfile(teacherId: String): Flow<Resource<Teacher?>>

    /**
     * Cập nhật thông tin chuyên môn, kinh nghiệm của giáo viên.
     */
    fun updateTeacherProfile(teacher: Teacher): Flow<Resource<Unit>>

    /**
     * Lấy tất cả các lớp học do giáo viên này phụ trách.
     */
    fun getClassesByTeacher(teacherId: String): Flow<Resource<List<Class>>>

    /**
     * Xóa hồ sơ giáo viên.
     */
    fun deleteTeacherProfile(teacherId: String): Flow<Resource<Unit>>
}