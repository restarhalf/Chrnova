package restarhalf.stellar.schedule.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildHomeHeaderUiUseCaseTest {

    private val greeting = BuildHomeGreetingUseCase()
    private val useCase = BuildHomeHeaderUiUseCase(greeting)

    @Test
    fun `日期标签透传`() {
        val ui = useCase("6月13日 星期六", courseCount = 2, hasFirstClass = false, dayOfWeekCount = 1)
        assertEquals("6月13日 星期六", ui.dateLabel)
    }

    @Test
    fun `问候语来自候选池`() {
        val ui = useCase("2026年9月6日", courseCount = 2, hasFirstClass = true, dayOfWeekCount = 4)
        val candidates = greeting.candidates(courseCount = 2, hasFirstClass = true, dayOfWeekCount = 4)
        assertTrue(candidates.contains(ui.greeting), "问候语应来自候选池: ${ui.greeting}")
    }
}
