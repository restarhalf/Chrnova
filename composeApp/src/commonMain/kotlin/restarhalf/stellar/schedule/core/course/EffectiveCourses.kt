package restarhalf.stellar.schedule.core.course

import restarhalf.stellar.schedule.domain.model.Course

fun isCourseActiveInWeek(course: Course, week: Int): Boolean {
    if (week <= 0) return true
    return if (course.type == 2) {
        course.targetWeek == week
    } else {
        course.weeks.contains(week)
    }
}

/** 提取去重的普通课程名称列表（type=0） */
fun buildCourseNames(courses: List<Course>): List<String> {
    return courses.filter { it.type == 0 }.map { it.name }.distinct()
}

fun effectiveCoursesForWeek(all: List<Course>, week: Int): List<Course> {
    if (week <= 0) return all

    val overridesThatHideOriginal = all.filter { it.type == 2 && it.weeks.contains(week) }

    val coveredKeys = overridesThatHideOriginal.mapNotNull { it.originRemoteKey }.toHashSet()

    val withoutCoveredOriginals =
        if (coveredKeys.isEmpty()) {
            all
        } else {
            all.filterNot { it.type == 0 && it.remoteKey.isNotBlank() && coveredKeys.contains(it.remoteKey) }
        }

    return withoutCoveredOriginals.filterNot { it.type == 2 && it.targetWeek != week }
}


