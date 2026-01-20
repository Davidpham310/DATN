package com.example.datn.data.remote.service.test

import android.util.Log
import com.example.datn.data.remote.service.firestore.BaseFirestoreService
import com.example.datn.data.remote.service.minio.MinIOService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.models.StudentTestAnswer
import com.example.datn.domain.models.StudentTestResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

private const val TAG = "TestService"

class TestService @Inject constructor(
    private val minIOService: MinIOService
) :
    BaseFirestoreService<Test>(
        collectionName = "tests",
        clazz = Test::class.java
    ) {

    private val questionRef = FirebaseFirestore.getInstance().collection("test_questions")
    private val optionRef = FirebaseFirestore.getInstance().collection("test_options")
    private val resultRef = FirebaseFirestore.getInstance().collection("student_test_results")
    private val answerRef = FirebaseFirestore.getInstance().collection("student_test_answers")

    private fun toObjectKey(path: String): String {
        val candidates = listOf("tests/", "test_options/", "lessons/")
        for (p in candidates) {
            val idx = path.indexOf(p)
            if (idx >= 0) return path.substring(idx)
        }
        return path
    }

    // ==================== QUESTIONS ====================
    suspend fun getQuestionsByTest(testId: String): List<TestQuestion> = try {
        val snapshot = questionRef.whereEqualTo("testId", testId).get().await()
        snapshot.documents.mapNotNull { doc ->
            try { doc.internalToDomain(TestQuestion::class.java) } catch (e: Exception) {
                Log.e(TAG, "Failed to map question ${doc.id}", e); null
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getQuestionsByTest", e)
        emptyList()
    }

    suspend fun addTestQuestion(question: TestQuestion): TestQuestion? = try {
        // Load existing questions to determine desired order and shifts
        val existingQuestions = getQuestionsByTest(question.testId)

        val currentMaxQ = existingQuestions.maxOfOrNull { it.order } ?: 0
        val desiredOrder = when {
            // If no order provided (<= 0), append to end using 1-based indexing
            question.order <= 0 -> currentMaxQ + 1
            // Clamp to end (max+1) if larger than allowed insert position
            question.order > currentMaxQ + 1 -> currentMaxQ + 1
            else -> question.order
        }

        val questionsToShift = existingQuestions.filter { it.order >= desiredOrder }

        val docRef = if (question.id.isNotEmpty()) questionRef.document(question.id) else questionRef.document()

        val now = Instant.now()
        val data = question.copy(
            id = docRef.id,
            order = desiredOrder,
            createdAt = now,
            updatedAt = now
        )

        firestore.runBatch { batch ->
            questionsToShift.forEach { existingQuestion ->
                batch.update(questionRef.document(existingQuestion.id), "order", existingQuestion.order + 1)
            }
            batch.set(docRef, data)
        }.await()

        data
    } catch (e: Exception) {
        Log.e(TAG, "Error addTestQuestion", e)
        null
    }

    suspend fun updateTestQuestion(questionId: String, question: TestQuestion): Boolean {
        return try {
            val doc = questionRef.document(questionId).get().await()
            if (!doc.exists()) return false

            val oldQuestion = doc.internalToDomain(TestQuestion::class.java)
            val oldOrder = oldQuestion.order

            val otherQuestions = getQuestionsByTest(oldQuestion.testId)
                .filter { it.id != questionId }

            // For update within existing items, valid range is [1..maxOrder]
            val maxAllowedOrder = (otherQuestions.maxOfOrNull { it.order } ?: 0).coerceAtLeast(1)
            val clampedOrder = when {
                question.order < 1 -> oldOrder
                question.order > maxAllowedOrder -> maxAllowedOrder
                else -> question.order
            }

            if (clampedOrder == oldOrder) {
                val updated = question.copy(
                    id = questionId,
                    testId = oldQuestion.testId,
                    order = oldOrder,
                    questionType = oldQuestion.questionType,
                    createdAt = oldQuestion.createdAt,
                    updatedAt = Instant.now()
                )
                questionRef.document(questionId).set(updated).await()
                return true
            }

            firestore.runBatch { batch ->
                otherQuestions.find { it.order == clampedOrder }?.let { conflict ->
                    batch.update(questionRef.document(conflict.id), "order", oldOrder)
                }

                val updated = question.copy(
                    id = questionId,
                    testId = oldQuestion.testId,
                    order = clampedOrder,
                    questionType = oldQuestion.questionType,
                    createdAt = oldQuestion.createdAt,
                    updatedAt = Instant.now()
                )
                batch.set(questionRef.document(questionId), updated)
            }.await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updateTestQuestion", e)
            false
        }
    }

    suspend fun deleteTestQuestion(questionId: String): Boolean = try {
        // Try to load question for MinIO cleanup
        var questionMedia: String? = null
        try {
            val qDoc = questionRef.document(questionId).get().await()
            if (qDoc.exists()) {
                val q = qDoc.internalToDomain(TestQuestion::class.java)
                questionMedia = q.mediaUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch question before delete: $questionId", e)
        }

        // Delete all options first (and their MinIO files)
        val options = getOptionsByQuestion(questionId)
        options.forEach { option ->
            val media = option.mediaUrl
            if (!media.isNullOrBlank()) {
                try { minIOService.deleteFile(toObjectKey(media)) } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete option media from MinIO: $media", e)
                }
            }
            optionRef.document(option.id).delete().await()
        }

        // Delete the question document
        questionRef.document(questionId).delete().await()

        // Delete question media from MinIO if provided
        val qm = questionMedia
        if (!qm.isNullOrBlank()) {
            try { minIOService.deleteFile(toObjectKey(qm)) } catch (e: Exception) {
                Log.w(TAG, "Failed to delete question media from MinIO: $qm", e)
            }
        }

        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleteTestQuestion", e)
        false
    }

    suspend fun getTestQuestionById(questionId: String): TestQuestion? = try {
        val doc = questionRef.document(questionId).get().await()
        if (doc.exists()) doc.internalToDomain(TestQuestion::class.java) else null
    } catch (e: Exception) {
        Log.e(TAG, "Error getTestQuestionById", e)
        null
    }

    // ==================== OPTIONS ====================
    suspend fun getOptionsByQuestion(questionId: String): List<TestOption> = try {
        val snapshot = optionRef.whereEqualTo("testQuestionId", questionId).get().await()
        snapshot.documents.mapNotNull { doc ->
            try { doc.internalToDomain(TestOption::class.java) } catch (e: Exception) {
                Log.e(TAG, "Failed to map option ${doc.id}", e); null
            }
        }.sortedBy { it.order }
    } catch (e: Exception) {
        Log.e(TAG, "Error getOptionsByQuestion", e)
        emptyList()
    }

    suspend fun addOption(option: TestOption): TestOption? = try {
        // Load existing options to determine desired order and shifts
        val existing = getOptionsByQuestion(option.testQuestionId)

        val currentMaxO = existing.maxOfOrNull { it.order } ?: 0
        val desiredOrder = when {
            // If no order provided (<= 0), append to end using 1-based indexing
            option.order <= 0 -> currentMaxO + 1
            // Clamp to end (max+1) if larger than allowed insert position
            option.order > currentMaxO + 1 -> currentMaxO + 1
            else -> option.order
        }

        val toShift = existing.filter { it.order >= desiredOrder }

        val docRef = if (option.id.isNotEmpty()) optionRef.document(option.id) else optionRef.document()
        val now = Instant.now()
        val data = option.copy(
            id = docRef.id,
            order = desiredOrder,
            createdAt = now,
            updatedAt = now
        )

        firestore.runBatch { batch ->
            toShift.forEach { ex ->
                batch.update(optionRef.document(ex.id), "order", ex.order + 1)
            }
            batch.set(docRef, data)
        }.await()

        data
    } catch (e: Exception) {
        Log.e(TAG, "Error addOption", e)
        null
    }

    suspend fun updateOption(optionId: String, option: TestOption): Boolean {
        return try {
            val doc = optionRef.document(optionId).get().await()
            if (!doc.exists()) return false

            val old = doc.internalToDomain(TestOption::class.java)
            val oldOrder = old.order

            val others = getOptionsByQuestion(old.testQuestionId).filter { it.id != optionId }
            // For update within existing items, valid range is [1..maxOrder]
            val maxAllowed = (others.maxOfOrNull { it.order } ?: 0).coerceAtLeast(1)
            val clampedOrder = when {
                option.order < 1 -> oldOrder
                option.order > maxAllowed -> maxAllowed
                else -> option.order
            }

            if (clampedOrder == oldOrder) {
                val updated = option.copy(
                    id = optionId,
                    testQuestionId = old.testQuestionId,
                    order = oldOrder,
                    createdAt = old.createdAt,
                    updatedAt = Instant.now()
                )
                optionRef.document(optionId).set(updated).await()
                return true
            }

            firestore.runBatch { batch ->
                others.find { it.order == clampedOrder }?.let { conflict ->
                    batch.update(optionRef.document(conflict.id), "order", oldOrder)
                }

                val updated = option.copy(
                    id = optionId,
                    testQuestionId = old.testQuestionId,
                    order = clampedOrder,
                    createdAt = old.createdAt,
                    updatedAt = Instant.now()
                )
                batch.set(optionRef.document(optionId), updated)
            }.await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updateOption", e)
            false
        }
    }

    suspend fun deleteOption(optionId: String): Boolean = try {
        // Try to load option to remove its MinIO media if any
        try {
            val doc = optionRef.document(optionId).get().await()
            if (doc.exists()) {
                val opt = doc.internalToDomain(TestOption::class.java)
                val media = opt.mediaUrl
                if (!media.isNullOrBlank()) {
                    try { minIOService.deleteFile(media) } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete option media from MinIO: $media", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch option before delete: $optionId", e)
        }

        optionRef.document(optionId).delete().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleteOption", e)
        false
    }

    // ==================== TEST CRUD ====================
    suspend fun getTestById(testId: String): Test? = try {
        val doc = collectionRef.document(testId).get().await()
        if (doc.exists()) doc.internalToDomain(clazz) else null
    } catch (e: Exception) {
        Log.e(TAG, "Error getTestById", e); null
    }

    suspend fun getTestsByLesson(lessonId: String): List<Test> = try {
        val snapshot = firestore.collection("tests")
            .whereEqualTo("lessonId", lessonId)
            .get()
            .await()
        snapshot.documents.mapNotNull { d ->
            try { d.internalToDomain(clazz) } catch (e: Exception) { Log.e(TAG, "map test ${d.id}", e); null }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getTestsByLesson", e); emptyList()
    }

    suspend fun getTestsByClassId(classId: String): List<Test> = try {
        Log.d(TAG, "getTestsByClassId - classId: $classId")
        val snapshot = firestore.collection("tests")
            .whereEqualTo("classId", classId)
            .get()
            .await()
        val tests = snapshot.documents.mapNotNull { d ->
            try { d.internalToDomain(clazz) } catch (e: Exception) { Log.e(TAG, "map test ${d.id}", e); null }
        }
        Log.d(TAG, "getTestsByClassId - found ${tests.size} tests")
        tests
    } catch (e: Exception) {
        Log.e(TAG, "Error getTestsByClassId", e)
        emptyList()
    }

    suspend fun addTest(test: Test): Test? = try {
        val docRef = if (test.id.isNotEmpty()) collectionRef.document(test.id) else collectionRef.document()
        val now = Instant.now()
        val data = test.copy(id = docRef.id, createdAt = now, updatedAt = now)
        docRef.set(data).await()
        data
    } catch (e: Exception) {
        Log.e(TAG, "Error addTest", e); null
    }

    suspend fun updateTest(testId: String, test: Test): Boolean = try {
        val updated = test.copy(id = testId, updatedAt = Instant.now())
        collectionRef.document(testId).set(updated).await(); true
    } catch (e: Exception) {
        Log.e(TAG, "Error updateTest", e); false
    }

    suspend fun deleteTest(testId: String): Boolean = try {
        val qs = getQuestionsByTest(testId)
        qs.forEach { q ->
            // Delete options and their MinIO media
            val opts = getOptionsByQuestion(q.id)
            opts.forEach { opt ->
                val media = opt.mediaUrl
                if (!media.isNullOrBlank()) {
                    try { minIOService.deleteFile(toObjectKey(media)) } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete option media from MinIO: $media", e)
                    }
                }
                optionRef.document(opt.id).delete().await()
            }

            // Delete question doc
            questionRef.document(q.id).delete().await()

            // Delete question media
            val qMedia = q.mediaUrl
            if (!qMedia.isNullOrBlank()) {
                try { minIOService.deleteFile(toObjectKey(qMedia)) } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete question media from MinIO: $qMedia", e)
                }
            }
        }
        collectionRef.document(testId).delete().await(); true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleteTest", e); false
    }

    // ==================== RESULTS ====================
    suspend fun submitResult(result: StudentTestResult): StudentTestResult? = try {
        val docRef = if (result.id.isNotEmpty()) resultRef.document(result.id) else resultRef.document()
        val now = Instant.now()
        val data = result.copy(id = docRef.id, createdAt = now, updatedAt = now)
        docRef.set(data).await(); data
    } catch (e: Exception) {
        Log.e(TAG, "Error submitResult", e); null
    }

    suspend fun updateResult(resultId: String, result: StudentTestResult): StudentTestResult? = try {
        val now = Instant.now()
        val data = result.copy(id = resultId, updatedAt = now)
        resultRef.document(resultId).set(data).await()
        data
    } catch (e: Exception) {
        Log.e(TAG, "Error updateResult", e)
        null
    }

    suspend fun getResultByStudentAndTest(studentId: String, testId: String): com.example.datn.domain.models.StudentTestResult? {
        return try {
            Log.d(TAG, "getResultByStudentAndTest - studentId: $studentId, testId: $testId")
            val snapshot = resultRef
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("testId", testId)
                .get().await()
            Log.d(TAG, "getResultByStudentAndTest - found ${snapshot.documents.size} results")
            val doc = snapshot.documents.firstOrNull()
            if (doc == null) {
                Log.w(TAG, "getResultByStudentAndTest - No result found, trying alternative query...")
                // Fallback: get all results for this test and filter
                val allResults = resultRef.whereEqualTo("testId", testId).get().await()
                Log.d(TAG, "getResultByStudentAndTest - alternative found ${allResults.documents.size} total results for test")
                val matchedDoc = allResults.documents.firstOrNull { it.getString("studentId") == studentId }
                if (matchedDoc != null) {
                    Log.d(TAG, "getResultByStudentAndTest - Found match via fallback!")
                    matchedDoc.internalToDomain(com.example.datn.domain.models.StudentTestResult::class.java)
                } else {
                    null
                }
            } else {
                doc.internalToDomain(com.example.datn.domain.models.StudentTestResult::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getResultByStudentAndTest", e)
            null
        }
    }

    suspend fun getResultsByTest(testId: String): List<com.example.datn.domain.models.StudentTestResult> = try {
        val snapshot = resultRef.whereEqualTo("testId", testId).get().await()
        snapshot.documents.mapNotNull { d ->
            try { d.internalToDomain(com.example.datn.domain.models.StudentTestResult::class.java) } catch (_: Exception) { null }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getResultsByTest", e); emptyList()
    }

    suspend fun getResultsByStudent(studentId: String): List<com.example.datn.domain.models.StudentTestResult> = try {
        Log.d(TAG, "getResultsByStudent - studentId: $studentId")
        val snapshot = resultRef.whereEqualTo("studentId", studentId).get().await()
        val results = snapshot.documents.mapNotNull { d ->
            try { d.internalToDomain(com.example.datn.domain.models.StudentTestResult::class.java) } catch (_: Exception) { null }
        }
        Log.d(TAG, "getResultsByStudent - found ${results.size} results")
        results
    } catch (e: Exception) {
        Log.e(TAG, "Error getResultsByStudent", e)
        emptyList()
    }
    
    // ==================== STUDENT ANSWERS ====================
    suspend fun saveStudentAnswers(answers: List<StudentTestAnswer>): Boolean = try {
        answers.forEach { answer ->
            val docRef = if (answer.id.isNotEmpty()) answerRef.document(answer.id) else answerRef.document()
            val data = answer.copy(id = docRef.id)
            docRef.set(data).await()
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error saveStudentAnswers", e)
        false
    }

    suspend fun updateStudentAnswer(answerId: String, answer: StudentTestAnswer): StudentTestAnswer? = try {
        val now = Instant.now()
        val data = answer.copy(id = answerId, updatedAt = now)
        answerRef.document(answerId).set(data).await()
        data
    } catch (e: Exception) {
        Log.e(TAG, "Error updateStudentAnswer", e)
        null
    }
    
    suspend fun getAnswersByResultId(resultId: String): List<StudentTestAnswer> = try {
        val snapshot = answerRef.whereEqualTo("resultId", resultId).get().await()
        snapshot.documents.mapNotNull { doc ->
            try {
                doc.internalToDomain(StudentTestAnswer::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to map answer ${doc.id}", e)
                null
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getAnswersByResultId", e)
        emptyList()
    }
}


