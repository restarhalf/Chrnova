package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * 首页ViewModel
 * 
 * 管理首页的UI状态，包括：
 * - 当前时间更新（每分钟刷新）
 * - 今日课程列表
 * - 课程时间段渲染
 * - 问候语和日期显示
 */
class HomeViewModel(
    observeAllCoursesUseCase: ObserveAllCoursesUseCase,
    private val getCampusTimetableUseCase: GetCampusTimetableUseCase,
    private val buildHomeClockSnapshotUseCase: BuildHomeClockSnapshotUseCase,
    private val buildHomeTodayScheduleUseCase: BuildHomeTodayScheduleUseCase,
    private val buildHomeHeaderUiUseCase: BuildHomeHeaderUiUseCase,
    private val buildHomePeriodSectionsUseCase: BuildHomePeriodSectionsUseCase,
    private val buildHomePeriodRenderRowsUseCase: BuildHomePeriodRenderRowsUseCase,
    private val buildHomeSurfaceUiUseCase: BuildHomeSurfaceUiUseCase,
) : ViewModel() {
    /**
     * 首页基础UI状态
     * 
     * @param courses 所有课程列表
     * @param nowMs 当前时间戳（毫秒）
     */
    data class HomeUiState(
        val courses: List<Course>,
        val nowMs: Long,
    )

    /**
     * 时间段渲染UI
     * 
     * @param title 时间段标题（如"上午"、"下午"）
     * @param rows 该时间段内的课程行列表
     */
    data class SectionRenderUi(
        val title: String,
        val rows: List<BuildHomePeriodRenderRowsUseCase.RowRenderUi>,
    )

    /**
     * 首页完整渲染状态
     * 
     * 包含首页所有需要渲染的UI数据。
     */
    data class HomeRenderState(
        /** 头部UI（问候语、日期等） */
        val headerUi: BuildHomeHeaderUiUseCase.HeaderUi,
        /** 今日课程安排 */
        val todaySchedule: BuildHomeTodayScheduleUseCase.HomeTodaySchedule,
        /** 时间段渲染列表 */
        val sectionRenders: List<SectionRenderUi>,
        /** 表面UI（背景相关） */
        val surfaceUi: BuildHomeSurfaceUiUseCase.SurfaceUi,
        /** 当前时间的分钟数（用于判断课程状态） */
        val nowMinutes: Int,
    )

    // 每分钟更新一次当前时间
    private val _nowMs = flow {
        while (true) {
            emit(kotlin.time.Clock.System.now().toEpochMilliseconds())
            delay(60_000L.milliseconds)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    )

    private val _uiState: StateFlow<HomeUiState> =
        combine(observeAllCoursesUseCase(), _nowMs) { courses, nowMs ->
            HomeUiState(courses = courses, nowMs = nowMs)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState(courses = emptyList(), nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()),
            )

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<HomeUiState> = _uiState

    /**
     * 获取校区课表时间配置
     * 
     * @param campus 校区
     * @return 时间槽列表
     */
    fun getCampusTimetable(campus: Campus): List<TimetableSlot> = getCampusTimetableUseCase(campus)

    /**
     * 构建时钟快照
     * 
     * @param nowMs 当前时间戳
     * @return 时钟快照，包含日期、星期等信息
     */
    fun buildClockSnapshot(nowMs: Long): BuildHomeClockSnapshotUseCase.Snapshot {
        return buildHomeClockSnapshotUseCase(nowMs)
    }

    /**
     * 构建今日课程安排
     * 
     * @param courses 所有课程列表
     * @param totalWeeks 学期总周数
     * @param termStartMs 学期开始时间戳
     * @param todayDayOfWeekMon1 今日星期几（1=周一）
     * @param nowMs 当前时间戳
     * @return 今日课程安排
     */
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

    /**
     * 构建头部UI
     * 
     * @param dateLabel 日期标签
     * @param courseCount 今日课程数量
     * @param hasFirstClass 是否有第一节课程
     * @return 头部UI数据
     */
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

    /**
     * 构建头部UI（简化版）
     * 
     * @param clockSnapshot 时钟快照
     * @param todaySchedule 今日课程安排
     * @return 头部UI数据
     */
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

    /**
     * 构建首页完整渲染状态
     * 
     * @param courses 所有课程列表
     * @param campus 当前校区
     * @param termStartMs 学期开始时间戳
     * @param totalWeeks 学期总周数
     * @param hasBackground 是否有背景图片
     * @param componentsAlpha 组件透明度
     * @param nowMs 当前时间戳
     * @return 首页渲染状态
     */
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

    /**
     * 构建时间段分组
     * 
     * @param schedule 今日课程安排
     * @return 时间段列表（上午、下午、晚上）
     */
    fun buildSections(
        schedule: BuildHomeTodayScheduleUseCase.HomeTodaySchedule,
    ): List<BuildHomePeriodSectionsUseCase.SectionUi> {
        return buildHomePeriodSectionsUseCase(schedule)
    }

    /**
     * 构建表面UI
     * 
     * @param hasBackground 是否有背景图片
     * @param componentsAlpha 组件透明度
     * @return 表面UI数据
     */
    fun buildSurfaceUi(
        hasBackground: Boolean,
        componentsAlpha: Float,
    ): BuildHomeSurfaceUiUseCase.SurfaceUi {
        return buildHomeSurfaceUiUseCase(
            hasBackground = hasBackground,
            componentsAlpha = componentsAlpha
        )
    }

    /**
     * 构建时间段内的课程行渲染数据
     * 
     * @param items 课程项列表
     * @param timetable 时间槽配置
     * @param nowMinutes 当前时间的分钟数
     * @return 课程行渲染数据列表
     */
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
