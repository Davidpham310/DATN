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
 * Data class cho một học sinh trong lớp (Teacher view)
 */
data class ClassStudentProgress(
    val studentId: String,
    val studentName: String,
    val email: String?,
    val totalLessons: Int,
    val completedLessons: Int,
    val averageProgress: Int,
    val lessons: List<StudentLessonProgressItem>
)

/**
 * Use case: Giáo viên xem tiến độ học tập của tất cả học sinh trong lớp
 * - Lấy danh sách bài học trong lớp
 * - Lấy tiến độ của học sinh (có thể lấy từ repository)
 * - Tính toán tổng hợp tiến độ cho từng học sinh
 */
class GetClassStudentsProgressForTeacherUseCase @Inject constructor(
    private val lessonRepository: ILessonRepository,
    private val progressRepository: IProgressRepository
) {
    private val tag = "GetTeacherProgressUC"

    /**
     * @param classId ID của lớp học
     * @return Flow<Resource<Map<studentId, ClassStudentProgress>>>
     * Giáo viên xem tiến độ của tất cả học sinh trong lớp
     */
    operator fun invoke(classId: String): Flow<Resource<List<ClassStudentProgress>>> {
        Log.d(tag, "🔄 Fetching all students progress for class: $classId")
        // Ghi chú: Cần repository.getClassStudents() để lấy danh sách học sinh
        // Hiện tại chỉ implement skeleton - cần bổ sung repository method
        return lessonRepository.getLessonsByClass(classId)
            .combine(progressRepository.getProgressOverview("")) { lessonsRes, progressRes ->
                when {
                    lessonsRes is Resource.Loading || progressRes is Resource.Loading -> {
                        Resource.Loading()
                    }
                    lessonsRes is Resource.Error -> {
                        Resource.Error(lessonsRes.message ?: "Lỗi lấy danh sách bài học")
                    }
                    progressRes is Resource.Error -> {
                        Resource.Error(progressRes.message ?: "Lỗi lấy dữ liệu tiến độ")
                    }
                    else -> {
                        Resource.Error("Cần implement: thêm classRepository.getClassStudents() vào")
                    }
                }
            }
    }

    /**
     * Lấy tiến độ của một học sinh cụ thể trong lớp
     * @param studentId ID học sinh
     * @param classId ID lớp
     * @param studentName Tên học sinh
     * @param studentEmail Email học sinh
     */
    fun getStudentProgressInClass(
        studentId: String,
        classId: String,
        studentName: String = "",
        studentEmail: String? = null
    ): Flow<Resource<ClassStudentProgress>> {
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
                                className = null,
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
                            ✅ Calculated summary for $studentName:
                            📊 Total: $totalLessons | ✓ Completed: $completedLessons | 📈 Average: $averageProgress%
                        """.trimIndent())

                        Resource.Success(
                            ClassStudentProgress(
                                studentId = studentId,
                                studentName = studentName,
                                email = studentEmail,
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
