package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.repository.ILessonContentRepository
import com.example.datn.domain.repository.IProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf


data class GetLessonContentsRequest(
    val studentId: String,
    val lessonId: String
)

data class LessonContentWithStatus(
    val content: LessonContent,
    val canAccess: Boolean,
    val isViewed: Boolean,
    val lockReason: String?
)

data class GetLessonContentsResponse(
    val lesson: Lesson,
    val contents: List<LessonContentWithStatus>,
    val progress: StudentLessonProgress?,
    val totalContents: Int,
    val viewedContents: Int,
    val currentContentIndex: Int
)

class GetLessonContentsUseCase @Inject constructor(
    private val classRepository: IClassRepository,
    private val contentRepository: ILessonContentRepository,
    private val progressRepository: IProgressRepository,
    private val getLessonById: GetLessonByIdUseCase,
    private val getLessonList: GetLessonListUseCase
) {
    operator fun invoke(params: GetLessonContentsRequest): Flow<Resource<GetLessonContentsResponse>> =
        getLessonById(params.lessonId).flatMapLatest { lessonRes ->
            when (lessonRes) {
                is Resource.Loading -> flowOf(Resource.Loading())
                is Resource.Error -> {
                    val message = lessonRes.message ?: "Không tìm thấy bài học"
                    flowOf(Resource.Error(message))
                }
                is Resource.Success -> {
                    val lesson = lessonRes.data
                    if (lesson == null) {
                        flowOf(Resource.Error("Không tìm thấy bài học"))
                    } else {
                        classRepository.isStudentInClass(
                            classId = lesson.classId,
                            studentId = params.studentId
                        ).flatMapLatest { enrollmentRes ->
                            when (enrollmentRes) {
                                is Resource.Loading -> flowOf(Resource.Loading())
                                is Resource.Error -> {
                                    val message = enrollmentRes.message ?: "Lỗi kiểm tra tham gia lớp học"
                                    flowOf(Resource.Error(message))
                                }
                                is Resource.Success -> {
                                    val canReadFlow: Flow<Resource<Boolean>> = if (enrollmentRes.data == true) {
                                        flowOf(Resource.Success(true))
                                    } else {
                                        classRepository.hasPendingEnrollment(
                                            classId = lesson.classId,
                                            studentId = params.studentId
                                        )
                                    }

                                    canReadFlow.flatMapLatest { canReadRes ->
                                        when (canReadRes) {
                                            is Resource.Loading -> flowOf(Resource.Loading())
                                            is Resource.Error -> flowOf(Resource.Error(canReadRes.message ?: "Lỗi kiểm tra tham gia lớp học"))
                                            is Resource.Success -> {
                                                if (canReadRes.data != true) {
                                                    flowOf(Resource.Error("Bạn chưa tham gia lớp học này"))
                                                } else {
                                                    combine(
                                                        getLessonList(
                                                            GetLessonListRequest(
                                                                studentId = params.studentId,
                                                                classId = lesson.classId
                                                            )
                                                        ),
                                                        contentRepository.getContentByLesson(params.lessonId),
                                                        progressRepository.getLessonProgress(
                                                            studentId = params.studentId,
                                                            lessonId = params.lessonId
                                                        )
                                                    ) { lessonListRes, contentsRes, progressRes ->
                                                        when (lessonListRes) {
                                                            is Resource.Loading -> Resource.Loading()
                                                            is Resource.Error -> Resource.Error(
                                                                lessonListRes.message ?: "Lỗi lấy danh sách bài học"
                                                            )
                                                            is Resource.Success -> {
                                                                val lessonWithStatus =
                                                                    lessonListRes.data?.find { it.lesson.id == params.lessonId }

                                                                if (lessonWithStatus == null) {
                                                                    Resource.Error("Không tìm thấy bài học")
                                                                } else if (!lessonWithStatus.canAccess || lessonWithStatus.status == LessonStatus.LOCKED) {
                                                                    Resource.Error(lessonWithStatus.lockReason ?: "Bài học đang bị khóa")
                                                                } else {
                                                                    when (contentsRes) {
                                                                        is Resource.Loading -> Resource.Loading()
                                                                        is Resource.Error -> Resource.Error(
                                                                            contentsRes.message
                                                                                ?: "Lỗi lấy nội dung bài học"
                                                                        )
                                                                        is Resource.Success -> {
                                                                            val contents =
                                                                                contentsRes.data?.sortedBy { it.order }.orEmpty()

                                                                            val progress =
                                                                                (progressRes as? Resource.Success)?.data

                                                                            val totalContents = contents.size
                                                                            val viewedIds = mutableSetOf<String>()

                                                                            if (progress != null && totalContents > 0) {
                                                                                val percentage =
                                                                                    progress.progressPercentage.coerceIn(0, 100)
                                                                                val viewedCount =
                                                                                    (percentage * totalContents) / 100

                                                                                contents.take(viewedCount)
                                                                                    .forEach { viewedIds.add(it.id) }

                                                                                val lastId = progress.lastAccessedContentId
                                                                                if (!lastId.isNullOrBlank()) {
                                                                                    val lastIndex = contents.indexOfFirst {
                                                                                        it.id == lastId
                                                                                    }
                                                                                    if (lastIndex in contents.indices) {
                                                                                        viewedIds.add(contents[lastIndex].id)
                                                                                    }
                                                                                }
                                                                            }

                                                                            val contentWithStatus = contents.mapIndexed { index, content ->
                                                                                val isViewed = viewedIds.contains(content.id)

                                                                                val canAccess = if (index == 0) {
                                                                                    true
                                                                                } else {
                                                                                    val previousContent = contents[index - 1]
                                                                                    viewedIds.contains(previousContent.id)
                                                                                }

                                                                                val lockReason = if (!canAccess) {
                                                                                    "Hãy hoàn thành nội dung trước đó để mở khóa"
                                                                                } else {
                                                                                    null
                                                                                }

                                                                                LessonContentWithStatus(
                                                                                    content = content,
                                                                                    canAccess = canAccess,
                                                                                    isViewed = isViewed,
                                                                                    lockReason = lockReason
                                                                                )
                                                                            }

                                                                            val computedViewedCount =
                                                                                contentWithStatus.count { it.isViewed }
                                                                            val firstUnviewedIndex =
                                                                                contentWithStatus.indexOfFirst { !it.isViewed }
                                                                            val currentContentIndex = when {
                                                                                totalContents == 0 -> 0
                                                                                firstUnviewedIndex >= 0 -> firstUnviewedIndex
                                                                                else -> totalContents - 1
                                                                            }

                                                                            val response = GetLessonContentsResponse(
                                                                                lesson = lesson,
                                                                                contents = contentWithStatus,
                                                                                progress = progress,
                                                                                totalContents = totalContents,
                                                                                viewedContents = computedViewedCount,
                                                                                currentContentIndex = currentContentIndex
                                                                            )

                                                                            Resource.Success(response)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
