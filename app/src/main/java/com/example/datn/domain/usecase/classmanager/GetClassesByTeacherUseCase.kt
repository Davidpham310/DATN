package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.repository.IClassRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClassesByTeacherUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(teacherId: String): Flow<Resource<List<Class>>> {
        return repository.getClassesByTeacher(teacherId)
    }
}