# Complete Implementation Summary

## 📊 Overview

Tổng cộng cần implement:
- **6 State files** ✅ (Completed)
- **6 Event files** ✅ (Completed)  
- **6 ViewModel files** (Creating skeletons)
- **6 Screen files** (Creating skeletons)

---

## ✅ COMPLETED FILES

### Test System - States & Events
1. ✅ `StudentTestListState.kt` - Categorized tests (upcoming/ongoing/completed)
2. ✅ `StudentTestListEvent.kt` - Load, refresh, navigate
3. ✅ `StudentTestTakingState.kt` - 4 answer types, timer, progress
4. ✅ `StudentTestTakingEvent.kt` - Navigation, answering, submit
5. ✅ `StudentTestResultState.kt` - Analytics, grading, class comparison
6. ✅ `StudentTestResultEvent.kt` - Load result, toggle details

### Lesson Progress - States & Events
7. ✅ `StudentLessonViewState.kt` - Progress tracking, content navigation
8. ✅ `StudentLessonViewEvent.kt` - Content navigation, mark viewed, save progress

### Class Detail - Complete
9. ✅ `StudentClassDetailState.kt` - Class info, lessons, student count
10. ✅ `StudentClassDetailEvent.kt` - Load, withdraw
11. ✅ `StudentClassDetailViewModel.kt` - Full implementation
12. ✅ `StudentClassDetailScreen.kt` - Complete UI

---

## 🔄 FILES TO CREATE

### A. Test System ViewModels

#### 1. StudentTestListViewModel.kt
```kotlin
@HiltViewModel
class StudentTestListViewModel @Inject constructor(
    // TODO: Inject TestUseCases when available
    private val classUseCases: ClassUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentTestListState, StudentTestListEvent>(...) {
    
    init { loadTests() }
    
    private fun loadTests() {
        // 1. Get student ID
        // 2. Get student's classes
        // 3. Get tests for these classes (TODO: need TestUseCases)
        // 4. Get student's test results
        // 5. Categorize: upcoming, ongoing, completed
        // 6. Update state
    }
}
```

**Key Logic:**
- Categorize tests by time and status
- Calculate time remaining
- Determine if student can take test
- Map test + result → TestWithStatus

#### 2. StudentTestTakingViewModel.kt
```kotlin
@HiltViewModel
class StudentTestTakingViewModel @Inject constructor(
    // TODO: Inject TestUseCases
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentTestTakingState, StudentTestTakingEvent>(...) {
    
    private var autoSaveJob: Job? = null
    private var timerJob: Job? = null
    
    private fun loadTest(testId: String) {
        // 1. Load test info
        // 2. Load questions + options
        // 3. Start timer
        // 4. Start auto-save (every 30s)
    }
    
    private fun submitTest() {
        // 1. Grade questions (auto for SINGLE/MULTIPLE/FILL_BLANK)
        // 2. Calculate total score
        // 3. Create StudentTestResult
        // 4. Submit to backend
        // 5. Navigate to result screen
    }
    
    private fun gradeQuestion(question, options, answer): Double {
        // Auto-grading logic based on question type
    }
    
    override fun onCleared() {
        timerJob?.cancel()
        autoSaveJob?.cancel()
        // Final auto-save before exit
    }
}
```

**Key Features:**
- Real-time countdown timer
- Auto-save every 30 seconds
- Auto-submit when time's up
- Instant grading (except essays)
- Question navigation

#### 3. StudentTestResultViewModel.kt
```kotlin
@HiltViewModel
class StudentTestResultViewModel @Inject constructor(
    // TODO: Inject TestUseCases
    notificationManager: NotificationManager
) : BaseViewModel<StudentTestResultState, StudentTestResultEvent>(...) {
    
    private fun loadResult(testId: String, resultId: String) {
        // 1. Load test info
        // 2. Load student's result
        // 3. Load all questions + options
        // 4. Load student's answers
        // 5. Map to QuestionWithAnswer
        // 6. Calculate class statistics (optional)
    }
}
```

---

### B. Test System UI Screens

