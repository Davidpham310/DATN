package com.example.datn.core.di

import FileRepositoryImpl
import android.content.Context
import androidx.room.Room
import com.example.datn.BuildConfig
import com.example.datn.core.network.datasource.FirebaseAuthDataSource
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.classroom.ClassService
import com.example.datn.core.network.service.lesson.LessonContentService
import com.example.datn.core.network.service.lesson.LessonService
import com.example.datn.core.network.service.mini_game.MiniGameService
import com.example.datn.core.network.service.test.TestService
import com.example.datn.core.network.service.minio.MinIOService
import com.example.datn.core.network.service.user.UserService
import com.example.datn.core.network.service.notification.FirestoreNotificationService
import com.example.datn.core.network.service.FirebaseMessagingService
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.data.local.AppDatabase
import com.example.datn.data.local.dao.ClassDao
import com.example.datn.data.local.dao.ConversationDao
import com.example.datn.data.local.dao.ConversationParticipantDao
import com.example.datn.data.local.dao.LessonContentDao
import com.example.datn.data.local.dao.LessonDao
import com.example.datn.data.local.dao.MessageDao
import com.example.datn.data.local.dao.MiniGameDao
import com.example.datn.data.local.dao.MiniGameQuestionDao
import com.example.datn.data.local.dao.MiniGameOptionDao
import com.example.datn.data.local.dao.StudentTestResultDao
import com.example.datn.data.local.dao.TestDao
import com.example.datn.data.local.dao.TestQuestionDao
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.local.dao.NotificationDao
import com.example.datn.data.local.dao.StudentDao
import com.example.datn.data.local.dao.ParentDao
import com.example.datn.data.local.dao.ParentStudentDao
import com.example.datn.data.local.dao.TeacherDao
import com.example.datn.data.repository.impl.AuthRepositoryImpl
import com.example.datn.data.repository.impl.ClassRepositoryImpl
import com.example.datn.data.repository.impl.LessonContentRepositoryImpl
import com.example.datn.data.repository.impl.LessonRepositoryImpl
import com.example.datn.data.repository.impl.MessagingRepositoryImpl
import com.example.datn.data.repository.impl.MiniGameRepositoryImpl
import com.example.datn.data.repository.impl.TestOptionRepositoryImpl
import com.example.datn.data.repository.impl.TestQuestionRepositoryImpl
import com.example.datn.data.repository.impl.TestRepositoryImpl
import com.example.datn.data.repository.impl.UserRepositoryImpl
import com.example.datn.data.repository.impl.NotificationRepositoryImpl
import com.example.datn.data.repository.impl.StudentRepositoryImpl
import com.example.datn.data.repository.impl.ParentRepositoryImpl
import com.example.datn.domain.repository.IAuthRepository
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.repository.IFileRepository
import com.example.datn.domain.repository.ILessonContentRepository
import com.example.datn.domain.repository.ILessonRepository
import com.example.datn.domain.repository.IMessagingRepository
import com.example.datn.domain.repository.IMiniGameRepository
import com.example.datn.domain.repository.ITestOptionRepository
import com.example.datn.domain.repository.ITestQuestionRepository
import com.example.datn.domain.repository.ITestRepository
import com.example.datn.domain.repository.IUserRepository
import com.example.datn.domain.repository.INotificationRepository
import com.example.datn.domain.repository.IStudentRepository
import com.example.datn.domain.repository.IParentRepository
import com.example.datn.domain.usecase.messaging.*
import com.example.datn.domain.usecase.test.TestQuestionUseCases
import com.example.datn.domain.usecase.minio.MinIOUseCase
import com.example.datn.domain.usecase.notification.SendTeacherNotificationUseCase
import com.example.datn.domain.usecase.notification.SendBulkNotificationUseCase
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.parentstudent.*
import com.example.datn.core.network.service.student.StudentService
import com.example.datn.core.network.service.parent.ParentService
import com.example.datn.core.network.service.parent.ParentStudentService
import com.example.datn.data.local.dao.ClassStudentDao
import com.example.datn.data.repository.impl.MessagingPermissionRepositoryImpl
import com.example.datn.domain.repository.IMessagingPermissionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.minio.MinioClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Firebase
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    // 🧩 MinIO Client
    @Provides
    @Singleton
    fun provideMinioClient(): MinioClient {
        val endpoint = BuildConfig.MINIO_ENDPOINT
        val accessKey = BuildConfig.MINIO_ACCESS_KEY
        val secretKey = BuildConfig.MINIO_SECRET_KEY

        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
    }

    // 🧩 Tên bucket (inject được ở nơi cần)
    @Provides
    @Singleton
    fun provideMinioBucketName(): String = BuildConfig.MINIO_BUCKET


    @Provides
    @Singleton
    fun provideMinioService(
        client: MinioClient,
        bucketName: String
    ): MinIOService = MinIOService(client, bucketName)

    // Local Database
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app_db")
            .fallbackToDestructiveMigration() // Cho phép reset nếu cấu trúc đổi
            .build()


    // Notifications
    @Provides
    @Singleton
    fun provideNotificationManager(): NotificationManager = NotificationManager()

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideClassDao(db: AppDatabase): ClassDao = db.classDao()

    @Provides
    @Singleton
    fun provideLessonDao(db: AppDatabase): LessonDao = db.lessonDao()

    @Provides
    @Singleton
    fun provideClassStudentDao(db: AppDatabase): ClassStudentDao = db.classStudentDao()

    @Provides
    @Singleton
    fun provideParentStudentDao(db: AppDatabase): ParentStudentDao = db.parentStudentDao()

    @Provides
    @Singleton
    fun provideLessonContentDao(db: AppDatabase): LessonContentDao = db.lessonContentDao()

    @Provides
    @Singleton
    fun provideMiniGameDao(db: AppDatabase): MiniGameDao = db.miniGameDao()

    @Provides
    @Singleton
    fun provideMiniGameQuestionDao(db: AppDatabase): MiniGameQuestionDao = db.miniGameQuestionDao()

    @Provides
    @Singleton
    fun provideMiniGameOptionDao(db: AppDatabase): MiniGameOptionDao = db.miniGameOptionDao()

    @Provides
    @Singleton
    fun provideTestDao(db: AppDatabase): TestDao = db.testDao()

    @Provides
    @Singleton
    fun provideTestQuestionDao(db: AppDatabase): TestQuestionDao = db.testQuestionDao()

    @Provides
    @Singleton
    fun provideStudentTestResultDao(db: AppDatabase): StudentTestResultDao = db.studentTestResultDao()

    // Firebase data sources
    @Provides
    @Singleton
    fun provideFirebaseAuthDataSource(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): FirebaseAuthDataSource = FirebaseAuthDataSource(firebaseAuth, firestore)

    @Provides
    @Singleton
    fun provideFirebaseDataSource(
        userService: UserService,
        classService: ClassService,
        lessonService: LessonService,
        lessonContentService: LessonContentService,
        miniGameService: MiniGameService,
        testService: TestService,
        conversationService: com.example.datn.core.network.service.conversation.ConversationService,
        messageService: com.example.datn.core.network.service.message.MessageService
    ): FirebaseDataSource = FirebaseDataSource(
        userService,
        classService,
        lessonService,
        lessonContentService,
        miniGameService,
        testService,
        conversationService,
        messageService
    )

    // Repositories
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthDataSource: FirebaseAuthDataSource,
        userDao: UserDao
    ): IAuthRepository = AuthRepositoryImpl(firebaseAuthDataSource, userDao)

    @Provides
    @Singleton
    fun provideUserRepository(
        firebaseDataSource: FirebaseDataSource
    ): IUserRepository = UserRepositoryImpl(firebaseDataSource)
    
    @Provides
    @Singleton
    fun provideStudentRepository(
        firebaseDataSource: FirebaseDataSource,
        studentService: StudentService,
        studentDao: StudentDao
    ): IStudentRepository = StudentRepositoryImpl(
        firebaseDataSource,
        studentService,
        studentDao
    )
    
    @Provides
    @Singleton
    fun provideParentRepository(
        firebaseDataSource: FirebaseDataSource,
        parentService: ParentService,
        parentStudentService: ParentStudentService,
        studentService: StudentService,
        classService: ClassService,
        userService: UserService,
        parentDao: ParentDao,
        studentDao: StudentDao,
        parentStudentDao: ParentStudentDao,
        classDao: ClassDao,
        classStudentDao: ClassStudentDao,
        teacherDao: TeacherDao,
        userDao: UserDao
    ): IParentRepository = ParentRepositoryImpl(
        firebaseDataSource,
        parentService,
        parentStudentService,
        studentService,
        classService,
        userService,
        parentDao,
        studentDao,
        parentStudentDao,
        classDao,
        classStudentDao,
        teacherDao,
        userDao
    )


    @Provides
    @Singleton
    fun provideClassRepository(
        firebaseDataSource: FirebaseDataSource,
        classDao: ClassDao
    ): IClassRepository = ClassRepositoryImpl(firebaseDataSource , classDao)

    @Provides
    @Singleton
    fun provideLessonRepository(
        firebaseDataSource: FirebaseDataSource,
        lessonDao: LessonDao
    ): ILessonRepository = LessonRepositoryImpl(firebaseDataSource, lessonDao)

    @Provides
    @Singleton
    fun provideLessonContentRepository(
        firebaseDataSource: FirebaseDataSource,
        lessonContentDao: LessonContentDao,
        minIOUseCase: MinIOUseCase
    ): ILessonContentRepository = LessonContentRepositoryImpl(
        firebaseDataSource,
        lessonContentDao,
        minIOUseCase
    )

    @Provides
    @Singleton
    fun provideMiniGameRepository(
        firebaseDataSource: FirebaseDataSource,
        miniGameDao: MiniGameDao,
        miniGameQuestionDao: MiniGameQuestionDao,
        miniGameOptionDao: MiniGameOptionDao
    ): IMiniGameRepository = MiniGameRepositoryImpl(
        firebaseDataSource,
        miniGameDao,
        miniGameQuestionDao,
        miniGameOptionDao
    )

    @Provides
    @Singleton
    fun provideTestOptionRepository(
        firebaseDataSource: FirebaseDataSource,
        db: AppDatabase
    ): ITestOptionRepository = TestOptionRepositoryImpl(
        firebaseDataSource,
        db.testOptionDao()
    )

    @Provides
    @Singleton
    fun provideTestRepository(
        firebaseDataSource: FirebaseDataSource,
        testDao: TestDao,
        testQuestionDao: TestQuestionDao,
        studentTestResultDao: StudentTestResultDao
    ): ITestRepository = TestRepositoryImpl(
        firebaseDataSource,
        testDao,
        testQuestionDao,
        studentTestResultDao
    )

    @Provides
    @Singleton
    fun provideTestQuestionRepository(
        firebaseDataSource: FirebaseDataSource,
        testQuestionDao: TestQuestionDao
    ): ITestQuestionRepository = TestQuestionRepositoryImpl(
        firebaseDataSource,
        testQuestionDao
    )

    @Provides
    fun provideTestQuestionUseCases(
        repository: ITestQuestionRepository
    ): TestQuestionUseCases = TestQuestionUseCases(repository)

    @Provides
    @Singleton
    fun provideFileRepository(
        minIOService: MinIOService
    ): IFileRepository = FileRepositoryImpl(minIOService)

    // Services
    @Provides
    @Singleton
    fun provideUserService(): UserService = UserService()


    @Provides
    @Singleton
    fun provideClassService(): ClassService = ClassService()


    @Provides
    @Singleton
    fun provideLessonService(): LessonService = LessonService()

    @Provides
    @Singleton
    fun provideLessonContentService(
        minIOService: MinIOService
    ): LessonContentService = LessonContentService(minIOService)

    @Provides
    @Singleton
    fun provideMiniGameService(): MiniGameService = MiniGameService()

    @Provides
    @Singleton
    fun provideTestService(): TestService = TestService()

    @Provides
    @Singleton
    fun provideConversationService(): com.example.datn.core.network.service.conversation.ConversationService =
        com.example.datn.core.network.service.conversation.ConversationService()

    @Provides
    @Singleton
    fun provideMessageService(): com.example.datn.core.network.service.message.MessageService =
        com.example.datn.core.network.service.message.MessageService()
    
    @Provides
    @Singleton
    fun provideStudentService(): StudentService = StudentService()
    
    @Provides
    @Singleton
    fun provideParentService(): ParentService = ParentService()
    
    @Provides
    @Singleton
    fun provideParentStudentService(): ParentStudentService = ParentStudentService()

    // Messaging DAOs
    @Provides
    @Singleton
    fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideConversationParticipantDao(db: AppDatabase): ConversationParticipantDao = db.conversationParticipantDao()

    @Provides
    @Singleton
    fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()
    
    @Provides
    @Singleton
    fun provideStudentDao(db: AppDatabase): StudentDao = db.studentDao()
    
    @Provides
    @Singleton
    fun provideParentDao(db: AppDatabase): ParentDao = db.parentDao()
    
    @Provides
    @Singleton
    fun provideTeacherDao(db: AppDatabase): TeacherDao = db.teacherDao()

    // Firebase Messaging Service
    @Provides
    @Singleton
    fun provideFirebaseMessagingService(
        firestore: FirebaseFirestore
    ): FirebaseMessagingService = FirebaseMessagingService(firestore)
    
    // Messaging Repository
    @Provides
    @Singleton
    fun provideMessagingRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        participantDao: ConversationParticipantDao,
        firebaseMessaging: FirebaseMessagingService,
        firebaseAuthDataSource: FirebaseAuthDataSource,
        userDao: UserDao
    ): IMessagingRepository = MessagingRepositoryImpl(
        conversationDao,
        messageDao,
        participantDao,
        firebaseMessaging,
        firebaseAuthDataSource,
        userDao
    )

    // Messaging Permission Repository
    @Provides
    @Singleton
    fun provideMessagingPermissionRepository(
        classDao: ClassDao,
        classStudentDao: ClassStudentDao,
        parentStudentDao: ParentStudentDao,
        userDao: UserDao
    ): IMessagingPermissionRepository = MessagingPermissionRepositoryImpl(
        classDao,
        classStudentDao,
        parentStudentDao,
        userDao
    )

    // Messaging Use Cases
    @Provides
    @Singleton
    fun provideMessagingUseCases(
        repository: IMessagingRepository,
        permissionRepository: IMessagingPermissionRepository,
        userRepository: IUserRepository,
        firebaseMessaging: FirebaseMessagingService,
        conversationDao: ConversationDao,
        participantDao: ConversationParticipantDao,
        firebaseAuthDataSource: FirebaseAuthDataSource
    ): MessagingUseCases = MessagingUseCases(
        getConversations = GetConversationsUseCase(repository),
        getMessages = GetMessagesUseCase(repository),
        sendMessage = SendMessageUseCase(repository),
        createConversation = CreateConversationUseCase(repository),
        markAsRead = MarkAsReadUseCase(repository),
        createGroupConversation = CreateGroupConversationUseCase(firebaseMessaging, conversationDao, participantDao),
        addParticipants = AddParticipantsUseCase(firebaseMessaging),
        leaveGroup = LeaveGroupUseCase(firebaseMessaging, participantDao),
        getGroupParticipants = GetGroupParticipantsUseCase(participantDao, firebaseAuthDataSource),
        getAllowedRecipients = GetAllowedRecipientsUseCase(permissionRepository, userRepository),
        checkMessagingPermission = CheckMessagingPermissionUseCase(permissionRepository),
        toggleMuteConversation = ToggleMuteConversationUseCase(repository),
        getUnreadCount = GetUnreadCountUseCase(repository)
    )

    // Notification Service
    @Provides
    @Singleton
    fun provideFirestoreNotificationService(): FirestoreNotificationService =
        FirestoreNotificationService()

    // Notification Repository
    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationDao: NotificationDao,
        firestoreService: FirestoreNotificationService
    ): INotificationRepository = NotificationRepositoryImpl(
        notificationDao,
        firestoreService
    )

    // Notification Use Cases
    @Provides
    @Singleton
    fun provideSendTeacherNotificationUseCase(
        repository: INotificationRepository
    ): SendTeacherNotificationUseCase = SendTeacherNotificationUseCase(repository)

    @Provides
    @Singleton
    fun provideSendBulkNotificationUseCase(
        notificationRepository: INotificationRepository,
        userRepository: IUserRepository,
        classRepository: IClassRepository,
        firestoreService: FirestoreNotificationService,
        firestore: FirebaseFirestore
    ): SendBulkNotificationUseCase = SendBulkNotificationUseCase(
        notificationRepository,
        userRepository,
        classRepository,
        firestoreService,
        firestore
    )

    @Provides
    @Singleton
    fun provideGetReferenceObjectsUseCase(
        classRepository: IClassRepository,
        lessonRepository: ILessonRepository,
        lessonContentRepository: ILessonContentRepository,
        testRepository: ITestRepository,
        miniGameRepository: IMiniGameRepository
    ): com.example.datn.domain.usecase.notification.GetReferenceObjectsUseCase =
        com.example.datn.domain.usecase.notification.GetReferenceObjectsUseCase(
            classRepository,
            lessonRepository,
            lessonContentRepository,
            testRepository,
            miniGameRepository
        )
    
    // Parent Student Use Cases
    @Provides
    @Singleton
    fun provideParentStudentUseCases(
        firebaseAuthDataSource: FirebaseAuthDataSource,
        studentService: StudentService,
        userService: UserService,
        parentStudentService: ParentStudentService,
        firebaseDataSource: FirebaseDataSource,
        parentRepository: IParentRepository
    ): ParentStudentUseCases = ParentStudentUseCases(
        createStudentAccount = CreateStudentAccountUseCase(
            firebaseAuthDataSource,
            studentService,
            userService,
            parentStudentService
        ),
        linkStudent = LinkStudentUseCase(parentStudentService),
        updateStudentInfo = UpdateStudentInfoUseCase(
            firebaseDataSource,
            studentService
        ),
        getLinkedStudents = GetLinkedStudentsUseCase(
            parentStudentService,
            studentService,
            firebaseDataSource
        ),
        unlinkStudent = UnlinkStudentUseCase(parentRepository),
        searchStudent = SearchStudentUseCase(
            studentService,
            firebaseDataSource
        ),
        updateRelationship = UpdateRelationshipUseCase(parentStudentService),
        getStudentClassesForParent = GetStudentClassesForParentUseCase(parentRepository)
    )

}
