package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.ui.model.home.BuildHomeClockSnapshotUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeHeaderUiUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomePeriodRenderRowsUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomePeriodSectionsUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeSurfaceUiUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeTodayScheduleUseCase
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.ui.model.ExamUi
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant


class HomeViewModel(
    private val courseRepository: CourseRepository,
    observeAllExaminations: ObserveAllExaminationsUseCase,
    private val auth: JwxtAuthPort,
    private val isExamNotEnded: IsExamNotEndedUseCase,
    private val timetable: TimetablePort,
    private val buildHomeClockSnapshotUseCase: BuildHomeClockSnapshotUseCase,
    private val buildHomeTodayScheduleUseCase: BuildHomeTodayScheduleUseCase,
    private val buildHomeHeaderUiUseCase: BuildHomeHeaderUiUseCase,
    private val buildHomePeriodSectionsUseCase: BuildHomePeriodSectionsUseCase,
    private val buildHomePeriodRenderRowsUseCase: BuildHomePeriodRenderRowsUseCase,
    private val buildHomeSurfaceUiUseCase: BuildHomeSurfaceUiUseCase,
) : ViewModel() {
    
    @Stable
    data class HomeUiState(
        val courses: ImmutableList<Course>,
        val exams: ImmutableList<Examination>,
        val nowMs: Long,
    )

    
    @Stable
    data class SectionRenderUi(
        val title: String,
        val rows: ImmutableList<BuildHomePeriodRenderRowsUseCase.RowRenderUi>,
    )

    
    @Stable
    data class HomeRenderState(
        
        val headerUi: BuildHomeHeaderUiUseCase.HeaderUi,
        
        val todaySchedule: BuildHomeTodayScheduleUseCase.HomeTodaySchedule,
        
        val sectionRenders: ImmutableList<SectionRenderUi>,
        
        val surfaceUi: BuildHomeSurfaceUiUseCase.SurfaceUi,
        
        val nowMinutes: Int,
    )


    private val _nowMs = flow {
        while (true) {
            emit(Clock.System.now().toEpochMilliseconds())
            delay(60_000L.milliseconds)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Clock.System.now().toEpochMilliseconds(),
    )

    private val _userNo = auth.observeProfile().map { it.userNo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _uiState: StateFlow<HomeUiState> =
        combine(courseRepository.observeAllCourses(), _nowMs, _userNo, observeAllExaminations()) { courses, nowMs, userNo, exams ->
            val filteredExams = if (userNo.isNotBlank()) {
                exams.filter { it.userNo == userNo }
            } else {
                exams
            }
            HomeUiState(courses = courses.toPersistentList(), exams = filteredExams.toPersistentList(), nowMs = nowMs)
        }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState(courses = persistentListOf(), exams = persistentListOf(), nowMs = Clock.System.now().toEpochMilliseconds()),
            )

    
    val uiState: StateFlow<HomeUiState> = _uiState

    
    fun getCampusTimetable(campus: Campus): List<TimetableSlot> = timetable.getCampusTimetable(campus)

    
    private fun buildClockSnapshot(nowMs: Long): BuildHomeClockSnapshotUseCase.Snapshot {
        return buildHomeClockSnapshotUseCase(nowMs)
    }

    
    private fun buildTodaySchedule(
        courses: List<Course>,
        totalWeeks: Int,
        termStartMs: Long,
        todayDayOfWeekMon1: Int,
        nowMs: Long,
    ): BuildHomeTodayScheduleUseCase.HomeTodaySchedule {
        return buildHomeTodayScheduleUseCase(
            courses = courses,
            totalWeeks = totalWeeks,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = todayDayOfWeekMon1,
            nowMs = nowMs
        )
    }

    
    private fun buildHeaderUi(
        dateLabel: String,
        courseCount: Int,
        hasFirstClass: Boolean,
        dayOfWeekCount: Int
    ): BuildHomeHeaderUiUseCase.HeaderUi {
        return buildHomeHeaderUiUseCase(
            dateLabel = dateLabel,
            courseCount = courseCount,
            hasFirstClass = hasFirstClass,
            dayOfWeekCount = dayOfWeekCount
        )
    }

    
    private fun buildHeaderUi(
        clockSnapshot: BuildHomeClockSnapshotUseCase.Snapshot,
        todaySchedule: BuildHomeTodayScheduleUseCase.HomeTodaySchedule,
    ): BuildHomeHeaderUiUseCase.HeaderUi {
        val dayOfWeekCount = clockSnapshot.dayOfWeekMon1
        return buildHeaderUi(
            dateLabel = clockSnapshot.dateLabel,
            courseCount = todaySchedule.todayCourses.size,
            hasFirstClass = todaySchedule.hasFirstClass,
            dayOfWeekCount = dayOfWeekCount
        )
    }

    
    fun buildHomeRenderState(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        hasBackground: Boolean,
        componentsAlpha: Float,
        nowMs: Long,
    ): HomeRenderState {
        val clockSnapshot = buildClockSnapshot(nowMs = nowMs)
        val todaySchedule =
            buildTodaySchedule(
                courses = courses,
                totalWeeks = totalWeeks,
                termStartMs = termStartMs,
                todayDayOfWeekMon1 = clockSnapshot.dayOfWeekMon1,
                nowMs = nowMs
            )
        val headerUi = buildHeaderUi(clockSnapshot = clockSnapshot, todaySchedule = todaySchedule)
        val timetable = getCampusTimetable(campus)
        val nowMinutes = clockSnapshot.nowMinutes
        val sectionRenders =
            buildSections(todaySchedule).map { section ->
                SectionRenderUi(
                    title = section.title,
                    rows = buildPeriodRenderRows(section.items, timetable, nowMinutes).toPersistentList()
                )
            }

        return HomeRenderState(
            headerUi = headerUi,
            todaySchedule = todaySchedule,
            sectionRenders = sectionRenders.toPersistentList(),
            surfaceUi = buildSurfaceUi(hasBackground = hasBackground, componentsAlpha = componentsAlpha),
            nowMinutes = nowMinutes
        )
    }

    
    private fun buildSections(
        schedule: BuildHomeTodayScheduleUseCase.HomeTodaySchedule,
    ): List<BuildHomePeriodSectionsUseCase.SectionUi> {
        return buildHomePeriodSectionsUseCase(schedule)
    }

    
    private fun buildSurfaceUi(
        hasBackground: Boolean,
        componentsAlpha: Float,
    ): BuildHomeSurfaceUiUseCase.SurfaceUi {
        return buildHomeSurfaceUiUseCase(
            hasBackground = hasBackground,
            componentsAlpha = componentsAlpha
        )
    }

    
    private fun buildPeriodRenderRows(
        items: List<BuildHomeTodayScheduleUseCase.PeriodItem>,
        timetable: List<TimetableSlot>,
        nowMinutes: Int,
    ): List<BuildHomePeriodRenderRowsUseCase.RowRenderUi> {
        return buildHomePeriodRenderRowsUseCase(
            items = items,
            timetable = timetable,
            nowMinutes = nowMinutes
        )
    }

    
    fun getTodayExams(exams: List<Examination>, nowMs: Long): List<Examination> {
        val todayDate = Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
        return exams
            .filter { it.time.startsWith(todayDate) }
            .sortedBy { it.time.substringAfter(" ").ifBlank { "00:00" } }
    }

    
    fun buildExamUiList(exams: List<Examination>, nowMs: Long): List<ExamUi> {
        return exams.map { exam ->
            val isEnded = !isExamNotEnded(exam.time, nowMs)
            val isStarted = isExamStarted(exam.time, nowMs)
            val timePart = exam.time.substringAfter(" ").substringAfter(" ").ifBlank { "待定" }
            val startTime = timePart.substringBefore("~").substringBefore("-").trim()
            val endTime = timePart.substringAfter("~").substringAfter("-").trim().ifBlank { startTime }
            val location = exam.examinationPlace.ifBlank { "待定" }
            ExamUi(
                title = exam.courseName.ifBlank { "未命名课程" },
                startTime = startTime,
                endTime = endTime,
                location = location,
                accentCourseName = exam.courseName.ifBlank { exam.courseNumber },
                isEnded = isEnded,
                isStarted = isStarted,
            )
        }
    }

    
    private fun isExamStarted(rawTime: String, nowMs: Long): Boolean {
        val date = DATE_REGEX.find(rawTime)?.groupValues?.getOrNull(1) ?: return false
        val start = TIME_REGEX.find(rawTime)?.groupValues?.getOrNull(1) ?: return false
        val normalized = "${date}T${start.padStart(5, '0')}"
        val startDateTime = runCatching { kotlinx.datetime.LocalDateTime.parse(normalized) }.getOrNull() ?: return false
        val startMs = startDateTime.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault()).toEpochMilliseconds()
        return nowMs >= startMs
    }

    private companion object {
        val DATE_REGEX = Regex("(\\d{4}-\\d{2}-\\d{2})")
        val TIME_REGEX = Regex("(\\d{1,2}:\\d{2})[~-]")
    }
}
