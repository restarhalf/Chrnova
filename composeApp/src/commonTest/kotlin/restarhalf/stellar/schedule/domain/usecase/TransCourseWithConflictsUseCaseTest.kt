package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransCourseWithConflictsUseCaseTest {

    private val repository = mock<CourseRepository>(MockMode.autofill)
    private val useCase = TransCourseWithConflictsUseCase(repository, TransCourseUseCase())

    private fun course(
        name: String,
        dayOfWeek: Int,
        startSection: Int,
        sectionCount: Int,
        weeks: List<Int> = listOf(1, 2, 3, 4),
        remoteKey: String = "",
    ) = Course(
        name = name,
        location = "旧教室",
        teacher = "老师",
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        sectionCount = sectionCount,
        weeks = weeks,
        color = "#FF0000",
        remoteKey = remoteKey,
    )

    @Test
    fun `预获取课程列表重载透传调课结果`() = kotlinx.coroutines.test.runTest {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val other = course("英语", dayOfWeek = 3, startSection = 6, sectionCount = 2, weeks = listOf(4))
        val result = useCase(
            allCourses = listOf(origin, other),
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "新教室",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )
        assertEquals(2, result.overrideCourse.type)
        assertEquals(listOf("英语"), result.conflicts.map { it.name })
    }

    @Test
    fun `自动获取课程列表重载调用repository`() = kotlinx.coroutines.test.runTest {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val other = course("英语", dayOfWeek = 3, startSection = 5, sectionCount = 2, weeks = listOf(4))
        everySuspend { repository.getAllCoursesOnce() } returns listOf(origin, other)

        val result = useCase(
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "新教室",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )
        assertEquals("新教室", result.overrideCourse.location)
        assertEquals(listOf("英语"), result.conflicts.map { it.name })
    }

    @Test
    fun `无冲突时返回空列表`() = kotlinx.coroutines.test.runTest {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        everySuspend { repository.getAllCoursesOnce() } returns listOf(origin)

        val result = useCase(
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "B",
            dayOfWeek = 5,
            startSection = 1,
            endSection = 2,
        )
        assertTrue(result.conflicts.isEmpty())
    }
}
