package com.example.datn.core.network.service.test

import android.util.Log
import com.example.datn.core.network.service.firestore.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.models.TestQuestion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

private const val TAG = "TestService"

class TestService @Inject constructor() :
    BaseFirestoreService<Test>(
        collectionName = "tests",
        clazz = Test::class.java
    ) {

    private val questionRef = FirebaseFirestore.getInstance().collection("test_questions")
    private val optionRef = FirebaseFirestore.getInstance().collection("test_options")
    private val resultRef = FirebaseFirestore.getInstance().collection("student_test_results")
    private val answerRef = FirebaseFirestore.getInstance().collection("student_test_answers")

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
        val docRef = if (question.id.isNotEmpty()) questionRef.document(question.id) else questionRef.document()
        val now = Instant.now()
        val data = question.copy(id = docRef.id, createdAt = now, updatedAt = now)
        docRef.set(data).await()
        data
    } catch (e: Exception) {
        Log.e(TAG, "Error addTestQuestion", e)
        null
    }

    suspend fun updateTestQuestion(questionId: String, question: TestQuestion): Boolean = try {
        val updated = question.copy(id = questionId, updatedAt = Instant.now())
        questionRef.document(questionId).set(updated).await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error updateTestQuestion", e)
        false
    }

    suspend fun deleteTestQuestion(questionId: String): Boolean = try {
        // Delete all options first
        val options = getOptionsByQuestion(questionId)
        options.forEach { option ->
            optionRef.document(option.id).delete().await()
        }
        // Then delete the question
        questionRef.document(questionId).delete().await()
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
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getOptionsByQuestion", e)
        emptyList()
    }

    suspend fun addOption(option: TestOption): TestOption? = try {
        val docRef = if (option.id.isNotEmpty()) optionRef.document(option.id) else optionRef.document()
        val now = Instant.now()
        val data = option.copy(id = docRef.id, createdAt = now, updatedAt = now)
        docRef.set(data).await()
        data
    } catch (e: Exception) {
        Log.e(TAG, "Error addOption", e)
        null
    }

    suspend fun updateOption(optionId: String, option: TestOption): Boolean = try {
        val updated = option.copy(id = optionId, updatedAt = Instant.now())
        optionRef.document(optionId).set(updated).await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error updateOption", e)
        false
    }

    suspend fun deleteOption(optionId: String): Boolean = try {
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
            optionRef.whereEqualTo("testQuestionId", q.id).get().await().documents.forEach { d ->
                optionRef.document(d.id).delete().await()
            }
            questionRef.document(q.id).delete().await()
        }
        collectionRef.document(testId).delete().await(); true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleteTest", e); false
    }

    // ==================== RESULTS ====================
    suspend fun submitResult(result: com.example.datn.domain.models.StudentTestResult): com.example.datn.domain.models.StudentTestResult? = try {
        val docRef = if (result.id.isNotEmpty()) resultRef.document(result.id) else resultRef.document()
        val now = Instant.now()
        val data = result.copy(id = docRef.id, createdAt = now, updatedAt = now)
        docRef.set(data).await(); data
    } catch (e: Exception) {
        Log.e(TAG, "Error submitResult", e); null
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
    suspend fun saveStudentAnswers(answers: List<com.example.datn.domain.models.StudentTestAnswer>): Boolean = try {
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
    
    suspend fun getAnswersByResultId(resultId: String): List<com.example.datn.domain.models.StudentTestAnswer> = try {
        val snapshot = answerRef.whereEqualTo("resultId", resultId).get().await()
        snapshot.documents.mapNotNull { doc ->
            try {
                doc.internalToDomain(com.example.datn.domain.models.StudentTestAnswer::class.java)
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


