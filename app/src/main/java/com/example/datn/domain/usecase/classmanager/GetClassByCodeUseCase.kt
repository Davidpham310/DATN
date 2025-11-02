package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.repository.IClassRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClassByCodeUseCase @Inject constructor(
    private val classRepository: IClassRepository
) {
    operator fun invoke(classCode: String): Flow<Resource<Class?>> {
        return classRepository.getClassByCode(classCode)
    }
}
