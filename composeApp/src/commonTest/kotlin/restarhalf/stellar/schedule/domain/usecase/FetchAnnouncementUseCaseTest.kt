package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import dev.mokkery.verify
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 单条公告详情用例：纯透传 port.getAnnouncement(id)。
 * 文档语义：公开详情接口对 status='published' 与 'ad' 均可见，草稿 404。
 */
class FetchAnnouncementUseCaseTest {

    private val port = mock<AnnouncementPort>()
    private val useCase = FetchAnnouncementUseCase(port)

    @Test
    fun `透传 id 并返回端口结果`() = runTest {
        val expected = Announcement(id = "a-1", title = "详情公告", createdAt = 1000L)
        everySuspend { port.getAnnouncement("a-1") } returns expected

        val result = useCase("a-1")

        assertEquals(expected, result)
        verifySuspend { port.getAnnouncement("a-1") }
    }

    @Test
    fun `不同 id 分别透传对应结果`() = runTest {
        val published = Announcement(id = "pub", title = "已发布")
        val ad = Announcement(id = "ad", title = "广告位公告")
        everySuspend { port.getAnnouncement("pub") } returns published
        everySuspend { port.getAnnouncement("ad") } returns ad

        assertEquals("已发布", useCase("pub").title)
        assertEquals("广告位公告", useCase("ad").title)
        verifySuspend(VerifyMode.exactly(1)) { port.getAnnouncement("pub") }
        verifySuspend(VerifyMode.exactly(1)) { port.getAnnouncement("ad") }
    }

    @Test
    fun `端口异常直接透传`() = runTest {
        everySuspend { port.getAnnouncement(any()) } throws RuntimeException("草稿 404")

        assertFailsWith<RuntimeException> { useCase("draft-1") }
    }
}
