package com.example.datn.core.network.datasource

import com.example.datn.core.base.BaseDataSource
import com.example.datn.core.network.service.classroom.ClassService
import com.example.datn.core.network.service.conversation.ConversationService
import com.example.datn.core.network.service.lesson.LessonContentService
import com.example.datn.core.network.service.lesson.LessonService
import com.example.datn.core.network.service.message.MessageService
import com.example.datn.core.network.service.mini_game.MiniGameService
import com.example.datn.core.network.service.test.TestService
import com.example.datn.core.network.service.user.UserService
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.Conversation
import com.example.datn.domain.models.Message
import kotlinx.coroutines.flow.Flow

import java.io.InputStream
import javax.inject.Inject

class FirebaseDataSource @Inject constructor(
    private val userService: UserService,
    private val classService: ClassService,
    private val lessonService: LessonService,
    private val lessonContentService: LessonContentService,
    private val miniGameService: MiniGameService,
    private val testService: TestService,
    private val conversationService: ConversationService,
    private val messageService: MessageService
) : BaseDataSource() {

    // ==================== USER OPERATIONS ====================

    suspend fun getUserById(userId: String): Resource<User?> = safeCallWithResult {
        userService.getUserById(userId)
    }.toResource()

    suspend fun getUserByEmail(email: String): Resource<User?> = safeCallWithResult {
        userService.getUserByEmail(email)
    }.toResource()

    suspend fun getUsersByRole(role: String): Resource<List<User>> = safeCallWithResult {
        userService.getUsersByRole(role)
    }.toResource()

    suspend fun getAllUsers(): Resource<List<User>> = safeCallWithResult {
        userService.getAll()
    }.toResource()

    suspend fun addUser(user: User, id: String? = null): Resource<String> = safeCallWithResult {
        userService.add(id, user)
    }.toResource()

    suspend fun updateUser(id: String, user: User): Resource<Unit> = safeCallWithResult {
        userService.update(id, user)
    }.toResource()

    suspend fun deleteUser(userId: String): Resource<Unit> = safeCallWithResult {
        userService.delete(userId)
    }.toResource()

    suspend fun updateAvatar(userId: String, avatarUrl: String): Resource<Unit> = safeCallWithResult {
        userService.updateAvatar(userId, avatarUrl)
    }.toResource()

    // ==================== CLASS OPERATIONS ====================

    /**
     * Lấy tất cả lớp học
     */
    suspend fun getAllClasses(): Resource<List<Class>> = safeCallWithResult {
        classService.getAll()
    }.toResource()

    /**
     * Lấy lớp theo ID
     */
    suspend fun getClassById(classId: String): Resource<Class?> = safeCallWithResult {
        classService.getClassById(classId)
    }.toResource()

    /**
     * Lấy tất cả lớp của giáo viên
     */
    suspend fun getClassesByTeacher(teacherId: String): Resource<List<Class>> = safeCallWithResult {
        classService.getClassesByTeacher(teacherId)
    }.toResource()

    /**
     * Lấy tất cả lớp học sinh tham gia (status = APPROVED)
     */
    suspend fun getClassesByStudent(studentId: String): Resource<List<Class>> = safeCallWithResult {
        classService.getClassesByStudent(studentId)
    }.toResource()

    /**
     * Thêm lớp mới
     */
    suspend fun addClass(classObj: Class): Resource<Class?> = safeCallWithResult {
        classService.addClass(classObj)
    }.toResource()

    /**
     * Cập nhật lớp
     */
    suspend fun updateClass(classId: String, classObj: Class): Resource<Boolean> = safeCallWithResult {
        classService.updateClass(classId, classObj)
    }.toResource()

    /**
     * Xóa lớp (và tất cả enrollments liên quan)
     */
    suspend fun deleteClass(classId: String): Resource<Boolean> = safeCallWithResult {
        classService.deleteClass(classId)
    }.toResource()

    // ==================== ENROLLMENT OPERATIONS ====================

    /**
     * Thêm học sinh vào lớp (tạo enrollment request)
     * @param status Trạng thái ban đầu (PENDING hoặc APPROVED)
     */
    suspend fun addStudentToClass(
        classId: String,
        studentId: String,
        status: EnrollmentStatus = EnrollmentStatus.PENDING,
        approvedBy: String = ""
    ): Resource<Boolean> = safeCallWithResult {
        classService.addStudentToClass(
            classId = classId,
            studentId = studentId,
            status = status,
            approvedBy = approvedBy
        )
    }.toResource()

    /**
     * Xóa học sinh khỏi lớp
     */
    suspend fun removeStudentFromClass(
        classId: String,
        studentId: String
    ): Resource<Boolean> = safeCallWithResult {
        classService.removeStudentFromClass(classId, studentId)
    }.toResource()

    /**
     * Approve enrollment request
     */
    suspend fun approveEnrollment(
        classId: String,
        studentId: String,
        approvedBy: String
    ): Resource<Boolean> = safeCallWithResult {
        classService.updateEnrollmentStatus(
            classId = classId,
            studentId = studentId,
            status = EnrollmentStatus.APPROVED,
            approvedBy = approvedBy
        )
    }.toResource()

    /**
     * Reject enrollment request
     */
    suspend fun rejectEnrollment(
        classId: String,
        studentId: String,
        rejectionReason: String,
        rejectedBy: String
    ): Resource<Boolean> = safeCallWithResult {
        classService.updateEnrollmentStatus(
            classId = classId,
            studentId = studentId,
            status = EnrollmentStatus.REJECTED,
            approvedBy = rejectedBy,
            rejectionReason = rejectionReason
        )
    }.toResource()

    /**
     * Cập nhật trạng thái enrollment
     */
    suspend fun updateEnrollmentStatus(
        classId: String,
        studentId: String,
        status: EnrollmentStatus,
        approvedBy: String = "",
        rejectionReason: String = ""
    ): Resource<Boolean> = safeCallWithResult {
        classService.updateEnrollmentStatus(
            classId = classId,
            studentId = studentId,
            status = status,
            approvedBy = approvedBy,
            rejectionReason = rejectionReason
        )
    }.toResource()

    /**
     * Lấy danh sách học sinh trong lớp
     * @param status Filter theo trạng thái (null = tất cả)
     */
    suspend fun getStudentsInClass(
        classId: String,
        status: EnrollmentStatus? = null
    ): Resource<List<ClassStudent>> = safeCallWithResult {
        classService.getStudentsInClass(classId, status)
    }.toResource()

    /**
     * Lấy danh sách học sinh đã được approve trong lớp
     */
    suspend fun getApprovedStudentsInClass(classId: String): Resource<List<ClassStudent>> =
        getStudentsInClass(classId, EnrollmentStatus.APPROVED)

    /**
     * Lấy danh sách enrollment requests đang pending
     */
    suspend fun getPendingEnrollments(classId: String): Resource<List<ClassStudent>> =
        getStudentsInClass(classId, EnrollmentStatus.PENDING)

    /**
     * Lấy enrollment của một học sinh trong lớp cụ thể
     */
    suspend fun getEnrollment(
        classId: String,
        studentId: String
    ): Resource<ClassStudent?> = safeCallWithResult {
        classService.getEnrollment(classId, studentId)
    }.toResource()

    /**
     * Kiểm tra xem học sinh có trong lớp không (và đã được approve)
     */
    suspend fun isStudentInClass(
        classId: String,
        studentId: String
    ): Resource<Boolean> = safeCallWithResult {
        val enrollment = classService.getEnrollment(classId, studentId)
        enrollment != null && enrollment.enrollmentStatus == EnrollmentStatus.APPROVED
    }.toResource()

    /**
     * Kiểm tra xem học sinh có enrollment request pending không
     */
    suspend fun hasPendingEnrollment(
        classId: String,
        studentId: String
    ): Resource<Boolean> = safeCallWithResult {
        val enrollment = classService.getEnrollment(classId, studentId)
        enrollment != null && enrollment.enrollmentStatus == EnrollmentStatus.PENDING
    }.toResource()

    // ==================== BATCH OPERATIONS ====================

    /**
     * Approve nhiều enrollment requests cùng lúc
     */
    suspend fun batchApproveEnrollments(
        classId: String,
        studentIds: List<String>,
        approvedBy: String
    ): Resource<List<Boolean>> = safeCallWithResult {
        studentIds.map { studentId ->
            classService.updateEnrollmentStatus(
                classId = classId,
                studentId = studentId,
                status = EnrollmentStatus.APPROVED,
                approvedBy = approvedBy
            )
        }
    }.toResource()

    /**
     * Xóa nhiều học sinh khỏi lớp cùng lúc
     */
    suspend fun batchRemoveStudentsFromClass(
        classId: String,
        studentIds: List<String>
    ): Resource<List<Boolean>> = safeCallWithResult {
        studentIds.map { studentId ->
            classService.removeStudentFromClass(classId, studentId)
        }
    }.toResource()


    // ==================== LESSON OPERATIONS ====================

    suspend fun addLesson(lesson: Lesson): Resource<Lesson?> = safeCallWithResult {
        lessonService.addLesson(lesson)
    }.toResource()

    suspend fun getLessonsByClass(classId: String): Resource<List<Lesson>> = safeCallWithResult {
        lessonService.getLessonsByClass(classId)
    }.toResource()

    suspend fun getLessonById(lessonId: String): Resource<Lesson?> = safeCallWithResult {
        lessonService.getLessonById(lessonId)
    }.toResource()

    suspend fun updateLesson(lessonId: String, lesson: Lesson): Resource<Boolean> = safeCallWithResult {
        lessonService.updateLesson(lessonId, lesson)
    }.toResource()

    suspend fun deleteLesson(lessonId: String): Resource<Boolean> = safeCallWithResult {
        lessonService.deleteLesson(lessonId)
    }.toResource()

// ==================== LESSON CONTENT OPERATIONS ====================

    suspend fun getLessonContent(lessonId: String): Resource<List<LessonContent>> = safeCallWithResult {
        lessonContentService.getContentByLesson(lessonId)
    }.toResource()

    suspend fun addLessonContent(
        content: LessonContent,
        fileStream: InputStream? = null,
        fileSize: Long = 0
    ): Resource<LessonContent?> = safeCallWithResult {
        lessonContentService.addContent(content, fileStream, fileSize)
    }.toResource()

    suspend fun updateLessonContent(
        contentId: String,
        content: LessonContent,
        newFileStream: InputStream? = null,
        newFileSize: Long = 0
    ): Resource<Boolean> = safeCallWithResult {
        lessonContentService.updateContent(contentId, content, newFileStream, newFileSize)
    }.toResource()

    suspend fun deleteLessonContent(contentId: String): Resource<Boolean> = safeCallWithResult {
        lessonContentService.deleteContent(contentId)
    }.toResource()

    // ==================== MINI GAME OPERATIONS ====================

    suspend fun addMiniGame(game: MiniGame): Resource<MiniGame?> = safeCallWithResult {
        miniGameService.addMiniGame(game)
    }.toResource()

    suspend fun getMiniGameById(gameId: String): Resource<MiniGame?> = safeCallWithResult {
        miniGameService.getMiniGameById(gameId)
    }.toResource()

    suspend fun getMiniGamesByLesson(lessonId: String): Resource<List<MiniGame>> = safeCallWithResult {
        miniGameService.getMiniGamesByLesson(lessonId)
    }.toResource()

    suspend fun getMiniGamesByTeacher(teacherId: String): Resource<List<MiniGame>> = safeCallWithResult {
        miniGameService.getMiniGamesByTeacher(teacherId)
    }.toResource()

    suspend fun updateMiniGame(gameId: String, game: MiniGame): Resource<Boolean> = safeCallWithResult {
        miniGameService.updateMiniGame(gameId, game)
    }.toResource()

    suspend fun deleteMiniGame(gameId: String): Resource<Boolean> = safeCallWithResult {
        miniGameService.deleteMiniGame(gameId)
    }.toResource()

    suspend fun searchMiniGames(query: String, teacherId: String? = null): Resource<List<MiniGame>> = safeCallWithResult {
        miniGameService.searchMiniGames(query, teacherId)
    }.toResource()

    // ==================== MINI GAME QUESTION OPERATIONS ====================

    suspend fun addMiniGameQuestion(question: MiniGameQuestion): Resource<MiniGameQuestion?> = safeCallWithResult {
        miniGameService.addMiniGameQuestion(question)
    }.toResource()

    suspend fun getQuestionsByMiniGame(gameId: String): Resource<List<MiniGameQuestion>> = safeCallWithResult {
        miniGameService.getQuestionsByMiniGame(gameId)
    }.toResource()


    suspend fun updateMiniGameQuestion(questionId: String, question: MiniGameQuestion): Resource<Boolean> = safeCallWithResult {
        miniGameService.updateMiniGameQuestion(questionId, question)
    }.toResource()

    suspend fun deleteMiniGameQuestion(questionId: String): Resource<Boolean> = safeCallWithResult {
        miniGameService.deleteMiniGameQuestion(questionId)
    }.toResource()

    // ==================== MINI GAME OPTION OPERATIONS ====================
    suspend fun addMiniGameOption(option: MiniGameOption): Resource<MiniGameOption?> = safeCallWithResult {
        miniGameService.addMiniGameOption(option)
    }.toResource()

    suspend fun getMiniGameOptionsByQuestion(questionId: String): Resource<List<MiniGameOption>> = safeCallWithResult {
        miniGameService.getOptionsByQuestion(questionId)
    }.toResource()

    suspend fun updateMiniGameOption(optionId: String, option: MiniGameOption): Resource<Boolean> = safeCallWithResult {
        miniGameService.updateMiniGameOption(optionId, option)
    }.toResource()

    suspend fun deleteMiniGameOption(optionId: String): Resource<Boolean> = safeCallWithResult {
        miniGameService.deleteMiniGameOption(optionId)
    }.toResource()

    // ==================== TEST OPTION OPERATIONS ====================
    suspend fun addTestOption(option: TestOption): Resource<TestOption?> = safeCallWithResult {
        testService.addOption(option)
    }.toResource()

    suspend fun getTestOptionsByQuestion(questionId: String): Resource<List<TestOption>> = safeCallWithResult {
        testService.getOptionsByQuestion(questionId)
    }.toResource()

    suspend fun updateTestOption(optionId: String, option: TestOption): Resource<Boolean> = safeCallWithResult {
        testService.updateOption(optionId, option)
    }.toResource()

    suspend fun deleteTestOption(optionId: String): Resource<Boolean> = safeCallWithResult {
        testService.deleteOption(optionId)
    }.toResource()

    // ==================== TEST OPERATIONS ====================
    suspend fun addTest(test: Test): Resource<Test?> = safeCallWithResult {
        testService.addTest(test)
    }.toResource()

    suspend fun getTestById(testId: String): Resource<Test?> = safeCallWithResult {
        testService.getTestById(testId)
    }.toResource()

    suspend fun getTestsByLesson(lessonId: String): Resource<List<Test>> = safeCallWithResult {
        testService.getTestsByLesson(lessonId)
    }.toResource()

    suspend fun updateTest(test: Test): Resource<Test?> = safeCallWithResult {
        testService.updateTest(test.id, test)
        test
    }.toResource()

    suspend fun deleteTest(testId: String): Resource<Boolean> = safeCallWithResult {
        testService.deleteTest(testId)
    }.toResource()

    suspend fun getTestQuestions(testId: String): Resource<List<com.example.datn.domain.models.TestQuestion>> = safeCallWithResult {
        testService.getQuestionsByTest(testId)
    }.toResource()

    suspend fun addTestQuestion(question: com.example.datn.domain.models.TestQuestion): Resource<com.example.datn.domain.models.TestQuestion?> = safeCallWithResult {
        testService.addTestQuestion(question)
    }.toResource()

    suspend fun updateTestQuestion(question: com.example.datn.domain.models.TestQuestion): Resource<com.example.datn.domain.models.TestQuestion?> = safeCallWithResult {
        testService.updateTestQuestion(question.id, question)
        question
    }.toResource()

    suspend fun deleteTestQuestion(questionId: String): Resource<Boolean> = safeCallWithResult {
        testService.deleteTestQuestion(questionId)
    }.toResource()

    suspend fun getTestQuestionById(questionId: String): Resource<com.example.datn.domain.models.TestQuestion?> = safeCallWithResult {
        testService.getTestQuestionById(questionId)
    }.toResource()

    // ==================== TEST RESULT OPERATIONS ====================
    suspend fun submitTestResult(result: com.example.datn.domain.models.StudentTestResult): Resource<com.example.datn.domain.models.StudentTestResult?> = safeCallWithResult {
        testService.submitResult(result)
    }.toResource()

    suspend fun getStudentResult(studentId: String, testId: String): Resource<com.example.datn.domain.models.StudentTestResult?> = safeCallWithResult {
        testService.getResultByStudentAndTest(studentId, testId)
    }.toResource()

    suspend fun getResultsByTest(testId: String): Resource<List<com.example.datn.domain.models.StudentTestResult>> = safeCallWithResult {
        testService.getResultsByTest(testId)
    }.toResource()

    // ==================== MESSAGING OPERATIONS ====================

    /**
     * Lấy danh sách cuộc hội thoại của người dùng
     */
    fun getConversationsByUser(userId: String): Flow<List<Conversation>> {
        return conversationService.getConversationsByUser(userId)
    }

    /**
     * Lấy tin nhắn trong cuộc hội thoại (real-time)
     */
    fun getMessages(conversationId: String): Flow<Message> {
        return messageService.getMessages(conversationId)
    }

    /**
     * Gửi tin nhắn
     */
    suspend fun sendMessage(message: Message): Resource<String> = safeCallWithResult {
        messageService.sendMessage(message)
    }.toResource()

    /**
     * Tạo cuộc hội thoại mới
     */
    suspend fun createConversation(
        conversation: Conversation,
        participantIds: List<String>
    ): Resource<Conversation> = safeCallWithResult {
        conversationService.createConversation(conversation, participantIds)
    }.toResource()

    /**
     * Đánh dấu cuộc hội thoại là đã đọc
     */
    suspend fun updateLastViewed(
        conversationId: String,
        userId: String,
        lastViewedAt: java.time.Instant
    ): Resource<Unit> = safeCallWithResult {
        messageService.markMessagesAsRead(conversationId, userId)
    }.toResource()

    /**
     * Cập nhật thời gian tin nhắn cuối cùng
     */
    suspend fun updateConversationLastMessageAt(conversationId: String): Resource<Unit> = safeCallWithResult {
        conversationService.updateLastMessageAt(conversationId)
    }.toResource()

    /**
     * Lấy cuộc hội thoại theo ID
     */
    suspend fun getConversationById(conversationId: String): Resource<Conversation?> = safeCallWithResult {
        conversationService.getConversationById(conversationId)
    }.toResource()

    /**
     * Tìm cuộc hội thoại 1-1 giữa 2 người dùng
     */
    suspend fun findOneToOneConversation(user1Id: String, user2Id: String): Resource<Conversation?> = safeCallWithResult {
        conversationService.findOneToOneConversation(user1Id, user2Id)
    }.toResource()

    // ==================== HELPER ====================

    /**
     * Helper để chuyển Result<T> thành Resource<T>
     */
    private fun <T> Result<T>.toResource(): Resource<T> {
        return if (this.isSuccess) {
            Resource.Success(this.getOrThrow())
        } else {
            Resource.Error(this.exceptionOrNull()?.message ?: "Unknown Firebase Error")
        }
    }
}