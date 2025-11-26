package com.example.datn.presentation.teacher.studentdetail

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.User
import com.example.datn.domain.models.Student
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.usecase.progress.StudentLessonProgressItem

/**
 * State for Student Detail Screen
 * Displays comprehensive information about a student's progress and performance
 */
data class StudentDetailState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    
    // Student basic info
    val studentId: String = "",
    val classId: String = "",
    val userInfo: User? = null,
    val studentInfo: Student? = null,
    val enrollmentInfo: ClassStudent? = null,
    
    // Academic progress
    val totalLessons: Int = 0,
    val completedLessons: Int = 0,
    val lessonProgress: Float = 0f,
    
    // Test scores
    val totalTests: Int = 0,
    val completedTests: Int = 0,
    val averageScore: Float = 0f,
    val testResults: List<TestResult> = emptyList(),
    
    // Study / Mini-game overview
    val totalStudyTimeSeconds: Long = 0L,
    val totalMiniGamesPlayed: Int = 0,
    val averageMiniGameScorePercent: Float = 0f,

    // Assignment/MiniGame progress
    val totalAssignments: Int = 0,
    val completedAssignments: Int = 0,
    val assignmentProgress: Float = 0f,
    
    // Attendance/Participation
    val enrolledDate: String = "",
    val lastActive: String = "",
    
    // Detailed per-lesson progress for the current class
    val lessonProgressItems: List<StudentLessonProgressItem> = emptyList(),
    
    // UI State
    val selectedTab: Int = 0
) : BaseState

/**
 * Data class for test result display
 */
data class TestResult(
    val testId: String = "",
    val testTitle: String = "",
    val score: Float = 0f,
    val maxScore: Float = 100f,
    val completedDate: String = "",
    val passed: Boolean = false
)
