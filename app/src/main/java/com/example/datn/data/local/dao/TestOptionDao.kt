package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.TestOptionEntity

@Dao
interface TestOptionDao : BaseDao<TestOptionEntity> {

    /**
     * Lấy tất cả các đáp án lựa chọn cho một câu hỏi cụ thể trong bài kiểm tra.
     *
     * @param testQuestionId ID của câu hỏi.
     * @return Danh sách các đáp án (TestOptionEntity).
     */
    @Query("SELECT * FROM test_option WHERE testQuestionId = :testQuestionId")
    suspend fun getOptionsByQuestion(testQuestionId: String): List<TestOptionEntity>

    /**
     * Lấy danh sách các đáp án đúng cho một câu hỏi.
     * Cần thiết cho việc chấm điểm tự động, đặc biệt đối với câu hỏi trắc nghiệm đa lựa chọn.
     *
     * @param testQuestionId ID của câu hỏi.
     * @return Danh sách các đáp án đúng (isCorrect = 1).
     */
    @Query("SELECT * FROM test_option WHERE testQuestionId = :testQuestionId AND isCorrect = 1")
    suspend fun getCorrectOptionsForQuestion(testQuestionId: String): List<TestOptionEntity>
}