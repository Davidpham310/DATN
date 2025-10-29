package com.example.datn.core.network.service.test

import android.util.Log
import com.example.datn.core.network.service.BaseFirestoreService
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

    suspend fun getResultByStudentAndTest(studentId: String, testId: String): com.example.datn.domain.models.StudentTestResult? = try {
        val snapshot = resultRef
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("testId", testId)
            .get().await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        doc.internalToDomain(com.example.datn.domain.models.StudentTestResult::class.java)
    } catch (e: Exception) {
        Log.e(TAG, "Error getResultByStudentAndTest", e); null
    }

    suspend fun getResultsByTest(testId: String): List<com.example.datn.domain.models.StudentTestResult> = try {
        val snapshot = resultRef.whereEqualTo("testId", testId).get().await()
        snapshot.documents.mapNotNull { d ->
            try { d.internalToDomain(com.example.datn.domain.models.StudentTestResult::class.java) } catch (_: Exception) { null }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getResultsByTest", e); emptyList()
    }
}


