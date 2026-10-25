package restarhalf.stellar.schedule.ui.viewmodel

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
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementCachePort
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


class AnnouncementViewModelTest {

    private val cache = mock<AnnouncementCachePort>(MockMode.autofill) {
        every { getCacheTtlMs() } returns 10L * 60L * 1000L
    }
    private val port = mock<AnnouncementPort>()

    private fun makeViewModel() = AnnouncementViewModel(
        fetchAnnouncements = FetchAnnouncementsUseCase(port, cache),
        markAnnouncementsRead = MarkAnnouncementsReadUseCase(cache),
        fetchAdConfig = FetchAdConfigUseCase(port),
        fetchAnnouncement = FetchAnnouncementUseCase(port),
    )

    
    private fun installTestMain(testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    }

    
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

        assertEquals(1, after.unreadCount)
    }
}