#### 1. StudentTestListScreen.kt - Structure
```kotlin
@Composable
fun StudentTestListScreen(
    onNavigateToTest: (String) -> Unit,
    onNavigateToResult: (String, String) -> Unit,
    viewModel: StudentTestListViewModel = hiltViewModel()
) {
    Scaffold(topBar = { TopAppBar(...) }) { padding ->
        LazyColumn {
            // Upcoming Tests Section
            if (state.upcomingTests.isNotEmpty()) {
                item { SectionHeader("SẮP TỚI", count) }
                items(state.upcomingTests) { testWithStatus ->
                    UpcomingTestCard(...)
                }
            }
            
            // Ongoing Tests Section
            if (state.ongoingTests.isNotEmpty()) {
                item { SectionHeader("ĐANG DIỄN RA", count) }
                items(state.ongoingTests) { testWithStatus ->
                    OngoingTestCard(...)
                }
            }
            
            // Completed Tests Section
            if (state.completedTests.isNotEmpty()) {
                item { SectionHeader("ĐÃ HOÀN THÀNH", count) }
                items(state.completedTests) { testWithStatus ->
                    CompletedTestCard(...)
                }
            }
        }
    }
}

@Composable
private fun UpcomingTestCard(testWithStatus: TestWithStatus, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column {
            Text("📝 ${test.title}")
            Text("📅 Bắt đầu: ${formatTime(test.startTime)}")
            Text("📅 Kết thúc: ${formatTime(test.endTime)}")
            Text("⏱️ ${testWithStatus.timeRemaining}")
            Text("💯 Tổng điểm: ${test.totalScore}")
            Row { Text("Làm bài →") }
        }
    }
}
```

#### 2. StudentTestTakingScreen.kt - Structure
```kotlin
@Composable
fun StudentTestTakingScreen(
    testId: String,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (String, String) -> Unit,
    viewModel: StudentTestTakingViewModel = hiltViewModel()
) {
    // Timer countdown
    LaunchedEffect(state.timeRemaining) {
        if (state.timeRemaining <= 0) {
            viewModel.onEvent(StudentTestTakingEvent.ConfirmSubmit)
        }
    }
    
    // Submit confirmation dialog
    if (state.showSubmitDialog) {
        SubmitConfirmationDialog(...)
    }
    
    // Question list dialog
    if (state.showQuestionList) {
        QuestionListDialog(...)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(test.title) },
                actions = {
                    Text("⏱️ ${state.getFormattedTimeRemaining()}")
                    IconButton { /* Show question list */ }
                }
            )
        }
    ) { padding ->
        Column {
            // Progress bar
            LinearProgressIndicator(progress = state.progress)
            Text("Câu ${state.currentQuestionIndex + 1}/${state.questions.size}")
            
            // Current question display
            state.currentQuestion?.let { questionWithOptions ->
                when (questionWithOptions.question.questionType) {
                    QuestionType.SINGLE_CHOICE -> SingleChoiceQuestionView(...)
                    QuestionType.MULTIPLE_CHOICE -> MultipleChoiceQuestionView(...)
                    QuestionType.FILL_BLANK -> FillBlankQuestionView(...)
                    QuestionType.ESSAY -> EssayQuestionView(...)
                }
            }
            
            // Navigation buttons
            Row {
                OutlinedButton(
                    onClick = { viewModel.onEvent(PreviousQuestion) },
                    enabled = state.canGoPrevious
                ) { Text("← Trước") }
                
                OutlinedButton(
                    onClick = { viewModel.onEvent(NextQuestion) },
                    enabled = state.canGoNext
                ) { Text("Tiếp theo →") }
            }
            
            // Submit button
            Button(
                onClick = { viewModel.onEvent(ShowSubmitDialog) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nộp bài (${state.answeredCount}/${state.questions.size})")
            }
        }
    }
}

@Composable
private fun SingleChoiceQuestionView(
    question: TestQuestion,
    options: List<TestOption>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Column {
        Text(question.content, style = MaterialTheme.typography.titleMedium)
        Text("(${question.score} điểm)")
        
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option.id) }
                    .padding(8.dp)
            ) {
                RadioButton(
                    selected = option.id == selectedId,
                    onClick = { onSelect(option.id) }
                )
                Text(option.content)
            }
        }
    }
}
```

