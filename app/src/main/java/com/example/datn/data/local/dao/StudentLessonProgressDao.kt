package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.StudentLessonProgressEntity

@Dao
interface StudentLessonProgressDao : BaseDao<StudentLessonProgressEntity> {

    /**
     * Lấy tiến độ học tập của một học sinh đối với một bài học cụ thể.
     * Đây là truy vấn cốt lõi để biết trạng thái học hiện tại (tiến độ, nội dung xem gần nhất).
     *
     * @param studentId ID của học sinh.
     * @param lessonId ID của bài học.
     * @return StudentLessonProgressEntity hoặc null nếu chưa bắt đầu.
     */
    @Query("SELECT * FROM student_lesson_progress WHERE studentId = :studentId AND lessonId = :lessonId")
    suspend fun getProgressByStudentAndLesson(studentId: String, lessonId: String): StudentLessonProgressEntity?

    /**
     * Lấy danh sách tất cả tiến độ bài học của một học sinh.
     * Thường dùng để hiển thị tổng quan các bài học đã/chưa học của học sinh.
     *
     * @param studentId ID của học sinh.
     * @return Danh sách các bản ghi tiến độ (StudentLessonProgressEntity).
     */
    @Query("SELECT * FROM student_lesson_progress WHERE studentId = :studentId ORDER BY lastAccessedAt DESC")
    suspend fun getAllProgressByStudent(studentId: String): List<StudentLessonProgressEntity>

    /**
     * Lấy tổng thời gian học của một học sinh đã dành cho tất cả các bài học.
     * Hữu ích cho các báo cáo tổng quan.
     *
     * @param studentId ID của học sinh.
     * @return Tổng thời gian (giây).
     */
    @Query("SELECT SUM(timeSpentSeconds) FROM student_lesson_progress WHERE studentId = :studentId")
    suspend fun getTotalTimeSpentByStudent(studentId: String): Long?
}