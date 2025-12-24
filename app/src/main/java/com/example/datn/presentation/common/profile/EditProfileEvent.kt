package com.example.datn.presentation.common.profile

import com.example.datn.core.base.BaseEvent
import java.io.InputStream
import java.time.LocalDate

sealed class EditProfileEvent : BaseEvent {
    // Common events
    data class LoadProfile(val userId: String, val role: String) : EditProfileEvent()
    data class UpdateName(val name: String) : EditProfileEvent()
    object SaveProfile : EditProfileEvent()
    object ClearMessages : EditProfileEvent()
    data class UploadAvatar(val inputStream: InputStream, val fileName: String, val fileSize: Long, val contentType: String) : EditProfileEvent()
    data class UpdateAvatarProgress(val uploaded: Long, val total: Long) : EditProfileEvent()
    
    // Student specific events
    data class UpdateGradeLevel(val gradeLevel: String) : EditProfileEvent()
    data class UpdateDateOfBirth(val dateOfBirth: LocalDate) : EditProfileEvent()
    
    // Teacher specific events
    data class UpdateSpecialization(val specialization: String) : EditProfileEvent()
    data class UpdateLevel(val level: String) : EditProfileEvent()
    data class UpdateExperienceYears(val years: String) : EditProfileEvent()
}
