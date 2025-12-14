package com.example.datn.domain.usecase.parentstudent
 
import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.network.datasource.FirebaseAuthDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.UserRole
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.student.StudentService
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class CreateStudentAccountParams(
    val parentId: String,  // Parent's id from parents collection (not userId)
    val email: String,
    val password: String,
    val name: String,
    val dateOfBirth: LocalDate,
    val gradeLevel: String,
    val relationship: String,
    val isPrimaryGuardian: Boolean = true
)
 
class CreateStudentAccountForParentUseCase @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val studentService: StudentService,
    private val firebaseDataSource: FirebaseDataSource
) : BaseUseCase<CreateStudentAccountParams, Flow<Resource<Unit>>> {
 
    override fun invoke(params: CreateStudentAccountParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val studentUserId = firebaseAuthDataSource.registerAsSecondary(
                email = params.email,
                password = params.password,
                name = params.name,
                role = UserRole.STUDENT.name
            )

            // 2. Tạo hồ sơ Student tương ứng
            val now = Instant.now()
            val studentId = studentUserId

            val student = Student(
                id = studentId,
                userId = studentUserId,
                dateOfBirth = params.dateOfBirth,
                gradeLevel = params.gradeLevel,
                createdAt = now,
                updatedAt = now
            )

            // Lưu Student vào collection "students" với id cố định = studentId
            studentService.add(studentId, student)

            // 3. Liên kết phụ huynh với học sinh
            // parentId là ID từ collection parents (không phải userId)
            firebaseDataSource.linkParentToStudent(
                studentId = studentId,
                parentId = params.parentId,
                relationship = params.relationship,
                isPrimaryGuardian = params.isPrimaryGuardian
            )

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi tạo tài khoản học sinh"))
        }
    }
}
