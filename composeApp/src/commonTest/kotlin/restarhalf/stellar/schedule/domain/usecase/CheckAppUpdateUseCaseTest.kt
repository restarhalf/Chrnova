package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CheckAppUpdateUseCaseTest {

    private val appUpdate = mock<AppUpdatePort>(MockMode.autofill)

    private val info = AppUpdateInfo(
        latestVersion = "1.2.0",
        releasePageUrl = "https://example.com/release",
        downloadUrl = "https://example.com/app.apk",
        changelog = "修复若干问题",
    )

    @Test
    fun `透传版本号与灰度标识并返回结果`() = runTest {
        var capturedGrayUid: String? = null
        everySuspend { appUpdate.check(any(), any()) } calls { (version: String, grayUid: String?) ->
            capturedGrayUid = grayUid
            info
        }
        val useCase = CheckAppUpdateUseCase(appUpdate, getGrayUid = { "gray-hash" })

        val result = useCase("1.0.0")

        assertEquals("1.2.0", result?.latestVersion)
        assertEquals("gray-hash", capturedGrayUid)
        verifySuspend(VerifyMode.exactly(1)) { appUpdate.check("1.0.0", any()) }
    }

    @Test
    fun `默认无灰度标识且可返回null`() = runTest {
        everySuspend { appUpdate.check(any(), any()) } returns null
        val useCase = CheckAppUpdateUseCase(appUpdate)

        val result = useCase("1.0.0")

        assertNull(result)
        verifySuspend(VerifyMode.exactly(1)) { appUpdate.check("1.0.0", null) }
    }
}
