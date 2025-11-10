package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import kotlinx.coroutines.flow.Flow

interface IStudentRepository {
    fun getStudentProfile(studentId: String): Flow<Resource<Student?>>
    fun getStudentProfileByUserId(userId: String): Flow<Resource<Student?>>
    fun getStudentUser(studentId: String): Flow<Resource<User?>>
    fun updateStudentProfile(student: Student): Flow<Resource<Unit>>
    fun linkParentToStudent(studentId: String, parentId: String, relationship: String): Flow<Resource<Unit>>
}