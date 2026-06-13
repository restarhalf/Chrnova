package restarhalf.stellar.schedule.ui.mapper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import restarhalf.stellar.schedule.core.course.filterNonOverlapping
import restarhalf.stellar.schedule.core.course.filterNonOverlappingForPreview
import restarhalf.stellar.schedule.core.course.findOverlappingCourses
import restarhalf.stellar.schedule.core.course.hasOverlapWith
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.ui.theme.pickCourseColor
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import restarhalf.stellar.schedule.ui.theme.pickCourseTitleColor

/**
 * 课程视觉样式数据类
 * 
 * @param cardColor 卡片颜色
 * @param titleColor 标题颜色
 * @param subColor 副标题颜色
 */
private data class CourseVisual(val cardColor: Color, val titleColor: Color, val subColor: Color)

/**
 * 每日渲染数据
 * 
 * @param items 课程渲染项列表
 */
data class DayRenderData(val items: List<CourseRenderItem>)

/**
 * 课程渲染项
 * 
 * @param model 课程卡片模型
 * @param overlaps 重叠的课程列表
 */
data class CourseRenderItem(val model: CourseCardModel, val overlaps: List<Course>)

/**
 * 课程卡片模型
 * 
 * 用于渲染课程卡片的不可变数据类。
 */
@Immutable
data class CourseCardModel(
    /** 课程名称 */
    val name: String,
    /** 上课地点 */
    val location: String,
    /** 教师 */
    val teacher: String,
    /** 顶部偏移量 */
    val topOffsetY: Dp,
    /** 卡片高度 */
    val height: Dp,
    /** 重叠课程数量徽章 */
    val badgeCount: Int?,
    /** 卡片颜色 */
    val color: Color,
    /** 标题颜色 */
    val titleColor: Color,
    /** 副文本颜色 */
    val subTextColor: Color,
    /** 卡片透明度 */
    val cardAlpha: Float,
)

/**
 * 构建每日渲染数据
 * 
 * 将课程列表转换为渲染数据，处理重叠课程和非当前周课程。
 * 
 * @param dayCourses 当天课程列表
 * @param page 当前周次
 * @param showNonCurrentWeek 是否显示非当前周课程
 * @param isDarkMode 是否为深色模式
 * @param mutedCourseColor 非当前周课程颜色
 * @param mutedTitleColor 非当前周标题颜色
 * @param mutedSubColor 非当前周副标题颜色
 * @param yForSection 计算节次Y坐标的函数
 * @param heightForSections 计算节次高度的函数
 * @param cellInset 单元格内边距
 * @param contentCardAlpha 内容卡片透明度
 * @return 每日渲染数据
 */
fun buildDayRenderData(
    dayCourses: List<Course>,
    page: Int,
    showNonCurrentWeek: Boolean,
    isDarkMode: Boolean,
    mutedCourseColor: Color,
    mutedTitleColor: Color,
    mutedSubColor: Color,
    yForSection: (Int) -> Dp,
    heightForSections: (Int) -> Dp,
    cellInset: Dp,
    contentCardAlpha: Float,
): DayRenderData {

    val shown =
        if (page == 0) {
            filterNonOverlappingForPreview(dayCourses)
        } else {
            val current = dayCourses.filter { isCourseActiveInWeek(it, page) }
            if (!showNonCurrentWeek) {
                filterNonOverlapping(current)
            } else {
                val nonCurrent = dayCourses.filter { !isCourseActiveInWeek(it, page) }
                val nonCurrentNoConflict = nonCurrent.filter { !hasOverlapWith(it, current) }
                filterNonOverlapping(current + nonCurrentNoConflict)
            }
        }

    fun splitCourseSections(course: Course): List<Pair<Int, Int>> {
        val start = course.startSection
        val end = course.startSection + course.sectionCount - 1
        if (course.sectionCount <= 0) return emptyList()
        val boundaries = listOf(4, 8)
        val segments = ArrayList<Pair<Int, Int>>()
        var cursorStart = start
        val cursorEnd = end
        for (b in boundaries) {
            val crosses = cursorStart <= b && cursorEnd >= (b + 1)
            if (crosses) {
                segments.add(cursorStart to b)
                cursorStart = b + 1
            }
        }
        if (cursorStart <= cursorEnd) {
            segments.add(cursorStart to cursorEnd)
        }
        return segments
    }

    val items =
        shown.flatMap { course ->
            val overlappingCourses =
                findOverlappingCourses(
                    course = course,
                    dayCourses = dayCourses,
                    week = page,
                    includeNonCurrent = showNonCurrentWeek
                )
            val isCurrentWeekCourse = page != 0 && isCourseActiveInWeek(course, page)
            val visual =
                if (isCurrentWeekCourse) {
                    CourseVisual(
                        cardColor = pickCourseColor(course.name, isDarkMode),
                        titleColor = pickCourseTitleColor(course.name, isDarkMode),
                        subColor = pickCourseSubColor(course.name, isDarkMode)
                    )
                } else {
                    CourseVisual(
                        cardColor = mutedCourseColor,
                        titleColor = mutedTitleColor,
                        subColor = mutedSubColor
                    )
                }
            val overlapCount = overlappingCourses.size
            val badgeCount = if (overlapCount > 1) overlapCount else null
            splitCourseSections(course).map { (segStart, segEnd) ->
                val segCount = (segEnd - segStart + 1).coerceAtLeast(0)
                val topOffsetY = yForSection(segStart) + cellInset
                val cardHeight = (heightForSections(segCount) - cellInset * 2).coerceAtLeast(1.dp)
                val model =
                    CourseCardModel(
                        name = course.name,
                        location = course.location,
                        teacher = course.teacher,
                        topOffsetY = topOffsetY,
                        height = cardHeight,
                        badgeCount = badgeCount,
                        color = visual.cardColor,
                        titleColor = visual.titleColor,
                        subTextColor = visual.subColor,
                        cardAlpha = contentCardAlpha
                    )
                CourseRenderItem(model = model, overlaps = overlappingCourses)
            }
        }
    return DayRenderData(items = items)
}
