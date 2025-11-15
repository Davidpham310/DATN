package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ParentStudent
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IParentRepository
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.parent.ParentStudentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.util.Collections.emptyList
import javax.inject.Inject

/**
 * Thông tin đầy đủ về một học sinh được liên kết với phụ huynh.
 * Kết hợp Student + User + ParentStudent (mối quan hệ).
 */
data class LinkedStudentInfo(
    val student: Student,
    val user: User,
    val parentStudent: ParentStudent
)

/**
 * Use case lấy danh sách học sinh liên kết với một phụ huynh, kèm thông tin user và quan hệ.
 */
class GetLinkedStudentsUseCase @Inject constructor(
    private val parentRepository: IParentRepository,
    private val firebaseDataSource: FirebaseDataSource,
    private val parentStudentService: ParentStudentService
) {

    operator fun invoke(parentId: String): Flow<Resource<List<LinkedStudentInfo>>> = flow {
        try {
            // Thu thập Flow từ repository và enrich thêm thông tin User + quan hệ ParentStudent
            parentRepository.getLinkedStudents(parentId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        emit(Resource.Loading())
                    }
                    is Resource.Error -> {
                        emit(Resource.Error(result.message))
                    }
                    is Resource.Success -> {
                        val students: List<Student> = result.data ?: emptyList()
                        val linkedStudents = mutableListOf<LinkedStudentInfo>()

                        for (student in students) {
                            // Lấy thông tin user tương ứng với student
                            val userResult: Resource<User?> = try {
                                firebaseDataSource.getUserById(student.userId)
                            } catch (e: Exception) {
                                Resource.Error(e.message ?: "Lỗi lấy thông tin người dùng")
                            }

                            val user = when (userResult) {
                                is Resource.Success -> userResult.data
                                else -> null
                            }

                            if (user == null) continue

                            // Lấy thông tin quan hệ parent-student
                            val relationship: ParentStudent? = try {
                                parentStudentService.getRelationship(parentId, student.id)
                            } catch (_: Exception) {
                                null
                            }

                            if (relationship != null) {
                                linkedStudents.add(
                                    LinkedStudentInfo(
                                        student = student,
                                        user = user,
                                        parentStudent = relationship
                                    )
                                )
                            }
                        }

                        emit(Resource.Success(linkedStudents))
                    }
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy danh sách học sinh liên kết"))
        }
    }
}
