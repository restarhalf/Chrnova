package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.usecase.BuildHomeTodayScheduleUseCase.PeriodItem
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildHomePeriodSectionsUseCaseTest {

    private val useCase = BuildHomePeriodSectionsUseCase()

    private fun course(name: String) = Course(
        name = name,
        location = "教室",
        teacher = "老师",
        dayOfWeek = 1,
        startSection = 1,
        sectionCount = 2,
        weeks = listOf(1),
        color = "#FF0000",
    )

    @Test
    fun `输出三个时段且标题固定`() {
        val schedule = BuildHomeTodayScheduleUseCase.HomeTodaySchedule(
            activeWeek = 1,
            todayCourses = emptyList(),
            morningItems = listOf(PeriodItem(1, 4, null)),
            afternoonItems = listOf(PeriodItem(5, 8, null)),
            eveningItems = listOf(PeriodItem(9, 12, null)),
            hasFirstClass = false,
        )
        val sections = useCase(schedule)
        assertEquals(listOf("上午课程", "下午课程", "晚上课程"), sections.map { it.title })
        assertEquals(listOf(PeriodItem(1, 4, null), PeriodItem(5, 8, null), PeriodItem(9, 12, null)), sections.map { it.items.first() })
    }

    @Test
    fun `条目引用与输入一致`() {
        val morning = listOf(PeriodItem(1, 2, course("高数")))
        val schedule = BuildHomeTodayScheduleUseCase.HomeTodaySchedule(
            activeWeek = 3,
            todayCourses = emptyList(),
            morningItems = morning,
            afternoonItems = emptyList(),
            eveningItems = emptyList(),
            hasFirstClass = true,
        )
        val sections = useCase(schedule)
        assertEquals(morning, sections[0].items)
        assertEquals(0, sections[1].items.size)
        assertEquals(0, sections[2].items.size)
    }
}
