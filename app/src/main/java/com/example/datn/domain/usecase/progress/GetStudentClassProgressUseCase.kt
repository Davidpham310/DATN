package com.example.datn.domain.usecase.progress

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.repository.ILessonRepository
import com.example.datn.domain.repository.IProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class StudentClassProgressOverview(
    val totalLessons: Int,
    val completedLessons: Int,
    val averageLessonProgressPercent: Int
)

class GetStudentClassProgressUseCase @Inject constructor(
    private val lessonRepository: ILessonRepository,
    private val progressRepository: IProgressRepository
) {

    operator fun invoke(studentId: String, classId: String): Flow<Resource<StudentClassProgressOverview>> {
        return lessonRepository.getLessonsByClass(classId)
            .combine(progressRepository.getProgressOverview(studentId)) { lessonsRes, progressRes ->
                when {
                    lessonsRes is Resource.Loading || progressRes is Resource.Loading -> {
                        Resource.Loading()
                    }
                    lessonsRes is Resource.Error -> {
                        Resource.Error(lessonsRes.message)
                    }
                    progressRes is Resource.Error -> {
                        Resource.Error(progressRes.message)
                    }
                    lessonsRes is Resource.Success && progressRes is Resource.Success -> {
                        val lessons: List<Lesson> = (lessonsRes as Resource.Success<List<Lesson>>).data ?: emptyList()
                        val allProgress: List<StudentLessonProgress> = (progressRes as Resource.Success<List<StudentLessonProgress>>).data ?: emptyList()
                        val lessonIds = lessons.map { it.id }.toSet()
                        val classProgress = allProgress.filter { it.lessonId in lessonIds }

                        val totalLessons = lessons.size
                        val completedLessons = classProgress.count { it.isCompleted }
                        val averageLessonProgressPercent =
                            if (classProgress.isNotEmpty()) {
                                classProgress.map { it.progressPercentage }.average().toInt()
                            } else {
                                0
                            }

                        Resource.Success(
                            StudentClassProgressOverview(
                                totalLessons = totalLessons,
                                completedLessons = completedLessons,
                                averageLessonProgressPercent = averageLessonProgressPercent
                            )
                        )
                    }
                    else -> {
                        Resource.Error("Lỗi lấy tiến độ lớp học")
                    }
                }
            }
    }
}
