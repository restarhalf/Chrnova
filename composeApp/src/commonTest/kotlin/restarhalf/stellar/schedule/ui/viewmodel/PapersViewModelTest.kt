package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.Paper
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * PapersViewModel 单元测试。
 *
 * PapersPort/SettingsPort 均为接口可直接 mock；VerifyGitHubStarUseCase 是
 * final class，用真实实例 + 同一批 mock 端口构造。
 * StarVerificationHolder 在 VM 构造 init 中读取 getStarVerified()，setUp 里 stub 为 false。
 * uiState 是裸 MutableStateFlow，无需订阅即可读 value。
 */
class PapersViewModelTest {

    private val papersPort = mock<PapersPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    /** 类级持有 Main dispatcher，测试中可主动 drain 其内部任务队列 */
    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun makeViewModel() = PapersViewModel(
        papersPort = papersPort,
        settings = settings,
        verifyGitHubStar = VerifyGitHubStarUseCase(papersPort, settings),
    )

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        every { settings.getStarVerified() } returns false
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun paper(id: String, title: String) = Paper(id = id, title = title)

    @Test
    fun `loadPapers成功更新列表并复位loading`() = runTest {
        everySuspend { papersPort.listPapers() } returns listOf(paper("1", "高等数学"))

        val vm = makeViewModel()
        vm.loadPapers()
        advanceMain()

        assertFalse(vm.uiState.value.loading)
        assertEquals(null, vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.allPapers.size)
        assertEquals("高等数学", vm.uiState.value.allPapers[0].title)
    }

    @Test
    fun `loadPapers失败置error并复位loading`() = runTest {
        everySuspend { papersPort.listPapers() } throws RuntimeException("服务错误")

        val vm = makeViewModel()
        vm.loadPapers()
        advanceMain()

        assertFalse(vm.uiState.value.loading)
        assertEquals("服务错误", vm.uiState.value.error)
    }

    @Test
    fun `loadFolders成功更新folders列表`() = runTest {
        everySuspend { papersPort.getFolders() } returns listOf("2024春", "2025春")

        val vm = makeViewModel()
        vm.loadFolders()
        advanceMain()

        assertEquals(listOf("2024春", "2025春"), vm.uiState.value.folders)
    }

    @Test
    fun `loadFolders失败静默不影响error`() = runTest {
        everySuspend { papersPort.getFolders() } throws RuntimeException("网络错误")

        val vm = makeViewModel()
        vm.loadFolders()
        advanceMain()

        assertEquals(null, vm.uiState.value.error)
        assertEquals(0, vm.uiState.value.folders.size)
    }

    @Test
    fun `loadPaperDetail成功设置selectedPaper`() = runTest {
        everySuspend { papersPort.getPaper("p1") } returns paper("p1", "试卷A")

        val vm = makeViewModel()
        vm.loadPaperDetail("p1")
        advanceMain()

        assertFalse(vm.uiState.value.loading)
        assertEquals("试卷A", vm.uiState.value.selectedPaper?.title)
        assertEquals(null, vm.uiState.value.downloadUrl)
    }

    @Test
    fun `loadPaperDetail清空上一次的downloadUrl`() = runTest {
        everySuspend { papersPort.getPaper("p1") } returns paper("p1", "试卷A")
        everySuspend { papersPort.downloadPaper("p0") } returns "/old/file.pdf"

        val vm = makeViewModel()
        vm.downloadPaper("p0")
        advanceMain()
        assertEquals("https://v4.gh-proxy.org//old/file.pdf", vm.uiState.value.downloadUrl)

        vm.loadPaperDetail("p1")
        advanceMain()

        assertEquals(null, vm.uiState.value.downloadUrl)
    }

    @Test
    fun `downloadPaper成功包装gh-proxy前缀`() = runTest {
        everySuspend { papersPort.downloadPaper("p1") } returns "/repo/file.pdf"

        val vm = makeViewModel()
        vm.downloadPaper("p1")
        advanceMain()

        assertFalse(vm.uiState.value.loading)
        assertEquals("https://v4.gh-proxy.org//repo/file.pdf", vm.uiState.value.downloadUrl)
    }

    @Test
    fun `consumeDownloadUrl置空下载链接`() = runTest {
        everySuspend { papersPort.downloadPaper("p1") } returns "/repo/file.pdf"

        val vm = makeViewModel()
        vm.downloadPaper("p1")
        advanceMain()

        vm.consumeDownloadUrl()

        assertEquals(null, vm.uiState.value.downloadUrl)
    }

    @Test
    fun `uploadPaper成功置successMessage并刷新列表`() = runTest {
        everySuspend {
            papersPort.uploadPaper(any(), any(), any(), any(), any())
        } returns paper("up1", "标题")
        everySuspend { papersPort.listPapers() } returns listOf(paper("1", "新试卷"))
        everySuspend { papersPort.getFolders() } returns listOf("新文件夹")

        val vm = makeViewModel()
        vm.uploadPaper(ByteArray(1), "a.pdf", "application/pdf", "标题", "文件夹")
        advanceMain()

        assertFalse(vm.uiState.value.uploading)
        assertEquals("上传成功", vm.uiState.value.successMessage)
        assertEquals(1, vm.uiState.value.allPapers.size)
        assertEquals(1, vm.uiState.value.folders.size)
    }

    @Test
    fun `uploadPaper失败置error并复位uploading`() = runTest {
        everySuspend {
            papersPort.uploadPaper(any(), any(), any(), any(), any())
        } throws RuntimeException("空间不足")

        val vm = makeViewModel()
        vm.uploadPaper(ByteArray(1), "a.pdf", "application/pdf", "标题", "文件夹")
        advanceMain()

        assertFalse(vm.uiState.value.uploading)
        assertEquals("空间不足", vm.uiState.value.error)
    }

    @Test
    fun `搜索query过滤papers列表`() = runTest {
        everySuspend {
            papersPort.listPapers()
        } returns listOf(paper("1", "高等数学"), paper("2", "大学英语"))

        val vm = makeViewModel()
        vm.loadPapers()
        advanceMain()
        assertEquals(2, vm.uiState.value.papers.size)

        vm.onSearchQueryChange("数学")

        assertEquals(1, vm.uiState.value.papers.size)
        assertEquals("高等数学", vm.uiState.value.papers[0].title)
    }

    @Test
    fun `空query时papers返回全部`() = runTest {
        everySuspend { papersPort.listPapers() } returns listOf(paper("1", "高等数学"))

        val vm = makeViewModel()
        vm.loadPapers()
        advanceMain()

        vm.onSearchQueryChange("")

        assertEquals(1, vm.uiState.value.papers.size)
    }
}
