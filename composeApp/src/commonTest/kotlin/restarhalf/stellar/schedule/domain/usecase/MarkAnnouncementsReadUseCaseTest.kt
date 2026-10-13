package restarhalf.stellar.schedule.domain.usecase

import com.russhwolf.settings.ObservableSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.domain.model.Announcement
import kotlin.test.Test


class MarkAnnouncementsReadUseCaseTest {

    private val settings = mock<ObservableSettings>(MockMode.autofill) {
        every { putLong(any(), any()) } returns Unit
    }
    private val store = AnnouncementStore(settings)
    private val useCase = MarkAnnouncementsReadUseCase(store)

    @Test
    fun `标记已读推进最后阅读时间到内容变化时间`() {
        every { settings.getLong(any(), any()) } returns 0L // 从未阅读
        val announcement = Announcement(id = "a", createdAt = 500L, updatedAt = 700L)

        useCase(announcement)


        verify { settings.putLong(any(), 700_000L) }
    }

    @Test
    fun `阅读时间不会后退`() {
        every { settings.getLong(any(), any()) } returns 999_999L // 已读到更晚
        val announcement = Announcement(id = "a", createdAt = 500L, updatedAt = 0L)

        useCase(announcement)

        verify(VerifyMode.not) { settings.putLong(any(), any()) }
    }
}
