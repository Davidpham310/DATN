package com.example.datn.domain.usecase.parentstudent

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ParentStudent
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IParentRepository
import com.example.datn.data.remote.datasource.FirebaseDataSource
import com.example.datn.data.remote.service.parent.ParentStudentService
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
        Log.d("GetLinkedStudentsUseCase", "invoke() called for parentId=$parentId")
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
                        Log.d("GetLinkedStudentsUseCase", "Loaded ${students.size} Student(s) from repository for parentId=$parentId")

                        val linkedStudents = mutableListOf<LinkedStudentInfo>()

                        for (student in students) {
                            Log.d(
                                "GetLinkedStudentsUseCase",
                                "Processing Student id=${student.id}, userId=${student.userId} for parentId=$parentId"
                            )
                            // Lấy thông tin user tương ứng với student
                            val userResult: Resource<User?> = try {
                                firebaseDataSource.getUserById(student.userId)
                            } catch (e: Exception) {
                                Resource.Error(e.message ?: "Lỗi lấy thông tin người dùng")
                            }

                            val user = when (userResult) {
                                is Resource.Success -> userResult.data
                                is Resource.Error -> {
                                    Log.e(
                                        "GetLinkedStudentsUseCase",
                                        "Failed to load User for userId=${student.userId}: ${userResult.message}"
                                    )
                                    null
                                }
                                else -> null
                            }

                            if (user == null) {
                                Log.w(
                                    "GetLinkedStudentsUseCase",
                                    "Skipping Student id=${student.id} because User not found for userId=${student.userId}"
                                )
                                continue
                            }

                            // Lấy thông tin quan hệ parent-student
                            val relationship: ParentStudent? = try {
                                parentStudentService.getRelationship(parentId, student.id)
                            } catch (e: Exception) {
                                Log.e(
                                    "GetLinkedStudentsUseCase",
                                    "Error loading relationship for parentId=$parentId, studentId=${student.id}: ${e.message}"
                                )
                                null
                            }

                            if (relationship != null) {
                                Log.d(
                                    "GetLinkedStudentsUseCase",
                                    "Found relationship for parentId=$parentId, studentId=${student.id}: $relationship"
                                )
                                linkedStudents.add(
                                    LinkedStudentInfo(
                                        student = student,
                                        user = user,
                                        parentStudent = relationship
                                    )
                                )
                            } else {
                                Log.w(
                                    "GetLinkedStudentsUseCase",
                                    "No relationship found for parentId=$parentId, studentId=${student.id}"
                                )
                            }
                        }

                        Log.d(
                            "GetLinkedStudentsUseCase",
                            "Emitting ${linkedStudents.size} LinkedStudentInfo item(s) for parentId=$parentId"
                        )
                        emit(Resource.Success(linkedStudents))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GetLinkedStudentsUseCase", "invoke() error for parentId=$parentId: ${e.message}")
            emit(Resource.Error(e.message ?: "Lỗi lấy danh sách học sinh liên kết"))
        }
    }
}
