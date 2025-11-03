package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ParentStudent
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.parent.ParentStudentService
import com.example.datn.core.network.service.student.StudentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class LinkedStudentInfo(
    val student: Student,
    val user: User,
    val parentStudent: ParentStudent
)

class GetLinkedStudentsUseCase @Inject constructor(
    private val parentStudentService: ParentStudentService,
    private val studentService: StudentService,
    private val firebaseDataSource: FirebaseDataSource
) : BaseUseCase<String, Flow<Resource<List<LinkedStudentInfo>>>> {
    override fun invoke(params: String): Flow<Resource<List<LinkedStudentInfo>>> = flow {
        emit(Resource.Loading())
        try {
            // Get all parent-student links
            val links = parentStudentService.getParentStudentLinks(params)
            
            // Get student and user info for each link
            val result = links.mapNotNull { link ->
                try {
                    // Validate studentId before querying
                    if (link.studentId.isBlank() || link.studentId == "students") {
                        return@mapNotNull null
                    }
                    
                    val student = studentService.getStudentById(link.studentId)
                    if (student == null) {
                        return@mapNotNull null
                    }
                    
                    // Get user info
                    val userResult = firebaseDataSource.getUserById(student.userId)
                    val user = when (userResult) {
                        is Resource.Success -> userResult.data
                        else -> null
                    }
                    
                    if (user != null) {
                        LinkedStudentInfo(student, user, link)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    // Skip invalid student entries
                    null
                }
            }
            
            emit(Resource.Success(result))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể lấy danh sách học sinh"))
        }
    }
}

