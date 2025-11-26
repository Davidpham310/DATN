package com.example.datn.domain.usecase.progress

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.repository.ILessonRepository
import com.example.datn.domain.repository.IMiniGameRepository
import com.example.datn.domain.repository.IProgressRepository
import com.example.datn.domain.repository.ITestRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

private const val TAG = "GetStudentDashboardUC"

class GetStudentDashboardUseCase @Inject constructor(
    private val progressRepository: IProgressRepository,
    private val classRepository: IClassRepository,
    private val lessonRepository: ILessonRepository,
    private val testRepository: ITestRepository,
    private val miniGameRepository: IMiniGameRepository,
    private val getStudyTimeStatistics: GetStudyTimeStatisticsUseCase
) {

    operator fun invoke(studentId: String): Flow<Resource<StudentDashboard>> = flow {
        emit(Resource.Loading())

        try {
            // 1. Load core data
            val progressRes = progressRepository
                .getProgressOverview(studentId)
                .first { it !is Resource.Loading }
            val progressList: List<StudentLessonProgress> = when (progressRes) {
                is Resource.Success -> progressRes.data ?: emptyList()
                is Resource.Error -> {
                    val msg = if (progressRes.message.isNullOrBlank()) {
                        "Lỗi lấy tiến độ học tập"
                    } else {
                        progressRes.message
                    }
                    emit(Resource.Error(msg))
                    return@flow
                }
                is Resource.Loading -> {
                    emit(Resource.Loading())
                    emptyList()
                }
            }
            Log.d(TAG, "Loaded progress overview: count=${progressList.size}")

            val classesRes = classRepository
                .getClassesByStudent(studentId)
                .first { it !is Resource.Loading }
            val classes: List<Class> = when (classesRes) {
                is Resource.Success -> classesRes.data ?: emptyList()
                is Resource.Error -> {
                    val msg = if (classesRes.message.isNullOrBlank()) {
                        "Lỗi lấy danh sách lớp học"
                    } else {
                        classesRes.message
                    }
                    emit(Resource.Error(msg))
                    return@flow
                }
                is Resource.Loading -> {
                    emit(Resource.Loading())
                    emptyList()
                }
            }
            val classById: Map<String, Class> = classes.associateBy { it.id }
            val classIds: List<String> = classes.map { it.id }
            Log.d(TAG, "Loaded classes: count=${classes.size}")

            // Lessons by class
            val lessonsByClassId = mutableMapOf<String, List<Lesson>>()
            classes.forEach { clazz ->
                val lessonsRes = lessonRepository
                    .getLessonsByClass(clazz.id)
                    .first { it !is Resource.Loading }

                val lessons: List<Lesson> = when (lessonsRes) {
                    is Resource.Success -> lessonsRes.data ?: emptyList()
                    is Resource.Error -> {
                        Log.w(TAG, "Error loading lessons for classId=${clazz.id}: ${lessonsRes.message}")
                        emptyList()
                    }
                    is Resource.Loading -> emptyList()
                }

                lessonsByClassId[clazz.id] = lessons
            }
            val lessonById: Map<String, Lesson> = lessonsByClassId.values
                .flatten()
                .associateBy { it.id }
            Log.d(TAG, "Loaded lessons by class: totalLessons=${lessonById.size}")

            // Tests metadata
            val testsRes = if (classIds.isNotEmpty()) {
                testRepository
                    .getTestsByClasses(classIds)
                    .first { it !is Resource.Loading }
            } else {
                Resource.Success(emptyList())
            }
            val tests: List<com.example.datn.domain.models.Test> = when (testsRes) {
                is Resource.Success -> testsRes.data ?: emptyList()
                is Resource.Error -> {
                    val msg = if (testsRes.message.isNullOrBlank()) {
                        "Lỗi lấy danh sách bài kiểm tra"
                    } else {
                        testsRes.message
                    }
                    emit(Resource.Error(msg))
                    return@flow
                }
                is Resource.Loading -> {
                    emit(Resource.Loading())
                    emptyList()
                }
            }
            val testById: Map<String, com.example.datn.domain.models.Test> = tests.associateBy { it.id }
            Log.d(TAG, "Loaded tests metadata: count=${tests.size}")

            // Student test results
            val studentTestResultsRes = testRepository
                .getStudentTestResults(studentId)
                .first { it !is Resource.Loading }
            val studentTestResults: List<StudentTestResult> = when (studentTestResultsRes) {
                is Resource.Success -> studentTestResultsRes.data ?: emptyList()
                is Resource.Error -> {
                    val msg = if (studentTestResultsRes.message.isNullOrBlank()) {
                        "Lỗi lấy kết quả kiểm tra"
                    } else {
                        studentTestResultsRes.message
                    }
                    emit(Resource.Error(msg))
                    return@flow
                }
                is Resource.Loading -> {
                    emit(Resource.Loading())
                    emptyList()
                }
            }

            // Mini game results (all games for this student)
            val miniGameResultsRes = miniGameRepository
                .getAllResultsByStudent(studentId)
                .first { it !is Resource.Loading }
            val miniGameResults: List<StudentMiniGameResult> =
                (miniGameResultsRes as? Resource.Success)?.data ?: emptyList()
            Log.d(TAG, "Loaded mini game results: count=${miniGameResults.size}")

            // Study time statistics (for total time)
            val studyStatsRes = getStudyTimeStatistics(studentId)
                .first { it !is Resource.Loading }
            val studyStats = (studyStatsRes as? Resource.Success)?.data
            val totalStudyTimeSeconds: Long =
                studyStats?.totalSeconds ?: progressList.sumOf { it.timeSpentSeconds }
            Log.d(
                TAG,
                "Loaded study time stats: todaySeconds=${studyStats?.todaySeconds}, " +
                    "weekSeconds=${studyStats?.weekSeconds}, " +
                    "monthSeconds=${studyStats?.monthSeconds}, " +
                    "totalSeconds=${studyStats?.totalSeconds}, " +
                    "totalStudyTimeUsed=$totalStudyTimeSeconds"
            )

            // 2. Build overview
            val totalLessonsFromRepo: Int = lessonsByClassId.values.sumOf { it.size }
            val totalLessonsFromProgress: Int = progressList.map { it.lessonId }.toSet().size
            val totalLessons: Int = when {
                totalLessonsFromRepo > 0 -> totalLessonsFromRepo
                totalLessonsFromProgress > 0 -> totalLessonsFromProgress
                else -> 0
            }
            Log.d(
                TAG,
                "Computed totalLessons: fromRepo=$totalLessonsFromRepo, fromProgress=$totalLessonsFromProgress, final=$totalLessons"
            )
            val completedLessons: Int = progressList
                .filter { it.isCompleted }
                .map { it.lessonId }
                .toSet()
                .size
            val averageLessonProgressPercent: Int = if (progressList.isNotEmpty()) {
                progressList.map { it.progressPercentage }.average().toInt()
            } else {
                0
            }

            val totalTests: Int = tests.size
            val completedTestStatuses = setOf(
                com.example.datn.domain.models.TestStatus.SUBMITTED,
                com.example.datn.domain.models.TestStatus.COMPLETED,
                com.example.datn.domain.models.TestStatus.GRADED
            )
            val completedTests: Int = studentTestResults.count { it.completionStatus in completedTestStatuses }
            val averageTestScorePercent: Double? = if (studentTestResults.isNotEmpty()) {
                studentTestResults.map { it.score }.average()
            } else {
                null
            }

            val totalMiniGamesPlayed: Int = miniGameResults.size
            val averageMiniGameScorePercent: Double? = if (miniGameResults.isNotEmpty()) {
                miniGameResults
                    .map { result ->
                        if (result.maxScore > 0) {
                            (result.score * 100.0) / result.maxScore
                        } else {
                            0.0
                        }
                    }
                    .average()
            } else {
                null
            }

            val overview = StudentDashboardOverview(
                totalLessons = totalLessons,
                completedLessons = completedLessons,
                averageLessonProgressPercent = averageLessonProgressPercent,
                totalTests = totalTests,
                completedTests = completedTests,
                averageTestScorePercent = averageTestScorePercent,
                totalStudyTimeSeconds = totalStudyTimeSeconds,
                totalMiniGamesPlayed = totalMiniGamesPlayed,
                averageMiniGameScorePercent = averageMiniGameScorePercent
            )
            Log.d(
                TAG,
                "Built overview: totalLessons=$totalLessons, completedLessons=$completedLessons, " +
                    "averageLessonProgressPercent=$averageLessonProgressPercent, " +
                    "totalTests=$totalTests, completedTests=$completedTests, " +
                    "averageTestScorePercent=$averageTestScorePercent, " +
                    "totalMiniGamesPlayed=$totalMiniGamesPlayed, " +
                    "averageMiniGameScorePercent=$averageMiniGameScorePercent, " +
                    "totalStudyTimeSeconds=$totalStudyTimeSeconds"
            )

            // 3. Subject-wise statistics
            data class SubjectAgg(
                val lessonIds: MutableSet<String> = mutableSetOf(),
                val completedLessonIds: MutableSet<String> = mutableSetOf(),
                var sumLessonProgress: Int = 0,
                var lessonProgressCount: Int = 0,
                var totalStudyTimeSeconds: Long = 0L,
                var sumTestScore: Double = 0.0,
                var testCount: Int = 0
            )

            val subjectAggs = mutableMapOf<String, SubjectAgg>()

            fun getSubjectForClassId(classId: String): String {
                val clazz = classById[classId]
                val subject = clazz?.subject?.takeIf { it.isNotBlank() } ?: "Khác"
                return subject
            }

            // Initialize total lessons per subject
            classes.forEach { clazz ->
                val subject = clazz.subject?.takeIf { it.isNotBlank() } ?: "Khác"
                val agg = subjectAggs.getOrPut(subject) { SubjectAgg() }
                val lessons = lessonsByClassId[clazz.id].orEmpty()
                lessons.forEach { lesson ->
                    agg.lessonIds.add(lesson.id)
                }
            }

            // Apply lesson progress per subject
            progressList.forEach { progress ->
                val lesson = lessonById[progress.lessonId] ?: return@forEach
                val subject = getSubjectForClassId(lesson.classId)
                val agg = subjectAggs.getOrPut(subject) { SubjectAgg() }

                if (progress.isCompleted) {
                    agg.completedLessonIds.add(progress.lessonId)
                }
                agg.sumLessonProgress += progress.progressPercentage
                agg.lessonProgressCount++
                agg.totalStudyTimeSeconds += progress.timeSpentSeconds
            }

            // Apply test results per subject
            studentTestResults.forEach { result ->
                val test = testById[result.testId] ?: return@forEach
                val subject = getSubjectForClassId(test.classId)
                val agg = subjectAggs.getOrPut(subject) { SubjectAgg() }

                agg.sumTestScore += result.score
                agg.testCount++
            }

            val subjects: List<SubjectProgressStatistics> = subjectAggs.map { (subject, agg) ->
                val avgLessonProgress: Double = if (agg.lessonProgressCount > 0) {
                    agg.sumLessonProgress.toDouble() / agg.lessonProgressCount
                } else {
                    0.0
                }
                val avgTestScore: Double? = if (agg.testCount > 0) {
                    agg.sumTestScore / agg.testCount
                } else {
                    null
                }

                val trend = when {
                    avgTestScore != null && avgTestScore >= 80.0 && avgLessonProgress >= 70.0 -> SubjectTrend.GOOD
                    avgTestScore != null && avgTestScore < 50.0 -> SubjectTrend.NEEDS_IMPROVEMENT
                    else -> SubjectTrend.STABLE
                }

                SubjectProgressStatistics(
                    subject = subject,
                    totalLessons = agg.lessonIds.size,
                    completedLessons = agg.completedLessonIds.size,
                    averageLessonProgressPercent = avgLessonProgress,
                    averageTestScorePercent = avgTestScore,
                    totalStudyTimeSeconds = agg.totalStudyTimeSeconds,
                    trend = trend
                )
            }.sortedBy { it.subject }
            Log.d(TAG, "Built subject stats: count=${subjects.size}")

            // 4. Recent activities (lessons + tests + mini games)
            val recentActivities = buildRecentActivities(
                progressList = progressList,
                tests = tests,
                testResults = studentTestResults,
                miniGameResults = miniGameResults,
                lessonsById = lessonById,
                classesById = classById
            )
            Log.d(TAG, "Built recent activities: count=${recentActivities.size}")

            val dashboard = StudentDashboard(
                overview = overview,
                subjects = subjects,
                recentActivities = recentActivities
            )

            Log.d(TAG, "Emitting StudentDashboard for studentId=$studentId")
            emit(Resource.Success(dashboard))
        } catch (e: Exception) {
            Log.e(TAG, "Error loading student dashboard for studentId=$studentId", e)
            emit(Resource.Error(e.message ?: "Lỗi tải dashboard học sinh"))
        }
    }

    private suspend fun buildRecentActivities(
        progressList: List<StudentLessonProgress>,
        tests: List<com.example.datn.domain.models.Test>,
        testResults: List<StudentTestResult>,
        miniGameResults: List<StudentMiniGameResult>,
        lessonsById: Map<String, Lesson>,
        classesById: Map<String, Class>
    ): List<RecentActivityItem> {
        val testById = tests.associateBy { it.id }

        val lessonActivities = progressList
            .sortedByDescending { it.lastAccessedAt }
            .take(5)
            .mapNotNull { progress ->
                val lesson = lessonsById[progress.lessonId] ?: return@mapNotNull null
                val clazz = classesById[lesson.classId]
                val subject = clazz?.subject

                RecentActivityItem(
                    id = progress.lessonId,
                    type = RecentActivityType.LESSON,
                    title = lesson.title,
                    subject = subject,
                    timestamp = progress.lastAccessedAt,
                    scoreText = "${progress.progressPercentage}% hoàn thành"
                )
            }

        val testActivities = testResults
            .sortedByDescending { it.submissionTime }
            .take(5)
            .mapNotNull { result ->
                val test = testById[result.testId] ?: return@mapNotNull null
                val clazz = classesById[test.classId]
                val subject = clazz?.subject

                RecentActivityItem(
                    id = result.testId,
                    type = RecentActivityType.TEST,
                    title = test.title,
                    subject = subject,
                    timestamp = result.submissionTime,
                    scoreText = "${"%.1f".format(result.score)} điểm"
                )
            }

        // For mini games we currently don't resolve the game title/subject to avoid heavy queries.
        // We still show them as generic recent mini game activities.
        val miniGameActivities = miniGameResults
            .sortedByDescending { it.submissionTime }
            .take(5)
            .map { result ->
                val scorePercent = if (result.maxScore > 0) {
                    (result.score * 100.0) / result.maxScore
                } else {
                    0.0
                }

                RecentActivityItem(
                    id = result.miniGameId,
                    type = RecentActivityType.MINI_GAME,
                    title = "Mini game gần đây",
                    subject = null,
                    timestamp = result.submissionTime,
                    scoreText = "${"%.1f".format(scorePercent)}% điểm"
                )
            }

        return (lessonActivities + testActivities + miniGameActivities)
            .sortedByDescending { it.timestamp }
            .take(10)
    }
}

data class StudentDashboard(
    val overview: StudentDashboardOverview,
    val subjects: List<SubjectProgressStatistics>,
    val recentActivities: List<RecentActivityItem>
)

data class StudentDashboardOverview(
    val totalLessons: Int,
    val completedLessons: Int,
    val averageLessonProgressPercent: Int,
    val totalTests: Int,
    val completedTests: Int,
    val averageTestScorePercent: Double?,
    val totalStudyTimeSeconds: Long,
    val totalMiniGamesPlayed: Int,
    val averageMiniGameScorePercent: Double?
)

data class SubjectProgressStatistics(
    val subject: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val averageLessonProgressPercent: Double,
    val averageTestScorePercent: Double?,
    val totalStudyTimeSeconds: Long,
    val trend: SubjectTrend
)

enum class SubjectTrend {
    GOOD,
    STABLE,
    NEEDS_IMPROVEMENT
}

data class RecentActivityItem(
    val id: String,
    val type: RecentActivityType,
    val title: String,
    val subject: String?,
    val timestamp: Instant,
    val scoreText: String?
)

enum class RecentActivityType {
    LESSON,
    TEST,
    MINI_GAME
}

