package com.example.datn.domain.usecase.progress

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.CompletionStatus
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.TestStatus
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.repository.IMiniGameRepository
import com.example.datn.domain.repository.ITestRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class GetStudentPerformanceDetailsUseCase @Inject constructor(
    private val classRepository: IClassRepository,
    private val testRepository: ITestRepository,
    private val miniGameRepository: IMiniGameRepository
) {

    operator fun invoke(studentId: String): Flow<Resource<StudentPerformanceDetails>> = flow {
        emit(Resource.Loading())

        try {
            val classesRes = classRepository
                .getClassesByStudent(studentId)
                .first { it !is Resource.Loading }

            val classIds: List<String> = when (classesRes) {
                is Resource.Success -> classesRes.data.orEmpty().map { it.id }
                is Resource.Error -> {
                    emit(Resource.Error(classesRes.message ?: "Lỗi lấy danh sách lớp học"))
                    return@flow
                }
                is Resource.Loading -> emptyList()
            }

            val testsRes = if (classIds.isNotEmpty()) {
                testRepository
                    .getTestsByClasses(classIds)
                    .first { it !is Resource.Loading }
            } else {
                Resource.Success(emptyList())
            }

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
                    StudentPerformanceTestResultItem(
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

            val allMiniGameResultsRes = miniGameRepository
                .getAllResultsByStudent(studentId)
                .first { it !is Resource.Loading }

            val allMiniGameResults: List<StudentMiniGameResult> = when (allMiniGameResultsRes) {
                is Resource.Success -> allMiniGameResultsRes.data.orEmpty()
                is Resource.Error -> emptyList()
                is Resource.Loading -> emptyList()
            }

            val gameTitleById: Map<String, String> = allMiniGameResults
                .map { it.miniGameId }
                .distinct()
                .associateWith { miniGameId ->
                    val gameRes = miniGameRepository
                        .getGameById(miniGameId)
                        .first { it !is Resource.Loading }

                    when (gameRes) {
                        is Resource.Success -> gameRes.data?.title ?: "Mini game"
                        else -> "Mini game"
                    }
                }

            val miniGameItems = allMiniGameResults
                .map { result ->
                    val title = gameTitleById[result.miniGameId] ?: "Mini game"
                    StudentPerformanceMiniGameResultItem(
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
                .sortedByDescending { it.submissionTime }

            emit(
                Resource.Success(
                    StudentPerformanceDetails(
                        testResults = testItems,
                        miniGameResults = miniGameItems
                    )
                )
            )
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi tải chi tiết tiến độ học tập"))
        }
    }
}

data class StudentPerformanceDetails(
    val testResults: List<StudentPerformanceTestResultItem>,
    val miniGameResults: List<StudentPerformanceMiniGameResultItem>
)

data class StudentPerformanceTestResultItem(
    val testId: String,
    val testTitle: String,
    val score: Double,
    val maxScore: Double,
    val completionStatus: TestStatus,
    val submissionTime: Instant,
    val durationSeconds: Long
)

data class StudentPerformanceMiniGameResultItem(
    val miniGameId: String,
    val miniGameTitle: String,
    val score: Double,
    val maxScore: Double,
    val completionStatus: CompletionStatus,
    val submissionTime: Instant,
    val durationSeconds: Long,
    val attemptNumber: Int
)
