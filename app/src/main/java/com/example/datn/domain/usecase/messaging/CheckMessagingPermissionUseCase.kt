package com.example.datn.domain.usecase.messaging

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IMessagingPermissionRepository
import javax.inject.Inject

/**
 * Use Case để kiểm tra quyền nhắn tin giữa 2 users
 */
class CheckMessagingPermissionUseCase @Inject constructor(
    private val permissionRepository: IMessagingPermissionRepository
) {
    /**
     * Kiểm tra user1 có được phép nhắn tin với user2 không
     * @return Resource<Boolean>
     */
    suspend operator fun invoke(user1Id: String, user2Id: String): Resource<Boolean> {
        return permissionRepository.canMessageUser(user1Id, user2Id)
    }
}
