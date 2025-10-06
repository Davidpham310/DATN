package com.example.datn.data.mapper

import com.example.datn.data.local.entities.ResourceEntity
import com.example.datn.domain.models.Resource

fun ResourceEntity.toDomain(): Resource {
    return Resource(
        id = id,
        title = title,
        subject = subject,
        url = url,
        uploadedBy = uploadedBy,
        createdAt = createdAt
    )
}

fun Resource.toEntity(): ResourceEntity {
    return ResourceEntity(
        id = id,
        title = title,
        subject = subject,
        url = url,
        uploadedBy = uploadedBy,
        createdAt = createdAt
    )
}