#### 3. StudentTestResultScreen.kt - Structure
```kotlin
@Composable
fun StudentTestResultScreen(
    testId: String,
    resultId: String,
    onNavigateBack: () -> Unit,
    viewModel: StudentTestResultViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Kết quả bài kiểm tra") }) }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            // Score summary card
            item {
                Card {
                    Column(horizontalAlignment = CenterHorizontally) {
                        Text("KẾT QUẢ BÀI KIỂM TRA")
                        Text(test.title)
                        
                        // Big score display
                        Text(
                            "📊 ${result.score}/${test.totalScore} điểm",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(state.gradeText)
                        
                        // Statistics
                        Text("✓ Đúng: ${state.correctCount}/${state.questions.size}")
                        Text("⏱️ Thời gian: ${state.durationMinutes} phút")
                        result.submissionTime?.let {
                            Text("📅 Nộp: ${formatTime(it)}")
                        }
                        
                        // Class comparison (if available)
                        state.classAverage?.let {
                            Text("📊 So với lớp:")
                            Text("• Điểm TB: $it")
                            state.getRankText()?.let { rank ->
                                Text("• Xếp hạng: $rank")
                            }
                        }
                    }
                }
            }
            
            // Detailed answers toggle
            item {
                OutlinedButton(
                    onClick = { viewModel.onEvent(ToggleDetailedAnswers) }
                ) {
                    Text(if (state.showDetailedAnswers) 
                        "Ẩn chi tiết" else "Xem chi tiết từng câu")
                }
            }
            
            // Detailed question by question (if shown)
            if (state.showDetailedAnswers) {
                itemsIndexed(state.questions) { index, questionWithAnswer ->
                    QuestionResultCard(
                        index = index + 1,
                        questionWithAnswer = questionWithAnswer
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionResultCard(
    index: Int,
    questionWithAnswer: QuestionWithAnswer
) {
    Card {
        Column {
            Row {
                Text("Câu $index:")
                if (questionWithAnswer.isCorrect) {
                    Icon(Icons.Default.CheckCircle, tint = Green)
                    Text("Đúng")
                } else {
                    Icon(Icons.Default.Cancel, tint = Red)
                    Text("Sai")
                }
                Text("${questionWithAnswer.earnedScore}/${questionWithAnswer.question.score}")
            }
            
            Text(questionWithAnswer.question.content)
            
            // Show options with indicators
            questionWithAnswer.options.forEach { option ->
                val isStudentAnswer = option.id in questionWithAnswer.studentSelectedIds
                val isCorrect = option.isCorrect
                
                Row {
                    when {
                        isCorrect -> Icon(Icons.Default.Check, tint = Green)
                        isStudentAnswer -> Icon(Icons.Default.Close, tint = Red)
                        else -> Icon(Icons.Default.Circle, tint = Gray)
                    }
                    Text(option.content)
                }
            }
        }
    }
}
```

---

### C. Lesson Progress System Updates

#### Update StudentLessonViewViewModel.kt

**Add these features to existing ViewModel:**

```kotlin
// Add at class level
private var timerJob: Job? = null

// Enhance loadLesson() to also load progress
private fun loadLesson(lessonId: String) {
    // ... existing code ...
    
    // NEW: Also load StudentLessonProgress
    val progress = progressUseCases.getProgress(studentId, lessonId).first()
    
    // Resume from last viewed if available
    val lastContentId = (progress as? Resource.Success)?.data?.lastAccessedContentId
    val resumeIndex = lastContentId?.let { id ->
        lessonContents.indexOfFirst { it.id == id }.takeIf { it >= 0 }
    } ?: 0
    
    setState {
        copy(
            currentContentIndex = resumeIndex,
            progress = (progress as? Resource.Success)?.data,
            sessionStartTime = System.currentTimeMillis()
        )
    }
    
    // Start auto-save timer
    startTimeTracking()
}

// NEW: Auto-save implementation
private fun startTimeTracking() {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
        while (isActive) {
            delay(30000) // 30 seconds
            saveProgress()
        }
    }
}

// IMPLEMENT saveProgress()
private fun saveProgress() {
    viewModelScope.launch {
        val state = state.value
        val lesson = state.lesson ?: return@launch
        
        // Calculate session duration
        val sessionDuration = (System.currentTimeMillis() - state.sessionStartTime) / 1000
        val totalTime = (state.progress?.timeSpentSeconds ?: 0) + sessionDuration
        
        // Create/update progress
        val progressData = StudentLessonProgress(
            id = state.progress?.id ?: UUID.randomUUID().toString(),
            studentId = studentId,
            lessonId = lesson.id,
            progressPercentage = state.progressPercentage,
            lastAccessedContentId = state.currentContent?.id,
            lastAccessedAt = Instant.now(),
            isCompleted = state.isLessonCompleted,
            timeSpentSeconds = totalTime,
            createdAt = state.progress?.createdAt ?: Instant.now(),
            updatedAt = Instant.now()
        )
        
        // TODO: Save via use case when available
        // progressUseCases.saveProgress(progressData)
        
        // Reset timer
        setState { copy(sessionStartTime = System.currentTimeMillis()) }
    }
}

// Don't forget cleanup
override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
    viewModelScope.launch { saveProgress() }
}
```

#### Update StudentLessonViewScreen.kt

**Add these UI components:**

