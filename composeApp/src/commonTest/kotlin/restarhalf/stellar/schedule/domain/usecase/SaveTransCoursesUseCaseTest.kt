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

        val overrides =
            listOf(
                course("高数", weeks = listOf(1), originRemoteKey = "k1", targetWeek = 1),
                course("高数", weeks = listOf(2), originRemoteKey = "k1", targetWeek = 2),
            )

        assertTrue(useCase(origin, overrides))
        verifySuspend { repository.replaceCourses(emptyList(), overrides) }
    }

    @Test
    fun `先删除同源且周次重叠的旧调课`() = runTest {
        val origin = course("高数", weeks = listOf(1, 2, 3), type = 0, remoteKey = "k1")
        val oldWeek1 = course("高数", weeks = listOf(1), originRemoteKey = "k1", targetWeek = 1, id = 11)
        val oldWeek5 = course("高数", weeks = listOf(5), originRemoteKey = "k1", targetWeek = 5, id = 12)
        val otherCourse = course("英语", weeks = listOf(1), originRemoteKey = "k2", targetWeek = 1, id = 13)
        everySuspend { repository.getAllCoursesOnce() } returns
            listOf(origin, oldWeek1, oldWeek5, otherCourse)

        val overrides =
            listOf(course("高数", weeks = listOf(1), originRemoteKey = "k1", targetWeek = 1))

        assertTrue(useCase(origin, overrides))

        // 旧记录和插入在同一个事务里完成，不单独调用 deleteCourse
        verifySuspend { repository.replaceCourses(listOf(oldWeek1), overrides) }
        verifySuspend(dev.mokkery.verify.VerifyMode.exactly(0)) { repository.deleteCourse(any()) }
    }

    @Test
    fun `换到已被旧记录占用的目标周时删掉旧记录`() = runTest {
        val origin = course("高数", weeks = listOf(1, 2, 3, 4), type = 0, remoteKey = "k1")
        // 上次第 3 周换到第 7 周
        val oldMove =
            course("高数", weeks = listOf(3), originRemoteKey = "k1", targetWeek = 7, id = 11)
        everySuspend { repository.getAllCoursesOnce() } returns listOf(origin, oldMove)

        // 这次第 4 周换到第 7 周
        val overrides =
            listOf(course("高数", weeks = listOf(4), originRemoteKey = "k1", targetWeek = 7))

        assertTrue(useCase(origin, overrides))
        verifySuspend { repository.replaceCourses(listOf(oldMove), overrides) }
    }

    @Test
    fun `取消的周会清掉旧调课记录`() = runTest {
        val origin = course("高数", weeks = listOf(1, 2, 3), type = 0, remoteKey = "k1")
        val oldWeek2 =
            course("高数", weeks = listOf(2), originRemoteKey = "k1", targetWeek = 2, id = 11)
        val oldWeek3 =
            course("高数", weeks = listOf(3), originRemoteKey = "k1", targetWeek = 3, id = 12)
        everySuspend { repository.getAllCoursesOnce() } returns
            listOf(origin, oldWeek2, oldWeek3)

        val overrides =
            listOf(course("高数", weeks = listOf(1), originRemoteKey = "k1", targetWeek = 1))

        assertTrue(useCase(origin, overrides, clearedWeeks = listOf(2)))
        verifySuspend { repository.replaceCourses(listOf(oldWeek2), overrides) }
    }

    @Test
    fun `只清周次不插新记录也能成功`() = runTest {
        val origin = course("高数", weeks = listOf(1, 2), type = 0, remoteKey = "k1")
        val oldWeek2 =
            course("高数", weeks = listOf(2), originRemoteKey = "k1", targetWeek = 2, id = 11)
        everySuspend { repository.getAllCoursesOnce() } returns listOf(origin, oldWeek2)

        assertTrue(useCase(origin, emptyList(), clearedWeeks = listOf(2)))
        verifySuspend { repository.replaceCourses(listOf(oldWeek2), emptyList()) }
    }

    @Test
    fun `空列表直接返回false`() = runTest {
        val origin = course("高数", weeks = listOf(1), type = 0, remoteKey = "k1")
        assertFalse(useCase(origin, emptyList()))
        verifySuspend(dev.mokkery.verify.VerifyMode.exactly(0)) { repository.replaceCourses(any(), any()) }
    }

    @Test
    fun `保存异常返回false`() = runTest {
        val origin = course("高数", weeks = listOf(1), type = 0, remoteKey = "k1")
        everySuspend { repository.getAllCoursesOnce() } returns listOf(origin)
        everySuspend { repository.replaceCourses(any(), any()) } throws RuntimeException("db down")

        val overrides =
            listOf(course("高数", weeks = listOf(1), originRemoteKey = "k1", targetWeek = 1))

        assertFalse(useCase(origin, overrides))
    }

    @Test
    fun `对调课记录再调课时按最初的课归属`() = runTest {
        val origin = course("高数", weeks = listOf(1, 2), type = 0, remoteKey = "k1")
        val previousOverride =
            course("高数", weeks = listOf(2), originRemoteKey = "k1", targetWeek = 2, id = 11)
        everySuspend { repository.getAllCoursesOnce() } returns
            listOf(origin, previousOverride)

        val overrides =
            listOf(course("高数", weeks = listOf(2), originRemoteKey = "k1", targetWeek = 2))

        assertTrue(useCase(previousOverride, overrides))
        verifySuspend { repository.replaceCourses(listOf(previousOverride), overrides) }
    }
}
