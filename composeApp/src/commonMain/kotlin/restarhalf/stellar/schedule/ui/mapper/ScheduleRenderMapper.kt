package restarhalf.stellar.schedule.ui.mapper

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import restarhalf.stellar.schedule.core.course.filterNonOverlapping
import restarhalf.stellar.schedule.core.course.findOverlappingCourses
import restarhalf.stellar.schedule.core.course.hasOverlapWith
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.ui.theme.pickCourseColor
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import restarhalf.stellar.schedule.ui.theme.pickCourseTitleColor


private data class CourseVisual(val cardColor: Color, val titleColor: Color, val subColor: Color)


@Stable
data class DayRenderData(val items: ImmutableList<CourseRenderItem> = persistentListOf())


@Stable
data class CourseRenderItem(
    val model: CourseCardModel,
    val overlaps: ImmutableList<Course> = persistentListOf(),
)


@Stable
data class CourseCardModel(
    
    val name: String,
    
    val location: String,
    
    val teacher: String,
    
    val topOffsetY: Dp,
    
    val height: Dp,
    
    val badgeCount: Int?,
    
    val color: Color,
    
    val titleColor: Color,
    
    val subTextColor: Color,
    
    val cardAlpha: Float,
)

private fun splitCourseSections(course: Course): List<Pair<Int, Int>> {
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
): DayRenderData {

    val shown =
        if (page == 0) {
            filterNonOverlapping(dayCourses)
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
            val overlapsImmutable = overlappingCourses.toPersistentList()
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
                        cardAlpha = 1f
                    )
                CourseRenderItem(model = model, overlaps = overlapsImmutable)
            }
        }
    return DayRenderData(items = items.toPersistentList())
}
