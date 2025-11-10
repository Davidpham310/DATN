package com.example.datn.presentation.student.lessons

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.Lesson

data class StudentClassDetailState(
    val classInfo: Class? = null,
    val lessons: List<Lesson> = emptyList(),
    val studentCount: Int = 0,
    val showWithdrawDialog: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState
