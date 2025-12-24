package com.example.datn.presentation.common.profile

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.User
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.Teacher
import com.example.datn.domain.models.Parent
import java.time.LocalDate

data class EditProfileState(
    // Common fields
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isSuccess: Boolean = false,
    val user: User? = null,
    val name: String = "",
    
    // Avatar fields
    val isUploadingAvatar: Boolean = false,
    val avatarUploadProgress: Float = 0f,
    val avatarUrl: String? = null,
    
    // Student specific
    val student: Student? = null,
    val gradeLevel: String = "",
    val dateOfBirth: LocalDate? = null,
    
    // Teacher specific
    val teacher: Teacher? = null,
    val specialization: String = "",
    val level: String = "",
    val experienceYears: String = "0",
    
    // Parent specific
    val parent: Parent? = null
) : BaseState
