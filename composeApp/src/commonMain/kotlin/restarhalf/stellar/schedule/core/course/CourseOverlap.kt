package restarhalf.stellar.schedule.core.course

import restarhalf.stellar.schedule.domain.model.Course

fun isOverlapping(firstStart: Int, firstEnd: Int, secondStart: Int, secondEnd: Int): Boolean {
    return firstStart < secondEnd && secondStart < firstEnd
}

fun hasOverlapWith(course: Course, others: List<Course>): Boolean {
    val courseStart = course.startSection
    val courseEnd = course.startSection + course.sectionCount
    return others.any { other ->
        val otherStart = other.startSection
        val otherEnd = other.startSection + other.sectionCount
        isOverlapping(courseStart, courseEnd, otherStart, otherEnd)
    }
}

fun findOverlappingCourses(
    course: Course,
    dayCourses: List<Course>,
    week: Int,
    includeNonCurrent: Boolean
): List<Course> {
    val courseStart = course.startSection
    val courseEnd = course.startSection + course.sectionCount
    val scope =
        if (includeNonCurrent) dayCourses else dayCourses.filter { isCourseActiveInWeek(it, week) }
    return scope
        .filter { other ->
            val otherStart = other.startSection
            val otherEnd = other.startSection + other.sectionCount
            courseStart < otherEnd && otherStart < courseEnd
        }
        .sortedWith(
            compareByDescending<Course> { isCourseActiveInWeek(it, week) }
                .thenBy { it.startSection }
                .thenBy { it.sectionCount }
                .thenBy { it.name })
}

fun filterNonOverlapping(courses: List<Course>): List<Course> {
    val sorted =
        courses.sortedWith(compareBy({ it.startSection }, { it.sectionCount }, { it.name }))
    val result = ArrayList<Course>()

    for (course in sorted) {
        if (result.isEmpty()) {
            result.add(course)
            continue
        }
        val lastCourse = result.last()
        val lastEnd = lastCourse.startSection + lastCourse.sectionCount
        val courseStart = course.startSection
        if (courseStart < lastEnd) continue
        result.add(course)
    }
    return result
}

