package com.example.datn.domain.usecase.messaging

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.repository.IMessagingPermissionRepository
import com.example.datn.domain.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use Case để lấy danh sách người được phép nhắn tin
 * Dựa trên role của user hiện tại và mối quan hệ
 */
class GetAllowedRecipientsUseCase @Inject constructor(
    private val permissionRepository: IMessagingPermissionRepository,
    private val userRepository: IUserRepository
) {
    
    /**
     * Lấy tất cả người được phép nhắn tin (tất cả roles)
     */
    operator fun invoke(userId: String): Flow<Resource<List<User>>> {
        return permissionRepository.getAllAllowedRecipients(userId)
    }
    
    /**
     * Lấy người được phép nhắn tin theo role cụ thể
     * @param userId - ID user hiện tại
     * @param filterByRole - Lọc theo role (null = tất cả)
     */
    fun getByRole(userId: String, filterByRole: UserRole? = null): Flow<Resource<List<User>>> = flow {
        permissionRepository.getAllAllowedRecipients(userId).collect { resource ->
            when (resource) {
                is Resource.Loading -> emit(Resource.Loading())
                is Resource.Success -> {
                    val filtered = if (filterByRole != null) {
                        resource.data?.filter { it.role == filterByRole } ?: emptyList()
                    } else {
                        resource.data ?: emptyList()
                    }
                    emit(Resource.Success(filtered))
                }
                is Resource.Error -> emit(Resource.Error(resource.message ?: "Lỗi không xác định"))
            }
        }
    }
    
    /**
     * Lấy học sinh (cho Teacher)
     */
    fun getStudents(teacherId: String): Flow<Resource<List<User>>> {
        return permissionRepository.getStudentsInMyClasses(teacherId)
    }
    
    /**
     * Lấy phụ huynh (cho Teacher)
     */
    fun getParents(teacherId: String): Flow<Resource<List<User>>> {
        return permissionRepository.getParentsOfMyStudents(teacherId)
    }
    
    /**
     * Lấy giáo viên (cho Student/Parent)
     */
    fun getTeachers(userId: String, role: UserRole): Flow<Resource<List<User>>> {
        return when (role) {
            UserRole.STUDENT -> permissionRepository.getMyTeachers(userId)
            UserRole.PARENT -> permissionRepository.getTeachersOfMyChildren(userId)
            else -> flow { emit(Resource.Success(emptyList())) }
        }
    }
    
    /**
     * Lấy con (cho Parent)
     */
    fun getChildren(parentId: String): Flow<Resource<List<User>>> {
        return permissionRepository.getMyChildren(parentId)
    }
    
    /**
     * Lấy bạn cùng lớp (cho Student)
     */
    fun getClassmates(studentId: String): Flow<Resource<List<User>>> {
        return permissionRepository.getMyClassmates(studentId)
    }
    
    /**
     * Tìm kiếm trong danh sách được phép
     * @param query - Từ khóa tìm kiếm (tên, email)
     */
    fun search(userId: String, query: String): Flow<Resource<List<User>>> = flow {
        permissionRepository.getAllAllowedRecipients(userId).collect { resource ->
            when (resource) {
                is Resource.Loading -> emit(Resource.Loading())
                is Resource.Success -> {
                    val filtered = resource.data?.filter { user ->
                        user.name.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true)
                    } ?: emptyList()
                    emit(Resource.Success(filtered))
                }
                is Resource.Error -> emit(Resource.Error(resource.message ?: "Lỗi tìm kiếm"))
            }
        }
    }
}
