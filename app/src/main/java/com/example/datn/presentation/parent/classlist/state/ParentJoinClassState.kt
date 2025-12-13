package com.example.datn.presentation.parent.classlist.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo

data class ParentJoinClassState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    
    // Danh sách học sinh được liên kết của phụ huynh
    val linkedStudents: List<LinkedStudentInfo> = emptyList(),
    
    // Học sinh được chọn để tham gia lớp
    val selectedStudent: LinkedStudentInfo? = null,
    
    // Tìm kiếm lớp
    val searchQuery: String = "",
    val searchType: SearchType = SearchType.BY_CODE,
    val searchResults: List<Class> = emptyList(),
    val selectedClass: Class? = null,
    
    // Enrollment status
    val enrollment: ClassStudent? = null,
    
    // Map of all enrollments for selected student: classId -> ClassStudent
    val studentEnrollments: Map<String, ClassStudent> = emptyMap(),
    
    // Dialog states
    val showStudentSelectionDialog: Boolean = false,
    val showClassDetailsDialog: Boolean = false,
    
    // Message
    val successMessage: String? = null
) : BaseState

enum class SearchType {
    BY_CODE,    // Tìm theo mã lớp
    BY_NAME,    // Tìm theo tên lớp
    BY_SUBJECT  // Tìm theo môn học
}
