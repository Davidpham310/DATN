package com.example.datn.data.remote.service.mini_game

import android.util.Log
import com.example.datn.data.remote.service.firestore.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.StudentMiniGameAnswer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

private const val TAG = "MiniGameService"

class MiniGameService @Inject constructor() :
    BaseFirestoreService<MiniGame>(
        collectionName = "minigames",
        clazz = MiniGame::class.java
    ) {

    private val questionRef = FirebaseFirestore.getInstance().collection("minigame_questions")
    private val optionRef = FirebaseFirestore.getInstance().collection("minigame_options")
    private val resultRef = FirebaseFirestore.getInstance().collection("student_minigame_results")
    private val answerRef = FirebaseFirestore.getInstance().collection("student_minigame_answers")

    // ==================== MINI GAME ====================

    suspend fun getMiniGameById(gameId: String): MiniGame? = try {
        Log.d(TAG, "Fetching mini game by ID: $gameId")
        val doc = collectionRef.document(gameId).get().await()
        if (doc.exists()) {
            doc.internalToDomain(clazz).also {
                Log.i(TAG, "✅ Loaded mini game: ${it?.title}")
            }
        } else {
            Log.w(TAG, "⚠️ Mini game not found for ID: $gameId")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching mini game: $gameId", e)
        null
    }

    suspend fun getMiniGamesByLesson(lessonId: String): List<MiniGame> = try {
        Log.d(TAG, "🎯 Fetching mini games for lesson: $lessonId")

        val snapshot = firestore.collection("minigames") // ✅ bắt đầu từ Firestore để tránh type mismatch
            .whereEqualTo("lessonId", lessonId)
//            .orderBy("createdAt")
            .get()
            .await()

        Log.d(TAG, "📄 Found ${snapshot.documents.size} documents in Firebase for lesson $lessonId")
        
        snapshot.documents.mapNotNull { doc ->
            try {
                Log.d(TAG, "🔍 Processing document ${doc.id}: ${doc.data}")
                val game = doc.internalToDomain(clazz)
                Log.d(TAG, "✅ Successfully parsed: ${game?.title} (level: ${game?.level})")
                game
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to parse mini game doc ${doc.id}", e)
                Log.e(TAG, "❌ Document data: ${doc.data}")
                null
            }
        }.also { games ->
            Log.i(TAG, "🎮 Final result: ${games.size} mini games for lesson $lessonId")
            games.forEach { game ->
                Log.i(TAG, "  📋 ${game.title} - ${game.level}")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching mini games by lesson: $lessonId", e)
        emptyList()
    }

    suspend fun getMiniGamesByTeacher(teacherId: String): List<MiniGame> = try {
        Log.d(TAG, "Fetching mini games for teacher: $teacherId")

        val snapshot = firestore.collection("minigames")
            .whereEqualTo("teacherId", teacherId)
//            .orderBy("createdAt")
            .get()
            .await()

        snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(clazz)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse mini game doc ${it.id}", e)
                null
            }
        }.also {
            Log.i(TAG, "✅ Found ${it.size} mini games for teacher $teacherId")
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching mini games by teacher: $teacherId", e)
        emptyList()
    }

    suspend fun addMiniGame(game: MiniGame): MiniGame? = try {
        Log.d(TAG, "Adding new mini game: ${game.title}")

        val docRef = if (game.id.isNotEmpty()) collectionRef.document(game.id)
        else collectionRef.document()

        val now = Instant.now()
        val data = game.copy(
            id = docRef.id,
            createdAt = now,
            updatedAt = now
        )

        docRef.set(data).await()
        Log.i(TAG, "✅ Added mini game: ${data.title} (${data.id})")
        data
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error adding mini game: ${game.title}", e)
        null
    }

    suspend fun updateMiniGame(gameId: String, game: MiniGame): Boolean = try {
        Log.d(TAG, "Updating mini game: $gameId")
        val updated = game.copy(
            id = gameId,
            updatedAt = Instant.now()
        )
        collectionRef.document(gameId).set(updated).await()
        Log.i(TAG, "✅ Updated mini game: $gameId")
        true
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error updating mini game: $gameId", e)
        false
    }

    suspend fun deleteMiniGame(gameId: String): Boolean = try {
        Log.d(TAG, "Deleting mini game: $gameId")

        val questions = getQuestionsByMiniGame(gameId)
        questions.forEach { deleteMiniGameQuestion(it.id) }

        collectionRef.document(gameId).delete().await()
        Log.i(TAG, "✅ Deleted mini game: $gameId and ${questions.size} related questions")
        true
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error deleting mini game: $gameId", e)
        false
    }

    suspend fun searchMiniGames(query: String, teacherId: String? = null): List<MiniGame> = try {
        Log.d(TAG, "Searching mini games with query: $query, teacherId=$teacherId")

        var queryRef: Query = firestore.collection("minigames")
        if (teacherId != null) {
            queryRef = queryRef.whereEqualTo("teacherId", teacherId)
        }

        val snapshot = queryRef
            .whereGreaterThanOrEqualTo("title", query)
            .whereLessThanOrEqualTo("title", query + "\uf8ff")
            .get()
            .await()

        snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(clazz)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse mini game doc ${it.id}", e)
                null
            }
        }.also {
            Log.i(TAG, "✅ Found ${it.size} mini games matching '$query'")
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error searching mini games", e)
        emptyList()
    }

    // ==================== MINI GAME QUESTIONS ====================

    suspend fun getQuestionsByMiniGame(gameId: String): List<MiniGameQuestion> = try {
        Log.d(TAG, "Fetching questions for mini game: $gameId")

        val snapshot = questionRef
            .whereEqualTo("miniGameId", gameId)
//            .orderBy("order")
            .get()
            .await()

        snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(MiniGameQuestion::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse question doc ${it.id}", e)
                null
            }
        }.also {
            Log.i(TAG, "✅ Found ${it.size} questions for mini game $gameId")
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching questions for mini game: $gameId", e)
        emptyList()
    }

    suspend fun addMiniGameQuestion(question: MiniGameQuestion): MiniGameQuestion? = try {
        Log.d(TAG, "Adding new question: ${question.content}")

        val existingQuestions = getQuestionsByMiniGame(question.miniGameId)
        val currentMaxQ = existingQuestions.maxOfOrNull { it.order } ?: 0
        val desiredOrder = when {
            // If no order provided (<= 0), append to end using 1-based indexing
            question.order <= 0 -> currentMaxQ + 1
            // Clamp to end (max+1) if larger than allowed insert position
            question.order > currentMaxQ + 1 -> currentMaxQ + 1
            else -> question.order
        }

        val questionsToShift = existingQuestions.filter { it.order >= desiredOrder }

        val docRef = if (question.id.isNotEmpty()) questionRef.document(question.id)
        else questionRef.document()

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

        Log.i(TAG, "✅ Added question: ${data.content} (${data.id})")
        data
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error adding question: ${question.content}", e)
        null
    }

    suspend fun updateMiniGameQuestion(questionId: String, question: MiniGameQuestion): Boolean {
        return try {
            val doc = questionRef.document(questionId).get().await()
            if (!doc.exists()) return false

            val oldQuestion = doc.internalToDomain(MiniGameQuestion::class.java)
            val oldOrder = oldQuestion.order

            val otherQuestions = getQuestionsByMiniGame(oldQuestion.miniGameId)
                .filter { it.id != questionId }

            val maxAllowedOrder = (otherQuestions.maxOfOrNull { it.order } ?: 0).coerceAtLeast(1)
            val clampedOrder = when {
                question.order < 1 -> oldOrder
                question.order > maxAllowedOrder -> maxAllowedOrder
                else -> question.order
            }

            if (clampedOrder == oldOrder) {
                val updated = question.copy(
                    id = questionId,
                    miniGameId = oldQuestion.miniGameId,
                    order = oldOrder,
                    questionType = oldQuestion.questionType,
                    createdAt = oldQuestion.createdAt,
                    updatedAt = Instant.now()
                )
                questionRef.document(questionId).set(updated).await()
                Log.i(TAG, "✅ Updated question: $questionId")
                return true
            }

            firestore.runBatch { batch ->
                otherQuestions.find { it.order == clampedOrder }?.let { conflictQuestion ->
                    batch.update(questionRef.document(conflictQuestion.id), "order", oldOrder)
                }

                val updated = question.copy(
                    id = questionId,
                    miniGameId = oldQuestion.miniGameId,
                    order = clampedOrder,
                    questionType = oldQuestion.questionType,
                    createdAt = oldQuestion.createdAt,
                    updatedAt = Instant.now()
                )
                batch.set(questionRef.document(questionId), updated)
            }.await()

            Log.i(TAG, "✅ Updated question: $questionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating question: $questionId", e)
            false
        }
    }

    suspend fun deleteMiniGameQuestion(questionId: String): Boolean = try {
        questionRef.document(questionId).delete().await()
        Log.i(TAG, "✅ Deleted question: $questionId")
        true
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error deleting question: $questionId", e)
        false
    }

    // ==================== MINI GAME OPTIONS ====================
    suspend fun getOptionsByQuestion(questionId: String): List<MiniGameOption> = try {
        Log.d(TAG, "Fetching option for questionId: $questionId")
        val snapshot = optionRef.whereEqualTo("miniGameQuestionId", questionId).get().await()

        snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(MiniGameOption::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Parse error for option: ${it.id}", e)
                null
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching options for question: $questionId", e)
        emptyList()
    }

    suspend fun addMiniGameOption(option: MiniGameOption): MiniGameOption? = try {
        // Load existing options to determine desired order and shifts (1-based)
        val existing = getOptionsByQuestion(option.miniGameQuestionId)

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
        Log.e(TAG, "❌ Error adding option", e)
        null
    }

    suspend fun updateMiniGameOption(optionId: String, option: MiniGameOption): Boolean {
        try {
            val doc = optionRef.document(optionId).get().await()
            if (!doc.exists()) return false

            val old = doc.internalToDomain(MiniGameOption::class.java)
            val oldOrder = old.order

            val others = getOptionsByQuestion(old.miniGameQuestionId).filter { it.id != optionId }
            val maxAllowed = (others.maxOfOrNull { it.order } ?: 0).coerceAtLeast(1)
            val clampedOrder = when {
                option.order < 1 -> oldOrder
                option.order > maxAllowed -> maxAllowed
                else -> option.order
            }

            if (clampedOrder == oldOrder) {
                val updated = option.copy(
                    id = optionId,
                    miniGameQuestionId = old.miniGameQuestionId,
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
                    miniGameQuestionId = old.miniGameQuestionId,
                    order = clampedOrder,
                    createdAt = old.createdAt,
                    updatedAt = Instant.now()
                )
                batch.set(optionRef.document(optionId), updated)
            }.await()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating option: $optionId", e)
            return false
        }
    }

    suspend fun deleteMiniGameOption(optionId: String): Boolean = try {
        optionRef.document(optionId).delete().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error deleting option: $optionId", e)
        false
    }

    // ==================== MINI GAME RESULTS ====================

    /**
     * Submit a mini game result to Firebase
     * Similar to test results but supports multiple attempts
     */
    suspend fun submitMiniGameResult(result: StudentMiniGameResult): StudentMiniGameResult? = try {
        Log.d(TAG, "Submitting mini game result: ${result.id}")

        val docRef = if (result.id.isNotEmpty()) resultRef.document(result.id)
                     else resultRef.document()

        val now = Instant.now()
        val data = result.copy(
            id = docRef.id,
            createdAt = if (result.createdAt.toEpochMilli() == 0L) now else result.createdAt,
            updatedAt = now
        )

        docRef.set(data).await()
        Log.i(TAG, "✅ Submitted mini game result: ${data.id}, Score: ${data.score}/${data.maxScore}, Attempt #${data.attemptNumber}")
        data
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error submitting mini game result: ${result.id}", e)
        null
    }

    /**
     * Get all results for a student and mini game (supports multiple attempts)
     */
    suspend fun getResultsByStudentAndMiniGame(
        studentId: String,
        miniGameId: String
    ): List<StudentMiniGameResult> = try {
        Log.d(TAG, "Fetching results for studentId: $studentId, miniGameId: $miniGameId")

        val snapshot = resultRef
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("miniGameId", miniGameId)
            .get()
            .await()

        val results = snapshot.documents.mapNotNull { doc ->
            try {
                doc.internalToDomain(StudentMiniGameResult::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse result doc ${doc.id}", e)
                null
            }
        }

        Log.i(TAG, "✅ Found ${results.size} results for student $studentId, mini game $miniGameId")
        results
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching results by student and mini game", e)
        emptyList()
    }

    /**
     * Get a specific result by ID
     */
    suspend fun getResultById(resultId: String): StudentMiniGameResult? = try {
        Log.d(TAG, "Fetching result by ID: $resultId")

        val doc = resultRef.document(resultId).get().await()
        if (doc.exists()) {
            doc.internalToDomain(StudentMiniGameResult::class.java).also {
                Log.i(TAG, "✅ Loaded result: ${it?.id}")
            }
        } else {
            Log.w(TAG, "⚠️ Result not found for ID: $resultId")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching result: $resultId", e)
        null
    }

    /**
     * Get all results for a mini game (for teacher/admin view)
     */
    suspend fun getResultsByMiniGame(miniGameId: String): List<StudentMiniGameResult> = try {
        Log.d(TAG, "Fetching all results for mini game: $miniGameId")

        val snapshot = resultRef
            .whereEqualTo("miniGameId", miniGameId)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            try {
                doc.internalToDomain(StudentMiniGameResult::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse result doc ${doc.id}", e)
                null
            }
        }.also {
            Log.i(TAG, "✅ Found ${it.size} results for mini game $miniGameId")
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching results by mini game: $miniGameId", e)
        emptyList()
    }

    /**
     * Get all results for a student (across all mini games)
     */
    suspend fun getResultsByStudent(studentId: String): List<StudentMiniGameResult> = try {
        Log.d(TAG, "Fetching all results for student: $studentId")

        val snapshot = resultRef
            .whereEqualTo("studentId", studentId)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            try {
                doc.internalToDomain(StudentMiniGameResult::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse result doc ${doc.id}", e)
                null
            }
        }.also {
            Log.i(TAG, "✅ Found ${it.size} results for student $studentId")
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching results by student: $studentId", e)
        emptyList()
    }

    // ==================== MINI GAME ANSWERS ====================

    /**
     * Save student answers for a mini game result
     */
    suspend fun saveMiniGameAnswers(answers: List<StudentMiniGameAnswer>): Boolean = try {
        Log.d(TAG, "Saving ${answers.size} mini game answers")

        answers.forEach { answer ->
            val docRef = if (answer.id.isNotEmpty()) answerRef.document(answer.id)
                        else answerRef.document()

            val now = Instant.now()
            val data = answer.copy(
                id = docRef.id,
                createdAt = if (answer.createdAt.toEpochMilli() == 0L) now else answer.createdAt,
                updatedAt = now
            )

            docRef.set(data).await()
        }

        Log.i(TAG, "✅ Saved ${answers.size} mini game answers")
        true
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error saving mini game answers", e)
        false
    }

    /**
     * Get answers for a specific result
     */
    suspend fun getAnswersByResultId(resultId: String): List<StudentMiniGameAnswer> = try {
        Log.d(TAG, "Fetching answers for result: $resultId")

        val snapshot = answerRef
            .whereEqualTo("resultId", resultId)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            try {
                doc.internalToDomain(StudentMiniGameAnswer::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse answer doc ${doc.id}", e)
                null
            }
        }.also {
            Log.i(TAG, "✅ Found ${it.size} answers for result $resultId")
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error fetching answers by result: $resultId", e)
        emptyList()
    }
}
