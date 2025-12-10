package com.example.datn.presentation.common.profile

import com.example.datn.core.base.BaseEvent
import java.time.LocalDate

sealed class EditProfileEvent : BaseEvent {
    // Common events
    data class LoadProfile(val userId: String, val role: String) : EditProfileEvent()
    object SaveProfile : EditProfileEvent()
    object ClearMessages : EditProfileEvent()
    
    // Student specific events
    data class UpdateGradeLevel(val gradeLevel: String) : EditProfileEvent()
    data class UpdateDateOfBirth(val dateOfBirth: LocalDate) : EditProfileEvent()
    
    // Teacher specific events
    data class UpdateSpecialization(val specialization: String) : EditProfileEvent()
    data class UpdateLevel(val level: String) : EditProfileEvent()
    data class UpdateExperienceYears(val years: String) : EditProfileEvent()
}
