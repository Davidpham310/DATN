package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.repository.IClassRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

data class AddClassParams(
    val name: String,
    val classCode: String,
    val teacherId: String
)

class AddClassUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(params: AddClassParams): Flow<Resource<Class>> {
        val newClass = Class(
            id = "", // Firebase sẽ tự tạo ID
            teacherId = params.teacherId,
            name = params.name,
            classCode = params.classCode,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return repository.addClass(newClass)
    }
}