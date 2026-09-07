package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.usecase.BuildHomeTodayScheduleUseCase.PeriodItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildHomePeriodRowUiUseCaseTest {

    private val useCase = BuildHomePeriodRowUiUseCase()

    private fun course(name: String, location: String = "教室A", teacher: String = "张三") = Course(
        name = name,
        location = location,
        teacher = teacher,
        dayOfWeek = 1,
        startSection = 1,
        sectionCount = 2,
        weeks = listOf(1),
        color = "#FF0000",
    )

    @Test
    fun `空闲时段显示空闲与节次`() {
        val ui = useCase(PeriodItem(1, 2, null), status = null)
        assertEquals("空闲", ui.primaryText)
        assertEquals("第1-2节", ui.secondaryText)
        assertTrue(ui.isPastOrEmpty)
    }

    @Test
    fun `有课时段拼出节次教室教师`() {
        val ui = useCase(PeriodItem(1, 2, course("高等数学")), status = "进行中")
        assertEquals("高等数学", ui.primaryText)
        assertEquals("第1-2节 | 教室A | 张三", ui.secondaryText)
        assertFalse(ui.isPastOrEmpty)
    }

    @Test
    fun `教室或教师为空时跳过对应字段`() {
        val ui = useCase(PeriodItem(3, 4, course("英语", location = "", teacher = "")), status = null)
        assertEquals("第3-4节", ui.secondaryText)
    }

    @Test
    fun `已结束状态标记为past`() {
        val ui = useCase(PeriodItem(1, 2, course("高等数学")), status = "已结束")
        assertTrue(ui.isPastOrEmpty)
    }

    @Test
    fun `进行中且有课不算past`() {
        val ui = useCase(PeriodItem(1, 2, course("高等数学")), status = "进行中")
        assertFalse(ui.isPastOrEmpty)
    }
}
