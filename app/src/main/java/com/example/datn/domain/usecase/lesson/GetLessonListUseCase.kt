package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.repository.ILessonRepository
import com.example.datn.domain.repository.IProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

enum class LessonStatus {
    LOCKED,
    UNLOCKED,
    COMPLETED
}

data class GetLessonListRequest(
    val studentId: String,
    val classId: String
)

data class LessonWithStatus(
    val lesson: Lesson,
    val status: LessonStatus,
    val progress: StudentLessonProgress?,
    val canAccess: Boolean,
    val lockReason: String?
)

class GetLessonListUseCase @Inject constructor(
    private val lessonRepository: ILessonRepository,
    private val classRepository: IClassRepository,
    private val progressRepository: IProgressRepository
) {
    operator fun invoke(params: GetLessonListRequest): Flow<Resource<List<LessonWithStatus>>> =
        classRepository.isStudentInClass(
            classId = params.classId,
            studentId = params.studentId
        ).flatMapLatest { enrollmentRes ->
            when (enrollmentRes) {
                is Resource.Loading -> {
                    flowOf(Resource.Loading())
                }
                is Resource.Error -> {
                    val message = enrollmentRes.message ?: "Lỗi kiểm tra tham gia lớp học"
                    flowOf(Resource.Error(message))
                }
                is Resource.Success -> {
                    if (enrollmentRes.data != true) {
                        flowOf(Resource.Error("Bạn chưa tham gia lớp học này"))
                    } else {
                        combine(
                            lessonRepository.getLessonsByClass(params.classId),
                            progressRepository.getProgressOverview(params.studentId)
                        ) { lessonsRes, progressRes ->
                            when {
                                lessonsRes is Resource.Loading -> Resource.Loading()
                                lessonsRes is Resource.Error -> Resource.Error(
                                    lessonsRes.message ?: "Lỗi lấy danh sách bài học"
                                )
                                lessonsRes is Resource.Success -> {
                                    val lessons = (lessonsRes as Resource.Success<List<Lesson>>).data?.sortedBy { it.order }.orEmpty()
                                    val progressList = when (progressRes) {
                                        is Resource.Success -> (progressRes as Resource.Success<List<StudentLessonProgress>>).data.orEmpty()
                                        else -> emptyList()
                                    }
                                    val result = buildLessonWithStatus(lessons, progressList)
                                    Resource.Success(result)
                                }
                                else -> Resource.Error("Lỗi lấy danh sách bài học")
                            }
                        }
                    }
                }
            }
        }

    private fun buildLessonWithStatus(
        lessons: List<Lesson>,
        progressList: List<StudentLessonProgress>
    ): List<LessonWithStatus> {
        val progressByLessonId = progressList.associateBy { it.lessonId }

        return lessons.mapIndexed { index, lesson ->
            val progress = progressByLessonId[lesson.id]

            val previousLesson = if (index > 0) lessons[index - 1] else null
            val previousProgress = previousLesson?.let { prev -> progressByLessonId[prev.id] }

            val status: LessonStatus
            val canAccess: Boolean
            val lockReason: String?

            if (progress?.isCompleted == true) {
                status = LessonStatus.COMPLETED
                canAccess = true
                lockReason = null
            } else {
                if (index == 0) {
                    status = LessonStatus.UNLOCKED
                    canAccess = true
                    lockReason = null
                } else {
                    val prevCompleted = previousProgress?.isCompleted == true
                    if (prevCompleted) {
                        status = LessonStatus.UNLOCKED
                        canAccess = true
                        lockReason = null
                    } else {
                        status = LessonStatus.LOCKED
                        canAccess = false
                        lockReason = "Hoàn thành bài học trước để mở khóa"
                    }
                }
            }

            LessonWithStatus(
                lesson = lesson,
                status = status,
                progress = progress,
                canAccess = canAccess,
                lockReason = lockReason
            )
        }
    }
}