```kotlin
@Composable
fun StudentLessonViewScreen(...) {
    // ... existing parameters ...
    
    // Auto-mark content as viewed after 2 seconds
    LaunchedEffect(state.currentContentIndex) {
        delay(2000)
        viewModel.onEvent(StudentLessonViewEvent.MarkCurrentAsViewed)
    }
    
    // Progress dialog
    if (state.showProgressDialog) {
        LessonProgressDialog(
            state = state,
            onDismiss = { viewModel.onEvent(DismissProgressDialog) }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lessonTitle) },
                actions = {
                    // Progress indicator in toolbar
                    TextButton(
                        onClick = { viewModel.onEvent(ShowProgressDialog) }
                    ) {
                        Text("${state.progressPercentage}%")
                    }
                }
            )
        }
    ) { padding ->
        Column {
            // Progress bar at top
            LinearProgressIndicator(
                progress = state.progressPercentage / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Content counter
            Text("Nội dung ${state.currentContentIndex + 1}/${state.lessonContents.size}")
            
            // Display current content
            LazyColumn(modifier = Modifier.weight(1f)) {
                state.currentContent?.let { content ->
                    item {
                        when (content.contentType) {
                            ContentType.TEXT -> TextContentView(content.content)
                            ContentType.VIDEO -> VideoContentView(content.content)
                            ContentType.AUDIO -> AudioContentView(content.content)
                            ContentType.IMAGE -> ImageContentView(content.content)
                            ContentType.PDF -> PdfContentView(content.content)
                            ContentType.MINIGAME -> MinigameContentView(content.content)
                        }
                    }
                }
            }
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.onEvent(PreviousContent) },
                    enabled = state.canGoPrevious
                ) {
                    Icon(Icons.Default.ArrowBack, null)
                    Text("Trước")
                }
                
                OutlinedButton(
                    onClick = { viewModel.onEvent(NextContent) },
                    enabled = state.canGoNext
                ) {
                    Text("Tiếp theo")
                    Icon(Icons.Default.ArrowForward, null)
                }
            }
            
            // Content navigation list
            ContentNavigationList(
                contents = state.lessonContents,
                currentIndex = state.currentContentIndex,
                viewedIds = state.viewedContentIds,
                onContentClick = { index ->
                    viewModel.onEvent(GoToContent(index))
                }
            )
        }
    }
}

@Composable
private fun ContentNavigationList(
    contents: List<LessonContent>,
    currentIndex: Int,
    viewedIds: Set<String>,
    onContentClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text("Danh sách nội dung", fontWeight = FontWeight.Bold)
        
        contents.forEachIndexed { index, content ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContentClick(index) }
                    .padding(vertical = 4.dp)
            ) {
                // Indicator
                Icon(
                    imageVector = if (viewedIds.contains(content.id))
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.Circle,
                    contentDescription = null,
                    tint = when {
                        index == currentIndex -> MaterialTheme.colorScheme.primary
                        viewedIds.contains(content.id) -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(Modifier.width(8.dp))
                
                Text(
                    text = "${index + 1}. ${content.title} (${content.contentType.displayName})",
                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun LessonProgressDialog(
    state: StudentLessonViewState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TIẾN ĐỘ BÀI HỌC") },
        text = {
            Column {
                Text(state.lesson?.title ?: "")
                
                Spacer(Modifier.height(16.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = state.progressPercentage / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${state.progressPercentage}%")
                
                Spacer(Modifier.height(16.dp))
                
                // Stats
                Text("✓ Đã xem: ${state.viewedContentIds.size}/${state.lessonContents.size} nội dung")
                
                state.progress?.let { progress ->
                    val minutes = progress.timeSpentSeconds / 60
                    Text("⏱️ Thời gian học: $minutes phút")
                    
                    // Time ago calculation would go here
                    Text("📅 Học lần cuối: ${formatTimeAgo(progress.lastAccessedAt)}")
                }
                
                state.currentContent?.let {
                    Text("📍 Đang xem: ${it.title}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
```

---

## 🎯 Summary

### What's Complete ✅
- All State classes with computed properties
- All Event classes with proper sealed hierarchies
- Implementation guides in markdown files

### What Needs Implementation 🔄
1. **ViewModels** - Add TODO comments for missing Use Cases
2. **UI Screens** - Create composables with proper Material3 design
3. **Use Cases** - Check which exist, create missing ones
4. **Navigation** - Add routes to Screen.kt and NavGraph

### Next Steps 📝
1. Create ViewModel files with TODO markers
2. Create UI Screen files  
3. Test with mock data
4. Implement/create missing Use Cases
5. Wire up navigation

---

## 📋 File Checklist

**Created:**
- [x] 6 State files
- [x] 6 Event files
- [x] Implementation guides (LESSON_PROGRESS_IMPLEMENTATION.md, TEST_SYSTEM_IMPLEMENTATION.md)

**To Create:**
- [ ] 3 Test ViewModels
- [ ] 3 Test UI Screens
- [ ] Update LessonViewViewModel
- [ ] Update LessonViewScreen

**To Check:**
- [ ] Available Use Cases
- [ ] Navigation routes
- [ ] Hilt modules configuration

