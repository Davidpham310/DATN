package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.RelationshipType
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.repository.IAuthRepository
import com.example.datn.domain.repository.IStudentRepository
import com.example.datn.core.network.datasource.FirebaseAuthDataSource
import com.example.datn.core.network.service.parent.ParentStudentService
import com.example.datn.core.network.service.student.StudentService
import com.example.datn.core.network.service.user.UserService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class CreateStudentAccountParams(
    val parentId: String,
    val name: String,
    val email: String,
    val password: String,
    val dateOfBirth: LocalDate,
    val gradeLevel: String
)

class CreateStudentAccountUseCase @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val studentService: StudentService,
    private val userService: UserService,
    private val parentStudentService: ParentStudentService
) : BaseUseCase<CreateStudentAccountParams, Flow<Resource<Pair<User, Student>>>> {
    override fun invoke(params: CreateStudentAccountParams): Flow<Resource<Pair<User, Student>>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Tạo User trong Firebase Auth và Firestore
            val userId = firebaseAuthDataSource.register(
                email = params.email,
                password = params.password,
                name = params.name,
                role = UserRole.STUDENT.name
            )
            
            // 2. Lấy User từ Firestore
            val user = firebaseAuthDataSource.getUserProfile(userId)
            
            // 3. Tạo Student profile trong Firestore
            val now = Instant.now()
            
            // Generate a new document ID first
            val newStudentId = studentService.generateStudentId()
            
            val student = Student(
                id = newStudentId,
                userId = userId,
                dateOfBirth = params.dateOfBirth,
                gradeLevel = params.gradeLevel,
                createdAt = now,
                updatedAt = now
            )
            
            // Save student to Firestore with the generated ID
            studentService.add(newStudentId, student)
            
            // Retrieve the created student to confirm
            val createdStudent = studentService.getStudentById(newStudentId)
                ?: throw Exception("Không thể tạo profile học sinh")
            
            // 4. Tự động tạo liên kết parent-student với quan hệ GUARDIAN
            val linkCreated = parentStudentService.createParentStudentLink(
                parentId = params.parentId,
                studentId = newStudentId,
                relationship = RelationshipType.GUARDIAN,
                isPrimaryGuardian = true
            )
            
            if (!linkCreated) {
                throw Exception("Không thể tạo liên kết với học sinh")
            }
            
            emit(Resource.Success(Pair(user, createdStudent)))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể tạo tài khoản học sinh"))
        }
    }
}

