package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.Student
import kotlinx.coroutines.flow.Flow

interface IClassRepository {
    fun createClass(classData: Class): Flow<Resource<Class>>
    fun getClassById(classId: String): Flow<Resource<Class?>>
    fun getClassesByTeacher(teacherId: String): Flow<Resource<List<Class>>>
    fun getClassesByStudent(studentId: String): Flow<Resource<List<Class>>>
    fun enrollStudentInClass(classId: String, studentId: String): Flow<Resource<Unit>>
    fun getStudentsInClass(classId: String): Flow<Resource<List<Student>>>
}
