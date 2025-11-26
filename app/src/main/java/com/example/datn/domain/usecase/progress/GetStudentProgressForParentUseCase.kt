package com.example.datn.domain.usecase.progress

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.repository.ILessonRepository
import com.example.datn.domain.repository.IProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Data class cho Parent xem tiến độ học tập của con
 */
data class StudentProgressForParent(
    val studentId: String,
    val studentName: String,
    val classId: String,
    val className: String?,
    val totalLessons: Int,
    val completedLessons: Int,
    val averageProgress: Int,
    val lessons: List<StudentLessonProgressItem>
)

/**
 * Use case: Phụ huynh xem tiến độ học tập của học sinh (con em)
 * - Lấy danh sách lớp học của học sinh
 * - Lấy danh sách bài học trong lớp
 * - Lấy tiến độ học tập của học sinh
 * - Tính toán tổng hợp tiến độ
 */
class GetStudentProgressForParentUseCase @Inject constructor(
    private val lessonRepository: ILessonRepository,
    private val progressRepository: IProgressRepository
) {
    private val tag = "GetParentProgressUC"

    /**
     * @param studentId ID của học sinh (con em của phụ huynh)
     * @param classId ID của lớp học
     * @param studentName Tên học sinh (để hiển thị)
     * @param className Tên lớp học (để hiển thị)
     */
    operator fun invoke(
        studentId: String,
        classId: String,
        studentName: String = "",
        className: String? = null
    ): Flow<Resource<StudentProgressForParent>> {
        Log.d(tag, "🔄 Fetching progress for student: $studentName (ID: $studentId) in class: $classId")
        return lessonRepository.getLessonsByClass(classId)
            .combine(progressRepository.getProgressOverview(studentId)) { lessonsRes, progressRes ->
                Log.d(tag, "📋 Lessons resource: ${lessonsRes::class.simpleName}, Progress resource: ${progressRes::class.simpleName}")
                
                when {
                    lessonsRes is Resource.Loading || progressRes is Resource.Loading -> {
                        Log.d(tag, "⏳ Loading lessons or progress data...")
                        Resource.Loading()
                    }
                    lessonsRes is Resource.Error -> {
                        Log.e(tag, "❌ Error fetching lessons: ${lessonsRes.message}")
                        Resource.Error(lessonsRes.message ?: "Lỗi lấy danh sách bài học")
                    }
                    progressRes is Resource.Error -> {
                        Log.e(tag, "❌ Error fetching progress: ${progressRes.message}")
                        Resource.Error(progressRes.message ?: "Lỗi lấy tiến độ học tập")
                    }
                    lessonsRes is Resource.Success && progressRes is Resource.Success -> {
                        val lessonsSuccess = lessonsRes as Resource.Success<List<Lesson>>
                        val progressSuccess = progressRes as Resource.Success<List<StudentLessonProgress>>

                        val lessons: List<Lesson> = (lessonsSuccess.data ?: emptyList())
                            .sortedBy { it.order }
                        val progressList: List<StudentLessonProgress> = progressSuccess.data ?: emptyList()
                        val progressByLessonId: Map<String, StudentLessonProgress> =
                            progressList.associateBy { it.lessonId }

                        Log.d(tag, "📚 Received ${lessons.size} lessons and ${progressList.size} progress records")

                        // Tạo danh sách bài học với tiến độ
                        val lessonItems = lessons.map { lesson ->
                            val progress = progressByLessonId[lesson.id]
                            StudentLessonProgressItem(
                                lessonId = lesson.id,
                                lessonTitle = lesson.title,
                                classId = lesson.classId,
                                className = className,
                                subject = null,
                                order = lesson.order,
                                progressPercentage = progress?.progressPercentage ?: 0,
                                isCompleted = progress?.isCompleted ?: false
                            )
                        }

                        // Tính toán tổng hợp
                        val totalLessons = lessons.size
                        val completedLessons = lessonItems.count { it.isCompleted }
                        val averageProgress = if (lessonItems.isNotEmpty()) {
                            lessonItems.map { it.progressPercentage }.average().toInt()
                        } else {
                            0
                        }

                        Log.d(tag, """
                            ✅ Calculated summary:
                            📊 Total: $totalLessons | ✓ Completed: $completedLessons | 📈 Average: $averageProgress%
                        """.trimIndent())

                        Resource.Success(
                            StudentProgressForParent(
                                studentId = studentId,
                                studentName = studentName,
                                classId = classId,
                                className = className,
                                totalLessons = totalLessons,
                                completedLessons = completedLessons,
                                averageProgress = averageProgress,
                                lessons = lessonItems
                            )
                        )
                    }
                    else -> {
                        Log.e(tag, "❌ Unknown error state")
                        Resource.Error("Lỗi lấy tiến độ học tập của học sinh")
                    }
                }
            }
    }
}
