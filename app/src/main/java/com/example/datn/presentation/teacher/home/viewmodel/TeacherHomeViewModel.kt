package com.example.datn.presentation.teacher.home.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseEvent
import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IProgressRepository
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType as UiNotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.coroutineScope

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    private val classUseCases: ClassUseCases,
    private val progressRepository: IProgressRepository,
    notificationManager: NotificationManager
) : BaseViewModel<TeacherHomeState, TeacherHomeEvent>(
    TeacherHomeState(),
    notificationManager
) {

    private data class StudentStats(
        val avgStudyTimeTodaySeconds: Long,
        val completedLessonsToday: Int,
        val activeStudentsToday: Int
    )

    private val currentTeacherIdFlow = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    init {
        load()
    }

    override fun onEvent(event: TeacherHomeEvent) {
        when (event) {
            TeacherHomeEvent.Load -> load()
            TeacherHomeEvent.Refresh -> load(isRefresh = true)
            TeacherHomeEvent.ClearError -> setState { copy(error = null) }
        }
    }

    private fun load(isRefresh: Boolean = false) {
        launch {
            Log.d(TAG, "load() start | isRefresh=$isRefresh")
            val loadingFlag = !isRefresh
            setState { copy(isLoading = loadingFlag, isRefreshing = isRefresh, error = null) }

            val teacherId = currentTeacherIdFlow.value.ifBlank {
                awaitNonBlank(currentTeacherIdFlow)
            }

            Log.d(TAG, "teacherId resolved | teacherId='${teacherId}'")

            if (teacherId.isBlank()) {
                setState { copy(isLoading = false, isRefreshing = false, error = "Vui lòng đăng nhập") }
                showNotification("Vui lòng đăng nhập", UiNotificationType.ERROR)
                Log.d(TAG, "load() abort | reason=blank_teacherId")
                return@launch
            }

            val currentUser = loadCurrentUser()
            Log.d(
                TAG,
                "currentUser loaded | userId='${currentUser?.id}' name='${currentUser?.name}' role='${currentUser?.role}'"
            )

            val classesRes = awaitFirstNonLoading(classUseCases.getClassesByTeacher(teacherId))
            val classes = (classesRes as? Resource.Success)?.data.orEmpty()

            Log.d(TAG, "classes loaded | result=${classesRes::class.simpleName} classesCount=${classes.size}")
            if (classes.isNotEmpty()) {
                Log.d(TAG, "classes sample | first3=${classes.take(3).joinToString { "${it.id}:${it.name}" }}")
            }

            val classStudentCounts = coroutineScope {
                classes.associate { clazz ->
                    clazz.id to async { loadApprovedStudentIds(clazz.id) }
                }.mapValues { it.value.await() }
            }

            Log.d(
                TAG,
                "approved students loaded | classCount=${classStudentCounts.size} totalApprovedStudents=${classStudentCounts.values.sumOf { it.size }}"
            )

            val totalStudents = classStudentCounts.values.sumOf { it.size }

            val stats = computeStudentStats(
                studentIds = classStudentCounts.values.flatten().distinct()
            )

            val studyMinutesLast7Days = computeStudyMinutesLast7Days(
                studentIds = classStudentCounts.values.flatten().distinct()
            )

            Log.d(
                TAG,
                "metrics computed | totalStudents=$totalStudents activeStudentsToday=${stats.activeStudentsToday} avgStudyTimeTodaySeconds=${stats.avgStudyTimeTodaySeconds} completedLessonsToday=${stats.completedLessonsToday}"
            )

            val classItems = classes
                .sortedByDescending { it.updatedAt }
                .take(10)
                .map { clazz ->
                    TeacherHomeClassItem(
                        clazz = clazz,
                        approvedStudentsCount = classStudentCounts[clazz.id]?.size ?: 0
                    )
                }

            setState {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    teacher = currentUser,
                    classes = classItems,
                    metrics = TeacherHomeMetrics(
                        totalStudents = totalStudents,
                        activeStudentsToday = stats.activeStudentsToday,
                        avgStudyTimeTodaySeconds = stats.avgStudyTimeTodaySeconds,
                        completedLessonsToday = stats.completedLessonsToday
                    ),
                    studyMinutesLast7Days = studyMinutesLast7Days,
                    error = null
                )
            }

            Log.d(TAG, "load() success")
        }
    }

    private suspend fun loadCurrentUser(): User? {
        return try {
            val res = awaitFirstNonLoading(authUseCases.getCurrentUser.invoke())
            Log.d(TAG, "getCurrentUser result=${res::class.simpleName}")
            (res as? Resource.Success)?.data
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUser exception", e)
            null
        }
    }

    private suspend fun loadApprovedStudentIds(classId: String): List<String> {
        return try {
            val res = awaitFirstNonLoading(classUseCases.getApprovedStudentsInClass(classId))
            val items = (res as? Resource.Success)?.data.orEmpty()
            val ids = items.map { it.studentId }.distinct()
            val errorMsg = (res as? Resource.Error)?.message
            Log.d(
                TAG,
                "approvedStudents | classId='${classId}' result=${res::class.simpleName} count=${ids.size} error='${errorMsg}'"
            )
            ids
        } catch (e: Exception) {
            Log.e(TAG, "approvedStudents exception | classId='${classId}'", e)
            emptyList()
        }
    }

    private suspend fun computeStudentStats(studentIds: List<String>): StudentStats {
        if (studentIds.isEmpty()) {
            Log.d(TAG, "computeStudentStats | studentIds empty")
            return StudentStats(
                avgStudyTimeTodaySeconds = 0L,
                completedLessonsToday = 0,
                activeStudentsToday = 0
            )
        }

        Log.d(TAG, "computeStudentStats | uniqueStudents=${studentIds.distinct().size} (cap=$MAX_STUDENTS_FOR_STATS)")

        val today = LocalDate.now()

        val todaySecondsList = coroutineScope {
            studentIds.take(MAX_STUDENTS_FOR_STATS).map { studentId ->
                async {
                    val res = awaitFirstNonLoading(progressRepository.getDailyStudyTime(studentId, today))
                    (res as? Resource.Success)?.data?.durationSeconds ?: 0L
                }
            }.map { it.await() }
        }

        val activeStudentsToday = todaySecondsList.count { it > 0L }

        val completedLessonsToday = coroutineScope {
            studentIds.take(MAX_STUDENTS_FOR_STATS).map { studentId ->
                async {
                    val res = awaitFirstNonLoading(progressRepository.getProgressOverview(studentId))
                    val list = (res as? Resource.Success)?.data.orEmpty()
                    countCompletedToday(list, today)
                }
            }.map { it.await() }
        }.sum()

        val avgStudyTimeTodaySeconds = if (todaySecondsList.isNotEmpty()) {
            val divisor = activeStudentsToday.coerceAtLeast(1)
            todaySecondsList.sum() / divisor
        } else {
            0L
        }

        return StudentStats(
            avgStudyTimeTodaySeconds = avgStudyTimeTodaySeconds,
            completedLessonsToday = completedLessonsToday,
            activeStudentsToday = activeStudentsToday
        )
    }

    private fun countCompletedToday(progressList: List<StudentLessonProgress>, today: LocalDate): Int {
        val zone = ZoneId.systemDefault()
        return progressList.count { p ->
            p.isCompleted && p.updatedAt.atZone(zone).toLocalDate() == today
        }
    }

    private suspend fun computeStudyMinutesLast7Days(studentIds: List<String>): List<TeacherHomeStudyDay> {
        val today = LocalDate.now()
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val targetDates = days.toSet()
        val secondsByDate = days.associateWith { 0L }.toMutableMap()

        if (studentIds.isEmpty()) {
            return days.map { date ->
                TeacherHomeStudyDay(date = date, minutes = 0)
            }
        }

        val dailyLists = coroutineScope {
            studentIds.take(MAX_STUDENTS_FOR_STATS).map { studentId ->
                async {
                    val res = awaitFirstNonLoading(progressRepository.getAllDailyStudyTime(studentId))
                    (res as? Resource.Success)?.data.orEmpty()
                }
            }.map { it.await() }
        }

        dailyLists.flatten()
            .filter { it.date in targetDates }
            .forEach { item ->
                secondsByDate[item.date] = (secondsByDate[item.date] ?: 0L) + item.durationSeconds
            }

        return days.map { date ->
            val minutes = ((secondsByDate[date] ?: 0L).coerceAtLeast(0L) / 60L).toInt()
            TeacherHomeStudyDay(date = date, minutes = minutes)
        }
    }

    private suspend fun <T> awaitFirstNonLoading(flow: Flow<Resource<T>>): Resource<T> {
        var result: Resource<T>? = null
        flow
            .filter { it !is Resource.Loading }
            .take(1)
            .collect { value -> result = value }
        return result ?: Resource.Error("Không thể tải dữ liệu")
    }

    private suspend fun awaitNonBlank(flow: Flow<String>): String {
        var result = ""
        flow
            .filter { it.isNotBlank() }
            .take(1)
            .collect { value -> result = value }
        return result
    }

    private companion object {
        const val TAG = "TeacherHomeVM"
        const val MAX_STUDENTS_FOR_STATS = 60
    }
}

data class TeacherHomeState(
    val teacher: User? = null,
    val classes: List<TeacherHomeClassItem> = emptyList(),
    val metrics: TeacherHomeMetrics = TeacherHomeMetrics(),
    val studyMinutesLast7Days: List<TeacherHomeStudyDay> = emptyList(),
    val isRefreshing: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState

data class TeacherHomeMetrics(
    val totalStudents: Int = 0,
    val activeStudentsToday: Int = 0,
    val avgStudyTimeTodaySeconds: Long = 0L,
    val completedLessonsToday: Int = 0
)

data class TeacherHomeStudyDay(
    val date: LocalDate,
    val minutes: Int
)

data class TeacherHomeClassItem(
    val clazz: Class,
    val approvedStudentsCount: Int
)

sealed class TeacherHomeEvent : BaseEvent {
    data object Load : TeacherHomeEvent()
    data object Refresh : TeacherHomeEvent()
    data object ClearError : TeacherHomeEvent()
}
