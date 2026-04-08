package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.usecase.BuildHomeClockSnapshotUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeHeaderUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodRenderRowsUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodSectionsUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeSurfaceUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeTodayScheduleUseCase
import restarhalf.stellar.schedule.domain.usecase.GetCampusTimetableUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase

class HomeViewModel(
    private val observeAllCoursesUseCase: ObserveAllCoursesUseCase,
    private val getCampusTimetableUseCase: GetCampusTimetableUseCase,
    private val buildHomeClockSnapshotUseCase: BuildHomeClockSnapshotUseCase,
    private val buildHomeTodayScheduleUseCase: BuildHomeTodayScheduleUseCase,
    private val buildHomeHeaderUiUseCase: BuildHomeHeaderUiUseCase,
    private val buildHomePeriodSectionsUseCase: BuildHomePeriodSectionsUseCase,
    private val buildHomePeriodRenderRowsUseCase: BuildHomePeriodRenderRowsUseCase,
    private val buildHomeSurfaceUiUseCase: BuildHomeSurfaceUiUseCase,
) : ViewModel() {
    data class SectionRenderUi(
        val title: String,
        val rows: List<BuildHomePeriodRenderRowsUseCase.RowRenderUi>,
    )

    data class HomeRenderState(
        val headerUi: BuildHomeHeaderUiUseCase.HeaderUi,
        val todaySchedule: BuildHomeTodayScheduleUseCase.HomeTodaySchedule,
        val sectionRenders: List<SectionRenderUi>,
        val surfaceUi: BuildHomeSurfaceUiUseCase.SurfaceUi,
        val nowMinutes: Int,
    )

    fun observeAllCourses(): Flow<List<Course>> = observeAllCoursesUseCase()

    fun getCampusTimetable(campus: Campus): List<TimetableSlot> = getCampusTimetableUseCase(campus)

    fun buildClockSnapshot(nowMs: Long): BuildHomeClockSnapshotUseCase.Snapshot {
        return buildHomeClockSnapshotUseCase(nowMs)
    }

    fun buildTodaySchedule(
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

    fun buildHeaderUi(
        dateLabel: String,
        courseCount: Int,
        hasFirstClass: Boolean,
    ): BuildHomeHeaderUiUseCase.HeaderUi {
        return buildHomeHeaderUiUseCase(
            dateLabel = dateLabel,
            courseCount = courseCount,
            hasFirstClass = hasFirstClass
        )
    }

    fun buildHeaderUi(
        clockSnapshot: BuildHomeClockSnapshotUseCase.Snapshot,
        todaySchedule: BuildHomeTodayScheduleUseCase.HomeTodaySchedule,
    ): BuildHomeHeaderUiUseCase.HeaderUi {
        return buildHeaderUi(
            dateLabel = clockSnapshot.dateLabel,
            courseCount = todaySchedule.todayCourses.size,
            hasFirstClass = todaySchedule.hasFirstClass
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
                    rows = buildPeriodRenderRows(section.items, timetable, nowMinutes)
                )
            }

        return HomeRenderState(
            headerUi = headerUi,
            todaySchedule = todaySchedule,
            sectionRenders = sectionRenders,
            surfaceUi = buildSurfaceUi(hasBackground = hasBackground, componentsAlpha = componentsAlpha),
            nowMinutes = nowMinutes
        )
    }

    fun buildSections(
        schedule: BuildHomeTodayScheduleUseCase.HomeTodaySchedule,
    ): List<BuildHomePeriodSectionsUseCase.SectionUi> {
        return buildHomePeriodSectionsUseCase(schedule)
    }

    fun buildSurfaceUi(
        hasBackground: Boolean,
        componentsAlpha: Float,
    ): BuildHomeSurfaceUiUseCase.SurfaceUi {
        return buildHomeSurfaceUiUseCase(
            hasBackground = hasBackground,
            componentsAlpha = componentsAlpha
        )
    }

    fun buildPeriodRenderRows(
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
}
