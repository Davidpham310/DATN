package com.example.datn.data.mapper

import com.example.datn.data.local.entities.ClassMemberEntity
import com.example.datn.domain.models.ClassMember

fun ClassMemberEntity.toDomain(): ClassMember {
    return ClassMember(
        id = id,
        classId = classId,
        studentId = studentId,
        status = status,
        joinedAt = joinedAt
    )
}

fun ClassMember.toEntity(): ClassMemberEntity {
    return ClassMemberEntity(
        id = id,
        classId = classId,
        studentId = studentId,
        status = status,
        joinedAt = joinedAt
    )
}
