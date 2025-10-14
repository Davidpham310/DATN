package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import kotlinx.coroutines.flow.Flow

interface IClassRepository {

    fun getClassById(classId: String): Flow<Resource<Class?>>

    fun getClassesByTeacher(teacherId: String): Flow<Resource<List<Class>>>

    fun getClassesByStudent(studentId: String): Flow<Resource<List<Class>>>

    fun addClass(classObj: Class): Flow<Resource<Class>>

    fun updateClass(classId: String, classObj: Class): Flow<Resource<Unit>>

    fun deleteClass(classId: String): Flow<Resource<Unit>>

    fun addStudentToClass(classId: String, studentId: String): Flow<Resource<Unit>>

    fun removeStudentFromClass(classId: String, studentId: String): Flow<Resource<Unit>>

    fun getAllClasses() : Flow<Resource<List<Class>>>
}
