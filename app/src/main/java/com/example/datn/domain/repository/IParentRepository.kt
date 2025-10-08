package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
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
}