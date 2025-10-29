package com.example.datn.core.network.service.mini_game

import android.util.Log
import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.MiniGameOption
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
        Log.d(TAG, "Fetching mini games for lesson: $lessonId")

        val snapshot = firestore.collection("minigames") // ✅ bắt đầu từ Firestore để tránh type mismatch
            .whereEqualTo("lessonId", lessonId)
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
            Log.i(TAG, "✅ Found ${it.size} mini games for lesson $lessonId")
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

        val docRef = if (question.id.isNotEmpty()) questionRef.document(question.id)
        else questionRef.document()

        val now = Instant.now()
        val data = question.copy(
            id = docRef.id,
            createdAt = now,
            updatedAt = now
        )

        docRef.set(data).await()
        Log.i(TAG, "✅ Added question: ${data.content} (${data.id})")
        data
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error adding question: ${question.content}", e)
        null
    }

    suspend fun updateMiniGameQuestion(questionId: String, question: MiniGameQuestion): Boolean = try {
        val updated = question.copy(
            id = questionId,
            updatedAt = Instant.now()
        )
        questionRef.document(questionId).set(updated).await()
        Log.i(TAG, "✅ Updated question: $questionId")
        true
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error updating question: $questionId", e)
        false
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
        val docRef = if (option.id.isNotEmpty()) optionRef.document(option.id) else optionRef.document()
        val now = Instant.now()
        val data = option.copy(id = docRef.id, createdAt = now, updatedAt = now)
        docRef.set(data).await()
        data
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error adding option", e)
        null
    }

    suspend fun updateMiniGameOption(optionId: String, option: MiniGameOption): Boolean = try {
        val updated = option.copy(id = optionId, updatedAt = Instant.now())
        optionRef.document(optionId).set(updated).await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error updating option: $optionId", e)
        false
    }

    suspend fun deleteMiniGameOption(optionId: String): Boolean = try {
        optionRef.document(optionId).delete().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error deleting option: $optionId", e)
        false
    }
}
