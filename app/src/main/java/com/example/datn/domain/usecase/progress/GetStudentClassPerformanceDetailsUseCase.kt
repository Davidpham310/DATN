package com.example.datn.domain.usecase.progress

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.CompletionStatus
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.TestStatus
import com.example.datn.domain.repository.ILessonRepository
import com.example.datn.domain.repository.IMiniGameRepository
import com.example.datn.domain.repository.ITestRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class GetStudentClassPerformanceDetailsUseCase @Inject constructor(
    private val lessonRepository: ILessonRepository,
    private val testRepository: ITestRepository,
    private val miniGameRepository: IMiniGameRepository
) {

    operator fun invoke(
        studentId: String,
        classId: String
    ): Flow<Resource<StudentClassPerformanceDetails>> = flow {
        emit(Resource.Loading())

        try {
            val lessonsRes = lessonRepository
                .getLessonsByClass(classId)
                .first { it !is Resource.Loading }

            val lessons = when (lessonsRes) {
                is Resource.Success -> lessonsRes.data.orEmpty()
                is Resource.Error -> {
                    emit(Resource.Error(lessonsRes.message ?: "Lỗi lấy danh sách bài học"))
                    return@flow
                }
                is Resource.Loading -> emptyList()
            }

            val testsRes = testRepository
                .getTestsByClasses(listOf(classId))
                .first { it !is Resource.Loading }

            val tests = when (testsRes) {
                is Resource.Success -> testsRes.data.orEmpty()
                is Resource.Error -> {
                    emit(Resource.Error(testsRes.message ?: "Lỗi lấy danh sách bài kiểm tra"))
                    return@flow
                }
                is Resource.Loading -> emptyList()
            }
            val testById = tests.associateBy { it.id }

            val studentTestResultsRes = testRepository
                .getStudentTestResults(studentId)
                .first { it !is Resource.Loading }

            val studentTestResults: List<StudentTestResult> = when (studentTestResultsRes) {
                is Resource.Success -> studentTestResultsRes.data.orEmpty()
                is Resource.Error -> {
                    emit(Resource.Error(studentTestResultsRes.message ?: "Lỗi lấy kết quả kiểm tra"))
                    return@flow
                }
                is Resource.Loading -> emptyList()
            }

            val testItems = studentTestResults
                .mapNotNull { result ->
                    val test = testById[result.testId] ?: return@mapNotNull null
                    StudentClassTestResultItem(
                        testId = result.testId,
                        testTitle = test.title,
                        score = result.score,
                        maxScore = test.totalScore,
                        completionStatus = result.completionStatus,
                        submissionTime = result.submissionTime,
                        durationSeconds = result.durationSeconds
                    )
                }
                .sortedByDescending { it.submissionTime }

            val miniGamesByLessonId = mutableMapOf<String, List<com.example.datn.domain.models.MiniGame>>()
            lessons.forEach { lesson ->
                val miniGamesRes = miniGameRepository
                    .getGamesByLesson(lesson.id)
                    .first { it !is Resource.Loading }

                val games = when (miniGamesRes) {
                    is Resource.Success -> miniGamesRes.data.orEmpty()
                    is Resource.Error -> emptyList()
                    is Resource.Loading -> emptyList()
                }

                miniGamesByLessonId[lesson.id] = games
            }

            val miniGameById = miniGamesByLessonId.values
                .flatten()
                .associateBy { it.id }

            val lessonIdsInClass: Set<String> = lessons.map { it.id }.toSet()
            val lessonTitleById: Map<String, String> = lessons.associate { it.id to it.title }

            val allMiniGameResultsRes = miniGameRepository
                .getAllResultsByStudent(studentId)
                .first { it !is Resource.Loading }

            val allMiniGameResults: List<StudentMiniGameResult> = when (allMiniGameResultsRes) {
                is Resource.Success -> allMiniGameResultsRes.data.orEmpty()
                is Resource.Error -> emptyList()
                is Resource.Loading -> emptyList()
            }

            val resolvedGameById = mutableMapOf<String, com.example.datn.domain.models.MiniGame?>()

            val miniGameItems = allMiniGameResults
                .mapNotNull { result ->
                    if (result.miniGameId.startsWith("lesson_")) {
                        val lessonId = result.miniGameId.removePrefix("lesson_")
                        if (lessonId !in lessonIdsInClass) return@mapNotNull null

                        val title = lessonTitleById[lessonId] ?: "Mini game bài học"
                        return@mapNotNull StudentClassMiniGameResultItem(
                            miniGameId = result.miniGameId,
                            miniGameTitle = title,
                            score = result.score,
                            maxScore = result.maxScore,
                            completionStatus = result.completionStatus,
                            submissionTime = result.submissionTime,
                            durationSeconds = result.durationSeconds,
                            attemptNumber = result.attemptNumber
                        )
                    }

                    val cachedResolved = resolvedGameById[result.miniGameId]
                    val game = if (cachedResolved != null) {
                        cachedResolved
                    } else {
                        val local = miniGameById[result.miniGameId]
                        if (local != null) {
                            resolvedGameById[result.miniGameId] = local
                            local
                        } else {
                            val gameRes = miniGameRepository
                                .getGameById(result.miniGameId)
                                .first { it !is Resource.Loading }
                            val fetched = (gameRes as? Resource.Success)?.data
                            resolvedGameById[result.miniGameId] = fetched
                            fetched
                        }
                    } ?: return@mapNotNull null

                    if (game.lessonId !in lessonIdsInClass) return@mapNotNull null

                    StudentClassMiniGameResultItem(
                        miniGameId = result.miniGameId,
                        miniGameTitle = game.title,
                        score = result.score,
                        maxScore = result.maxScore,
                        completionStatus = result.completionStatus,
                        submissionTime = result.submissionTime,
                        durationSeconds = result.durationSeconds,
                        attemptNumber = result.attemptNumber
                    )
                }
                .sortedByDescending { it.submissionTime }

            val finalMiniGameItems: List<StudentClassMiniGameResultItem> =
                if (miniGameItems.isNotEmpty() || allMiniGameResults.isEmpty()) {
                    miniGameItems
                } else {
                    // Fallback: dữ liệu Firestore có nhưng không map được theo class (thường do miniGameId dạng lesson_<lessonId>
                    // hoặc lessonId không nằm trong danh sách lessons của class).
                    allMiniGameResults
                        .map { result ->
                            if (result.miniGameId.startsWith("lesson_")) {
                                val lessonId = result.miniGameId.removePrefix("lesson_")
                                val title = lessonTitleById[lessonId] ?: "Mini game bài học"
                                StudentClassMiniGameResultItem(
                                    miniGameId = result.miniGameId,
                                    miniGameTitle = title,
                                    score = result.score,
                                    maxScore = result.maxScore,
                                    completionStatus = result.completionStatus,
                                    submissionTime = result.submissionTime,
                                    durationSeconds = result.durationSeconds,
                                    attemptNumber = result.attemptNumber
                                )
                            } else {
                                val cachedResolved = resolvedGameById[result.miniGameId]
                                val game = if (cachedResolved != null) {
                                    cachedResolved
                                } else {
                                    val local = miniGameById[result.miniGameId]
                                    if (local != null) {
                                        resolvedGameById[result.miniGameId] = local
                                        local
                                    } else {
                                        val gameRes = miniGameRepository
                                            .getGameById(result.miniGameId)
                                            .first { it !is Resource.Loading }
                                        val fetched = (gameRes as? Resource.Success)?.data
                                        resolvedGameById[result.miniGameId] = fetched
                                        fetched
                                    }
                                }

                                StudentClassMiniGameResultItem(
                                    miniGameId = result.miniGameId,
                                    miniGameTitle = game?.title ?: "Mini game",
                                    score = result.score,
                                    maxScore = result.maxScore,
                                    completionStatus = result.completionStatus,
                                    submissionTime = result.submissionTime,
                                    durationSeconds = result.durationSeconds,
                                    attemptNumber = result.attemptNumber
                                )
                            }
                        }
                        .sortedByDescending { it.submissionTime }
                }

            emit(
                Resource.Success(
                    StudentClassPerformanceDetails(
                        testResults = testItems,
                        miniGameResults = finalMiniGameItems
                    )
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            emit(Resource.Error(e.message ?: "Lỗi tải chi tiết tiến độ học tập"))
        }
    }
}

data class StudentClassPerformanceDetails(
    val testResults: List<StudentClassTestResultItem>,
    val miniGameResults: List<StudentClassMiniGameResultItem>
)

data class StudentClassTestResultItem(
    val testId: String,
    val testTitle: String,
    val score: Double,
    val maxScore: Double,
    val completionStatus: TestStatus,
    val submissionTime: java.time.Instant,
    val durationSeconds: Long
)

data class StudentClassMiniGameResultItem(
    val miniGameId: String,
    val miniGameTitle: String,
    val score: Double,
    val maxScore: Double,
    val completionStatus: CompletionStatus,
    val submissionTime: java.time.Instant,
    val durationSeconds: Long,
    val attemptNumber: Int
)
