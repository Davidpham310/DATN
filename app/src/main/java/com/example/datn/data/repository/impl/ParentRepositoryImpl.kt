package com.example.datn.data.repository.impl

import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.parent.ParentService
import com.example.datn.core.network.service.parent.ParentStudentService
import com.example.datn.core.network.service.student.StudentService
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.data.local.dao.ParentDao
import com.example.datn.data.local.dao.ParentStudentDao
import com.example.datn.data.local.dao.StudentDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.Parent
import com.example.datn.domain.models.Student
import com.example.datn.domain.repository.IParentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ParentRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val parentService: ParentService,
    private val parentStudentService: ParentStudentService,
    private val studentService: StudentService,
    private val parentDao: ParentDao,
    private val studentDao: StudentDao,
    private val parentStudentDao: ParentStudentDao
) : IParentRepository {

    override fun getParentProfile(parentId: String): Flow<Resource<Parent?>> = flow {
        emit(Resource.Loading())
        try {
            // Implementation: Get parent from Firestore or local
            emit(Resource.Success(null))
        } catch (e: Exception) {
            val local = parentDao.getParentById(parentId)?.toDomain()
            emit(Resource.Success(local))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun getLinkedStudents(parentId: String): Flow<Resource<List<Student>>> = flow {
        emit(Resource.Loading())
        try {
            val links = parentStudentService.getParentStudentLinks(parentId)
            val studentIds = links.map { it.studentId }
            val students = studentIds.mapNotNull { studentId ->
                studentService.getStudentById(studentId)
            }
            
            // Cache to local
            students.forEach { studentDao.insert(it.toEntity()) }
            
            emit(Resource.Success(students))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun updateParentProfile(parent: Parent): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            parentDao.update(parent.toEntity())
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun unlinkStudent(parentId: String, studentId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val success = parentStudentService.deleteParentStudentLink(parentId, studentId)
            if (success) {
                // Remove from local cache
                val link = parentStudentDao.getStudentsOfParent(parentId)
                    .find { it.studentId == studentId }
                link?.let { parentStudentDao.delete(it) }
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Không thể xóa liên kết"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }
}

