package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.RelationshipType
import com.example.datn.core.network.service.parent.ParentStudentService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UpdateRelationshipParams(
    val parentId: String,
    val studentId: String,
    val relationship: RelationshipType,
    val isPrimaryGuardian: Boolean
)

class UpdateRelationshipUseCase @Inject constructor(
    private val parentStudentService: ParentStudentService
) : BaseUseCase<UpdateRelationshipParams, Flow<Resource<Unit>>> {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    override fun invoke(params: UpdateRelationshipParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val docId = "${params.parentId}_${params.studentId}"
            
            // Cập nhật relationship và isPrimaryGuardian
            firestore.collection("parent_student")
                .document(docId)
                .update(
                    mapOf(
                        "relationship" to params.relationship.name,
                        "isPrimaryGuardian" to params.isPrimaryGuardian
                    )
                )
                .await()
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể cập nhật quan hệ"))
        }
    }
}
