package restarhalf.stellar.schedule.ui.viewmodel

import com.russhwolf.settings.ObservableSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import restarhalf.stellar.schedule.domain.usecase.FetchAdConfigUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementsUseCase
import restarhalf.stellar.schedule.domain.usecase.MarkAnnouncementsReadUseCase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * AnnouncementViewModel 单元测试（Mokkery）
 *
 * ViewModel 与四个 UseCase 均为 final class（Mokkery 3.x 仅支持接口 / open 类），
 * 因此保持它们为真实实现，在端口层 [AnnouncementPort] 与 [ObservableSettings]
 * 打 mock，从入口到状态流全链路验证。
 *
 * viewModelScope 绑定 Dispatchers.Main（经 TestMainDispatcher 包装后落到后台线程），
 * 挂起任务与测试线程是并发的，因此断言前统一用 [awaitState] 真实时间轮询等
 * 状态就绪（终态 = loaded 或 error 非空），避免与协程执行产生竞态。
 */
class AnnouncementViewModelTest {

    private val settings = mock<ObservableSettings>(MockMode.autofill) {
        every { putString(any(), any()) } returns Unit
        every { putLong(any(), any()) } returns Unit
    }
    private val port = mock<AnnouncementPort>()

    private fun makeViewModel() = AnnouncementViewModel(
        fetchAnnouncements = FetchAnnouncementsUseCase(port, AnnouncementStore(settings)),
        markAnnouncementsRead = MarkAnnouncementsReadUseCase(AnnouncementStore(settings)),
        fetchAdConfig = FetchAdConfigUseCase(port),
        fetchAnnouncement = FetchAnnouncementUseCase(port),
    )

    /** viewModelScope 依赖 Main dispatcher；绑定 runTest 的 scheduler 使协程可被驱动 */
    private fun installTestMain(testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    }

    /**
     * 等待 ViewModel 到达指定状态。
     * viewModelScope 经 TestMainDispatcher 包装后落到后台线程执行，StateFlow.value
     * 是 volatile 读，用真实时间短轮询与协程同步，避免断言竞态。
     */
    private suspend fun awaitState(
        vm: AnnouncementViewModel,
        timeoutMs: Long = 5_000,
        predicate: (AnnouncementViewModel.AnnouncementUiState) -> Boolean,
    ): AnnouncementViewModel.AnnouncementUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        var state = vm.uiState.value
        while (!predicate(state) && Clock.System.now().toEpochMilliseconds() < deadline) {
            withContext(Dispatchers.Default) { delay(10) }
            state = vm.uiState.value
        }
        return state
    }

    /** 等待首次加载到终态（成功或失败） */
    private suspend fun awaitSettled(vm: AnnouncementViewModel): AnnouncementViewModel.AnnouncementUiState =
        awaitState(vm) { (it.loaded && !it.loading) || it.error != null }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始化加载公告列表与广告配置`() = runTest {
        installTestMain(testScheduler)
        val announcements = listOf(
            Announcement(id = "1", title = "公告一", createdAt = 1000L),
            Announcement(id = "2", title = "公告二", createdAt = 2000L),
        )
        everySuspend { port.listAnnouncements() } returns announcements
        everySuspend { port.getAdConfig() } returns null

        val vm = makeViewModel()
        val state = awaitSettled(vm)

        assertTrue(state.loaded, "应处于加载完成状态")
        assertFalse(state.loading)
        assertNull(state.error)
        assertEquals(listOf("公告一", "公告二"), state.announcements.map { it.title })
    }

    @Test
    fun `加载失败时置错误信息`() = runTest {
        installTestMain(testScheduler)
        everySuspend { port.listAnnouncements() } throws RuntimeException("网络开小差")
        everySuspend { port.getAdConfig() } returns null

        val vm = makeViewModel()
        val state = awaitSettled(vm)

        assertFalse(state.loaded)
        assertEquals("网络开小差", state.error)
    }

    @Test
    fun `selectAnnouncement 命中本地列表缓存时不打详情接口`() = runTest {
        installTestMain(testScheduler)
        val cached = Announcement(id = "1", title = "公告一", createdAt = 1000L)
        everySuspend { port.listAnnouncements() } returns listOf(cached)
        everySuspend { port.getAdConfig() } returns null

        val vm = makeViewModel()
        awaitSettled(vm)
        vm.selectAnnouncement("1")

        assertEquals(cached, vm.uiState.value.selectedAnnouncement)
        verifySuspend(VerifyMode.not) { port.getAnnouncement(any()) }
    }

    @Test
    fun `selectAnnouncement 未命中时走详情接口`() = runTest {
        installTestMain(testScheduler)
        val adAnnouncement = Announcement(id = "ad-1", title = "广告公告", createdAt = 3000L)
        everySuspend { port.listAnnouncements() } returns emptyList()
        everySuspend { port.getAdConfig() } returns null
        everySuspend { port.getAnnouncement("ad-1") } returns adAnnouncement

        val vm = makeViewModel()
        awaitSettled(vm)
        vm.selectAnnouncement("ad-1")
        val state = awaitState(vm) { it.selectedAnnouncement != null || it.error != null }

        assertEquals(adAnnouncement, state.selectedAnnouncement)
        verifySuspend { port.getAnnouncement("ad-1") }
    }

    @Test
    fun `markAnnouncementRead 推进阅读时间并重算未读数`() = runTest {
        installTestMain(testScheduler)
        val older = Announcement(id = "1", title = "较早", createdAt = 1000L)
        val newer = Announcement(id = "2", title = "较晚", createdAt = 5000L)
        everySuspend { port.listAnnouncements() } returns listOf(older, newer)
        everySuspend { port.getAdConfig() } returns null

        val vm = makeViewModel()
        val state = awaitSettled(vm)
        assertEquals(2, state.unreadCount)

        vm.markAnnouncementRead(older)

        val after = vm.uiState.value
        assertEquals(1_000_000L, after.lastReadAtMs)
        // newer(5000s) > lastReadAtMs(1000s) → 仍有一条未读
        assertEquals(1, after.unreadCount)
    }
}
