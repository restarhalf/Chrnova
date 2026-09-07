package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransCourseUseCaseTest {

    private val useCase = TransCourseUseCase()

    private fun course(
        name: String,
        dayOfWeek: Int,
        startSection: Int,
        sectionCount: Int,
        weeks: List<Int> = listOf(1, 2, 3, 4),
        type: Int = 0,
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
        type = type,
        remoteKey = remoteKey,
    )

    @Test
    fun `调课生成override课程字段正确`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "新教室",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )
        val o = result.overrideCourse
        assertEquals(0, o.id)
        assertEquals(2, o.type) // 调课类型
        assertEquals("k1", o.originRemoteKey)
        assertEquals("k1#override#2#4", o.remoteKey)
        assertEquals(4, o.targetWeek)
        assertEquals("新教室", o.location)
        assertEquals(3, o.dayOfWeek)
        assertEquals(5, o.startSection)
        assertEquals(2, o.sectionCount)
        assertEquals(listOf(2), o.weeks) // 调课记录标注原始周次
    }

    @Test
    fun `新教室为空时沿用原教室`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2)
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )
        assertEquals("旧教室", result.overrideCourse.location)
    }

    @Test
    fun `endSection小于startSection时节数至少为1`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2)
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "A",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 5,
        )
        assertEquals(1, result.overrideCourse.sectionCount)
    }

    @Test
    fun `目标周同天重叠节次检测为冲突`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val other = course("英语", dayOfWeek = 3, startSection = 6, sectionCount = 2, weeks = listOf(4))
        val result = useCase(
            allCourses = listOf(origin, other),
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "A",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )
        assertEquals(listOf("英语"), result.conflicts.map { it.name })
    }

    @Test
    fun `目标周无重叠则无冲突`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val other = course("英语", dayOfWeek = 3, startSection = 8, sectionCount = 2, weeks = listOf(4))
        val result = useCase(
            allCourses = listOf(origin, other),
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "A",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )
        assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `不同天的课程不冲突`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val other = course("英语", dayOfWeek = 2, startSection = 5, sectionCount = 2, weeks = listOf(4))
        val result = useCase(
            allCourses = listOf(origin, other),
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "A",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )
        assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `冲突检测基于目标周的生效课程`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        // 课程只在第3周，第4周不生效 → 不冲突
        val otherWeek3 = course("体育", dayOfWeek = 3, startSection = 5, sectionCount = 2, weeks = listOf(3))
        val result = useCase(
            allCourses = listOf(origin, otherWeek3),
            originCourse = origin,
            originWeek = 2,
            targetWeek = 4,
            newRoom = "A",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )
        assertTrue(result.conflicts.isEmpty())
    }
}
