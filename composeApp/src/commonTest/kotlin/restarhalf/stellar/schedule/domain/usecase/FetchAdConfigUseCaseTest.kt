package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.AdConfig
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 广告位配置用例：无缓存策略，直接透传端口结果（后端未配置时为 null）。
 */
class FetchAdConfigUseCaseTest {

    private val port = mock<AnnouncementPort>()
    private val useCase = FetchAdConfigUseCase(port)

    @Test
    fun `有配置时透传 AdConfig`() = runTest {
        val config = AdConfig(imageUrl = "https://img/banner.png", targetUrl = "https://x", announcementId = "a-9")
        everySuspend { port.getAdConfig() } returns config

        assertEquals(config, useCase())
        verifySuspend { port.getAdConfig() }
    }

    @Test
    fun `未配置时透传 null`() = runTest {
        everySuspend { port.getAdConfig() } returns null

        assertEquals(null, useCase())
    }

    @Test
    fun `端口异常直接透传`() = runTest {
        everySuspend { port.getAdConfig() } throws RuntimeException("网络开小差")

        assertFailsWith<RuntimeException> { useCase() }
    }
}
