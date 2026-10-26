package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveTransCoursesUseCaseTest {

    private val repository = mock<CourseRepository>(MockMode.autofill)
    private val useCase = SaveTransCoursesUseCase(repository)

    private fun course(
        name: String,
        weeks: List<Int>,
        type: Int = 2,
        remoteKey: String = "",
        originRemoteKey: String? = null,
        targetWeek: Int = 0,
        id: Long = 0,
    ) = Course(
        id = id,
        name = name,
        location = "教室",
        teacher = "老师",
        dayOfWeek = 1,
        startSection = 1,
        sectionCount = 2,
        weeks = weeks,
        color = "#FF0000",
        type = type,
        remoteKey = remoteKey,
        originRemoteKey = originRemoteKey,
        targetWeek = targetWeek,
    )

    @Test
    fun `批量插入所有调课记录`() = runTest {
        val origin = course("高数", weeks = listOf(1, 2), type = 0, remoteKey = "k1")
        everySuspend { repository.getAllCoursesOnce() } returns listOf(origin)
        everySuspend { repository.insertCourses(any()) } returns listOf(1L, 2L)

        val overrides =
            listOf(
                course("高数", weeks = listOf(1), originRemoteKey = "k1", targetWeek = 1),
                course("高数", weeks = listOf(2), originRemoteKey = "k1", targetWeek = 2),
            )

        assertTrue(useCase(origin, overrides))
        verifySuspend { repository.insertCourses(overrides) }
    }

    @Test
    fun `先删除同源且周次重叠的旧调课`() = runTest {
        val origin = course("高数", weeks = listOf(1, 2, 3), type = 0, remoteKey = "k1")
        val oldWeek1 = course("高数", weeks = listOf(1), originRemoteKey = "k1", targetWeek = 1, id = 11)
        val oldWeek5 = course("高数", weeks = listOf(5), originRemoteKey = "k1", targetWeek = 5, id = 12)
        val otherCourse = course("英语", weeks = listOf(1), originRemoteKey = "k2", targetWeek = 1, id = 13)
        everySuspend { repository.getAllCoursesOnce() } returns
            listOf(origin, oldWeek1, oldWeek5, otherCourse)
        everySuspend { repository.insertCourses(any()) } returns listOf(1L)

        val overrides =
            listOf(course("高数", weeks = listOf(1), originRemoteKey = "k1", targetWeek = 1))

        assertTrue(useCase(origin, overrides))

        verifySuspend { repository.deleteCourse(oldWeek1) }
        verifySuspend(dev.mokkery.verify.VerifyMode.exactly(0)) { repository.deleteCourse(oldWeek5) }
        verifySuspend(dev.mokkery.verify.VerifyMode.exactly(0)) { repository.deleteCourse(otherCourse) }
    }

    @Test
    fun `空列表直接返回false`() = runTest {
        val origin = course("高数", weeks = listOf(1), type = 0, remoteKey = "k1")
        assertFalse(useCase(origin, emptyList()))
        verifySuspend(dev.mokkery.verify.VerifyMode.exactly(0)) { repository.insertCourses(any()) }
    }

    @Test
    fun `插入异常返回false`() = runTest {
        val origin = course("高数", weeks = listOf(1), type = 0, remoteKey = "k1")
        everySuspend { repository.getAllCoursesOnce() } returns listOf(origin)
        everySuspend { repository.insertCourses(any()) } throws RuntimeException("db down")

        val overrides =
            listOf(course("高数", weeks = listOf(1), originRemoteKey = "k1", targetWeek = 1))

        assertFalse(useCase(origin, overrides))
    }
}
