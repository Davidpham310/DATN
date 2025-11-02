package com.example.datn.presentation.student.classmanager

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.ClassStudent

data class StudentClassState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    
    // Danh sách lớp đã tham gia (APPROVED)
    val myClasses: List<Class> = emptyList(),
    
    // Kết quả tìm kiếm lớp
    val searchedClass: Class? = null,
    val searchCode: String = "",
    
    // Enrollment của học sinh trong lớp
    val enrollment: ClassStudent? = null,
    
    // Selected class để xem chi tiết
    val selectedClass: Class? = null,
    
    // Dialog states
    val showJoinClassDialog: Boolean = false,
    val showWithdrawConfirmDialog: Boolean = false,
    
    // Message
    val successMessage: String? = null
) : BaseState
