package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementCachePort
import kotlin.test.Test


class MarkAnnouncementsReadUseCaseTest {

    private val store = mock<AnnouncementCachePort>(MockMode.autofill)
    private val useCase = MarkAnnouncementsReadUseCase(store)

    @Test
    fun `标记已读推进最后阅读时间到内容变化时间`() {
        every { store.getLastReadAtMs() } returns 0L // 从未阅读
        val announcement = Announcement(id = "a", createdAt = 500L, updatedAt = 700L)

        useCase(announcement)

        verify { store.setLastReadAtMs(700_000L) }
    }

    @Test
    fun `阅读时间不会后退`() {
        every { store.getLastReadAtMs() } returns 999_999L // 已读到更晚
        val announcement = Announcement(id = "a", createdAt = 500L, updatedAt = 0L)

        useCase(announcement)

        verify(VerifyMode.not) { store.setLastReadAtMs(any()) }
    }
}
