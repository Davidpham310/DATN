package com.example.datn.domain.usecase.progress

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.repository.ILessonRepository
import com.example.datn.domain.repository.IProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take

/**
 * Thông tin tiến độ cho từng bài học của một học sinh
 * 
 * Model được sử dụng chung bởi:
 * - GetStudentProgressForParentUseCase (Parent xem tiến độ con em)
 * - GetClassStudentsProgressForTeacherUseCase (Teacher xem tiến độ học sinh)
 * - GetStudentClassLessonProgressUseCase (Student detail view)
 * - GetStudentAllLessonProgressUseCase (Student all lessons view)
 */
data class StudentLessonProgressItem(
    val lessonId: String,
    val lessonTitle: String,
    val classId: String,
    val className: String?,
    val subject: String?,
    val order: Int,
    val progressPercentage: Int,
    val isCompleted: Boolean
)

/**
 * Lấy danh sách bài học và tiến độ chi tiết của một học sinh trong một lớp cụ thể
 * Được sử dụng bởi: StudentDetailViewModel (teacher module)
 */
class GetStudentClassLessonProgressUseCase @Inject constructor(
    private val lessonRepository: ILessonRepository,
    private val progressRepository: IProgressRepository
) {

    operator fun invoke(studentId: String, classId: String): Flow<Resource<List<StudentLessonProgressItem>>> {
        return lessonRepository.getLessonsByClass(classId)
            .combine(progressRepository.getProgressOverview(studentId)) { lessonsRes, progressRes ->
                when {
                    lessonsRes is Resource.Loading || progressRes is Resource.Loading -> {
                        Resource.Loading()
                    }
                    lessonsRes is Resource.Error -> {
                        Resource.Error(lessonsRes.message ?: "Lỗi lấy danh sách bài học")
                    }
                    progressRes is Resource.Error -> {
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

                        val items = lessons.map { lesson ->
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

                        Resource.Success(items)
                    }
                    else -> {
                        Resource.Error("Lỗi lấy danh sách tiến độ bài học trong lớp")
                    }
                }
            }
    }
}

/**
 * Lấy danh sách bài học và tiến độ chi tiết của một học sinh trên toàn bộ các lớp đang tham gia
 * Được sử dụng bởi: StudentDetailViewModel (parent module)
 */
class GetStudentAllLessonProgressUseCase @Inject constructor(
    private val classRepository: IClassRepository,
    private val lessonRepository: ILessonRepository,
    private val progressRepository: IProgressRepository
) {

    operator fun invoke(studentId: String): Flow<Resource<List<StudentLessonProgressItem>>> = flow {
        emit(Resource.Loading())

        try {
            // Lấy danh sách lớp mà học sinh đang tham gia
            var classesRes: Resource<List<Class>> = Resource.Loading()
            classRepository
                .getClassesByStudent(studentId)
                .filter { it !is Resource.Loading }
                .take(1)
                .collect { result ->
                    classesRes = result
                }

            val finalClassesRes = classesRes
            val classes: List<Class> = when (finalClassesRes) {
                is Resource.Success -> finalClassesRes.data ?: emptyList()
                is Resource.Error -> {
                    emit(Resource.Error(finalClassesRes.message ?: "Lỗi lấy danh sách lớp học của học sinh"))
                    return@flow
                }
                is Resource.Loading -> emptyList()
            }

            // Lấy danh sách bài học theo từng lớp
            val lessonsByClassId = mutableMapOf<String, List<Lesson>>()
            classes.forEach { clazz ->
                var lessonsRes: Resource<List<Lesson>> = Resource.Loading()
                lessonRepository.getLessonsByClass(clazz.id).collect { result ->
                    lessonsRes = result
                }
                val finalLessonsRes = lessonsRes
                val lessons = if (finalLessonsRes is Resource.Success) {
                    (finalLessonsRes.data ?: emptyList()).sortedBy { it.order }
                } else {
                    emptyList()
                }
                lessonsByClassId[clazz.id] = lessons
            }

            // Lấy toàn bộ tiến độ bài học của học sinh
            var progressRes: Resource<List<StudentLessonProgress>> = Resource.Loading()
            progressRepository
                .getProgressOverview(studentId)
                .filter { it !is Resource.Loading }
                .take(1)
                .collect { result ->
                    progressRes = result
                }

            val finalProgressRes = progressRes
            val progressList: List<StudentLessonProgress> = when (finalProgressRes) {
                is Resource.Success -> finalProgressRes.data ?: emptyList()
                is Resource.Error -> {
                    emit(Resource.Error(finalProgressRes.message ?: "Lỗi lấy tiến độ học tập của học sinh"))
                    return@flow
                }
                is Resource.Loading -> emptyList()
            }

            val progressByLessonId: Map<String, StudentLessonProgress> =
                progressList.associateBy { it.lessonId }

            // Build danh sách chi tiết bài học cho tất cả lớp
            val items = classes.flatMap { clazz ->
                val lessons = lessonsByClassId[clazz.id].orEmpty()
                lessons.map { lesson ->
                    val progress = progressByLessonId[lesson.id]
                    StudentLessonProgressItem(
                        lessonId = lesson.id,
                        lessonTitle = lesson.title,
                        classId = clazz.id,
                        className = clazz.name,
                        subject = clazz.subject,
                        order = lesson.order,
                        progressPercentage = progress?.progressPercentage ?: 0,
                        isCompleted = progress?.isCompleted ?: false
                    )
                }
            }

            emit(Resource.Success(items))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi tải danh sách tiến độ bài học"))
        }
    }
}
