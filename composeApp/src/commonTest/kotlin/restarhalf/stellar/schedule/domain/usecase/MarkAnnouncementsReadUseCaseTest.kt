package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementCachePort
import kotlin.test.Test

class MarkAnnouncementsReadUseCaseTest {

    private val store = mock<AnnouncementCachePort>(MockMode.autofill)
    private val useCase = MarkAnnouncementsReadUseCase(store)

    @Test
    fun `标记已读写入公告 id`() {
        val announcement = Announcement(id = "a", createdAt = 500L, updatedAt = 700L)

        useCase(announcement)

        verify { store.addReadId("a") }
    }
}
