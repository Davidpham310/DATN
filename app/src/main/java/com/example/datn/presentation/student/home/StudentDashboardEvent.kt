package com.example.datn.presentation.student.home

import com.example.datn.core.base.BaseEvent

sealed class StudentDashboardEvent : BaseEvent {
    object LoadDashboard : StudentDashboardEvent()
    object Refresh : StudentDashboardEvent()
    object ClearError : StudentDashboardEvent()
}
