package com.example.datn.presentation.student.tests.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.TestStatus
import java.time.Duration
import java.time.Instant

data class StudentTestListState(
    val tests: List<TestWithStatus> = emptyList(),
    val upcomingTests: List<TestWithStatus> = emptyList(),
    val ongoingTests: List<TestWithStatus> = emptyList(),
    val completedTests: List<TestWithStatus> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState

data class TestWithStatus(
    val test: Test,
    val result: StudentTestResult?,
    val status: TestStatus,
    val timeRemaining: String? = null,
    val isOverdue: Boolean = false,
    val canTakeTest: Boolean = true
) {
    val hasResult: Boolean
        get() = result != null
    
    val scoreText: String?
        get() = result?.let { "${it.score}/${test.totalScore}" }
    
    val statusText: String
        get() = when (status) {
            TestStatus.UNSUBMITTED -> "Chưa làm"
            TestStatus.IN_PROGRESS -> "Đang làm"
            TestStatus.SUBMITTED -> "Đã nộp"
            TestStatus.COMPLETED -> "Hoàn thành"
            TestStatus.GRADED -> "Đã chấm điểm"
            TestStatus.OVERDUE -> "Quá hạn"
        }
    
    fun getTimeRemainingText(): String? {
        val now = Instant.now()
        return when {
            now.isBefore(test.startTime) -> {
                val days = Duration.between(now, test.startTime).toDays()
                "Còn $days ngày"
            }
            now.isAfter(test.endTime) -> "Đã kết thúc"
            else -> {
                val hours = Duration.between(now, test.endTime).toHours()
                when {
                    hours > 24 -> "Còn ${hours / 24} ngày"
                    hours > 0 -> "Còn $hours giờ"
                    else -> {
                        val minutes = Duration.between(now, test.endTime).toMinutes()
                        "Còn $minutes phút"
                    }
                }
            }
        }
    }
}
