package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.repository.ITestOptionRepository
import com.example.datn.domain.repository.ITestQuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject

data class TestExcelImportSummary(
    val totalRows: Int,
    val importedQuestions: Int,
    val importedOptions: Int,
    val skippedRows: Int
)

class ImportTestQuestionsFromExcelUseCase @Inject constructor(
    private val testQuestionRepository: ITestQuestionRepository,
    private val testOptionRepository: ITestOptionRepository
) {
    operator fun invoke(
        testId: String,
        inputStream: InputStream,
        startingOrder: Int
    ): Flow<Resource<TestExcelImportSummary>> = flow {
        emit(Resource.Loading())

        var totalRows = 0
        var importedQuestions = 0
        var importedOptions = 0
        var skippedRows = 0

        var nextOrder = startingOrder

        inputStream.use { isr ->
            ReadableWorkbook(isr).use { workbook ->
                val sheet = workbook.firstSheet
                val rows = sheet.openStream()
                try {
                    val iterator = rows.iterator()
                    var rowIndex = 0

                    while (iterator.hasNext()) {
                        val row = iterator.next()
                        rowIndex++

                        // Skip completely empty rows
                        val rawContent = getCellTextSafe(row, 0).trim()
                        val rawScore = getCellTextSafe(row, 1).trim()
                        val rawType = getCellTextSafe(row, 2).trim()

                        val looksLikeHeader = rowIndex == 1 && (
                            rawContent.equals("content", ignoreCase = true) ||
                                rawContent.contains("câu", ignoreCase = true) ||
                                rawType.equals("type", ignoreCase = true) ||
                                rawType.contains("loại", ignoreCase = true)
                            )
                        if (looksLikeHeader) continue

                        if (rawContent.isBlank() && rawScore.isBlank() && rawType.isBlank()) continue

                        totalRows++

                        val parseResult = runCatching {
                            parseRowToQuestionAndOptions(
                                testId = testId,
                                content = rawContent,
                                scoreCell = runCatching { row.getCellAsNumber(1).orElse(null) }.getOrNull(),
                                scoreRaw = rawScore,
                                typeRaw = rawType,
                                mediaUrl = getCellTextSafe(row, 3).trim().ifBlank { null },
                                optionCells = (4..7).map { idx -> getCellTextSafe(row, idx).trim().ifBlank { null } },
                                correctRaw = getCellTextSafe(row, 8).trim().ifBlank { null },
                                order = nextOrder
                            )
                        }.getOrNull()

                        if (parseResult == null) {
                            skippedRows++
                            continue
                        }

                        val (question, options) = parseResult

                        // Create question
                        val questionCreated = awaitFinal(testQuestionRepository.createQuestion(question))
                        val savedQuestion = (questionCreated as? Resource.Success)?.data
                        if (savedQuestion == null) {
                            skippedRows++
                            continue
                        }

                        // Create options
                        val createdOptionIds = mutableListOf<String>()
                        var optionFailed = false

                        for (opt in options) {
                            val optForQuestion = opt.copy(testQuestionId = savedQuestion.id)
                            val created = awaitFinal(testOptionRepository.createOption(optForQuestion))
                            val savedOpt = (created as? Resource.Success)?.data
                            if (savedOpt == null) {
                                optionFailed = true
                                break
                            } else {
                                createdOptionIds.add(savedOpt.id)
                            }
                        }

                        if (optionFailed) {
                            // Best-effort rollback: delete created options + question
                            createdOptionIds.forEach { id ->
                                awaitFinal(testOptionRepository.deleteOption(id))
                            }
                            awaitFinal(testQuestionRepository.deleteQuestion(savedQuestion.id))

                            skippedRows++
                            continue
                        }

                        importedQuestions++
                        importedOptions += createdOptionIds.size
                        nextOrder++
                    }
                } finally {
                    rows.close()
                }
            }
        }

        emit(
            Resource.Success(
                TestExcelImportSummary(
                    totalRows = totalRows,
                    importedQuestions = importedQuestions,
                    importedOptions = importedOptions,
                    skippedRows = skippedRows
                )
            )
        )
    }

    private suspend fun <T> awaitFinal(flow: Flow<Resource<T>>): Resource<T> {
        var last: Resource<T> = Resource.Loading()
        flow.collect { value ->
            last = value
        }
        return last
    }

    private fun getCellTextSafe(row: Row, index: Int): String {
        val asString = runCatching { row.getCellAsString(index).orElse(null) }.getOrNull()
        if (!asString.isNullOrBlank()) return asString

        val asNumber = runCatching { row.getCellAsNumber(index).orElse(null) }.getOrNull()
        if (asNumber != null) return asNumber.toPlainString()

        return ""
    }

    private fun parseRowToQuestionAndOptions(
        testId: String,
        content: String,
        scoreCell: java.math.BigDecimal?,
        scoreRaw: String,
        typeRaw: String,
        mediaUrl: String?,
        optionCells: List<String?>,
        correctRaw: String?,
        order: Int
    ): Pair<TestQuestion, List<TestOption>>? {
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank()) return null

        val score = scoreCell?.toDouble() ?: scoreRaw.trim().replace(",", ".").toDoubleOrNull() ?: return null
        if (score <= 0.0) return null

        val type = QuestionType.fromString(typeRaw) ?: QuestionType.fromDisplayName(typeRaw) ?: return null

        val now = Instant.now()
        val question = TestQuestion(
            id = "",
            testId = testId,
            content = normalizedContent,
            score = score,
            questionType = type,
            mediaUrl = mediaUrl,
            timeLimit = 0,
            order = order,
            createdAt = now,
            updatedAt = now
        )

        val options = when (type) {
            QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE -> {
                val opts = optionCells.filterNotNull().map { it.trim() }.filter { it.isNotBlank() }
                if (opts.isEmpty()) return null

                val correctIndexes = parseCorrectIndexes(correctRaw)
                if (type == QuestionType.SINGLE_CHOICE && correctIndexes.size != 1) return null
                if (type == QuestionType.MULTIPLE_CHOICE && correctIndexes.isEmpty()) return null

                val maxIndex = opts.size
                if (correctIndexes.any { it !in 1..maxIndex }) return null

                opts.mapIndexed { idx, text ->
                    val index1 = idx + 1
                    TestOption(
                        id = "",
                        testQuestionId = "",
                        content = text,
                        isCorrect = correctIndexes.contains(index1),
                        order = idx,
                        mediaUrl = null,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }

            QuestionType.FILL_BLANK -> {
                val correctText = correctRaw?.trim().orEmpty()
                if (correctText.isBlank()) return null

                listOf(
                    TestOption(
                        id = "",
                        testQuestionId = "",
                        content = correctText,
                        isCorrect = true,
                        order = 0,
                        mediaUrl = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            QuestionType.ESSAY -> {
                emptyList()
            }
        }

        return question to options
    }

    private fun parseCorrectIndexes(raw: String?): Set<Int> {
        val s = raw?.trim().orEmpty()
        if (s.isBlank()) return emptySet()

        return s.split(",", ";", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { token ->
                token.toIntOrNull()
                    ?: token.replace(",", ".").toDoubleOrNull()?.let { d ->
                        val asInt = d.toInt()
                        if (d == asInt.toDouble()) asInt else null
                    }
            }
            .toSet()
    }
}
