package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.repository.IClassRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetApprovedStudentsInClassUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(classId: String): Flow<Resource<List<ClassStudent>>> {
        return repository.getApprovedStudentsInClass(classId)
    }
}