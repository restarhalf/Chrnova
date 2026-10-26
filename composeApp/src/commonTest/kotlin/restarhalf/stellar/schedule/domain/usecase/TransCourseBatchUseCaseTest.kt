package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransCourseBatchUseCaseTest {

    private val useCase = TransCourseBatchUseCase(TransCourseUseCase())

    private fun course(
        name: String,
        dayOfWeek: Int,
        startSection: Int,
        sectionCount: Int,
        weeks: List<Int> = listOf(1, 2, 3, 4),
        type: Int = 0,
        remoteKey: String = "",
        id: Long = 0,
    ) = Course(
        id = id,
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
    fun `每个选中周各生成一条同周调课记录`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            weeks = listOf(1, 2, 3),
            newRoom = "新教室",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )

        assertEquals(listOf(1, 2, 3), result.overrideCourses.map { it.targetWeek })
        assertEquals(listOf(1, 2, 3), result.overrideCourses.map { it.weeks.single() })
        assertEquals(
            listOf("k1#override#1#1", "k1#override#2#2", "k1#override#3#3"),
            result.overrideCourses.map { it.remoteKey },
        )
        assertTrue(result.overrideCourses.all { it.type == 2 })
        assertTrue(result.overrideCourses.all { it.originRemoteKey == "k1" })
        assertTrue(result.overrideCourses.all { it.location == "新教室" })
        assertTrue(result.overrideCourses.all { it.id == 0L })
        assertTrue(result.weekConflicts.isEmpty())
    }

    @Test
    fun `周次去重并忽略非法周`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            weeks = listOf(3, 0, 3, -1, 2),
            newRoom = "",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )

        assertEquals(listOf(2, 3), result.overrideCourses.map { it.targetWeek })
    }

    @Test
    fun `空周次返回空结果`() {
        val origin = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            weeks = emptyList(),
            newRoom = "",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )

        assertTrue(result.overrideCourses.isEmpty())
        assertTrue(result.weekConflicts.isEmpty())
    }

    @Test
    fun `按周聚合并跳过自身`() {
        val origin = course(
            name = "高数",
            dayOfWeek = 1,
            startSection = 1,
            sectionCount = 2,
            weeks = listOf(1, 2, 3, 4),
            remoteKey = "k1",
        )
        val physics = course(
            name = "大学物理",
            dayOfWeek = 3,
            startSection = 5,
            sectionCount = 2,
            weeks = listOf(2, 4),
        )

        val result = useCase(
            allCourses = listOf(origin, physics),
            originCourse = origin,
            weeks = listOf(1, 2, 3, 4),
            newRoom = "新教室",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )

        assertEquals(listOf(2, 4), result.weekConflicts.map { it.week })
        assertTrue(result.weekConflicts.all { week -> week.courses.map { it.name } == listOf("大学物理") })
        assertEquals(4, result.overrideCourses.size)
    }

    @Test
    fun `调到原时间地点的周次不生成记录`() {
        val origin = course(
            name = "高数",
            dayOfWeek = 1,
            startSection = 1,
            sectionCount = 2,
            weeks = listOf(1, 2, 3),
            remoteKey = "k1",
        )
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            weeks = listOf(1, 2, 3),
            newRoom = "",
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
        )

        assertTrue(result.overrideCourses.isEmpty())
        assertEquals(listOf(1, 2, 3), result.noOpWeeks)
        assertFalse(result.hasChanges)
    }

    @Test
    fun `只改时间的周次正常生成记录`() {
        val origin = course(
            name = "高数",
            dayOfWeek = 1,
            startSection = 1,
            sectionCount = 2,
            weeks = listOf(1, 2, 3),
            remoteKey = "k1",
        )
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            weeks = listOf(1, 2, 3),
            newRoom = "",
            dayOfWeek = 3,
            startSection = 5,
            endSection = 6,
        )

        assertEquals(listOf(1, 2, 3), result.overrideCourses.map { it.targetWeek })
        assertTrue(result.noOpWeeks.isEmpty())
        assertTrue(result.hasChanges)
    }

    @Test
    fun `取消的原周与新增的周配对成换周`() {
        val origin = course(
            name = "高数",
            dayOfWeek = 1,
            startSection = 1,
            sectionCount = 2,
            weeks = listOf(1, 2, 3, 4),
            remoteKey = "k1",
        )
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            weeks = listOf(1, 2, 3, 7),
            newRoom = "新教室",
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
        )

        assertEquals(listOf(1, 2, 3, 7), result.overrideCourses.map { it.targetWeek })
        val moved = result.overrideCourses.last()
        assertEquals(listOf(4), moved.weeks)
        assertEquals(7, moved.targetWeek)
        assertEquals(4, result.overrideCourses.size)
    }

    @Test
    fun `选中的周不超过原周数时不会多出记录`() {
        val origin = course(
            name = "高数",
            dayOfWeek = 1,
            startSection = 1,
            sectionCount = 2,
            weeks = listOf(1, 2, 3, 4),
            remoteKey = "k1",
        )
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            weeks = listOf(6, 7),
            newRoom = "新教室",
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
        )

        assertEquals(listOf(6, 7), result.overrideCourses.map { it.targetWeek })
        assertEquals(listOf(listOf(1), listOf(2)), result.overrideCourses.map { it.weeks })
    }

    @Test
    fun `调课不与自己冲突`() {
        val origin = course(
            name = "高数",
            dayOfWeek = 1,
            startSection = 1,
            sectionCount = 2,
            weeks = listOf(1, 2),
            remoteKey = "k1",
        )
        val result = useCase(
            allCourses = listOf(origin),
            originCourse = origin,
            weeks = listOf(1, 2),
            newRoom = "",
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
        )
        assertTrue(result.weekConflicts.isEmpty())
    }
}
