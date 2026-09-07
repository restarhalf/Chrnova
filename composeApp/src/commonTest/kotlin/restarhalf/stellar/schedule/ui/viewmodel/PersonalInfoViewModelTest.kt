package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.port.SettingsPort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PersonalInfoViewModel 单元测试。
 *
 * SettingsPort 是接口可直接 mock；用 calls 块维护可变存储状态，
 * 让 getter 读到 setter 写入的最新值，验证"写入 + reload"的完整闭环。
 * uiState 是裸 MutableStateFlow，无需订阅即可读 value。
 */
class PersonalInfoViewModelTest {

    private var avatarState: String? = null
    private var nicknameState: String? = null

    private val settingsPort = mock<SettingsPort>(MockMode.autofill)

    /** 类级持有 Main dispatcher，测试中可主动 drain 其内部任务队列 */
    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** stub 四个存储方法并构造 VM（init 块即触发 load，stub 必须先于构造） */
    private fun makeViewModel(): PersonalInfoViewModel {
        every { settingsPort.getUserAvatarUri() } calls { avatarState }
        every { settingsPort.getUserNickname() } calls { nicknameState }
        every { settingsPort.setUserAvatarUri(any()) } calls { (uri: String?) ->
            avatarState = uri
        }
        every { settingsPort.setUserNickname(any()) } calls { (nickname: String?) ->
            nicknameState = nickname
        }
        return PersonalInfoViewModel(settingsPort)
    }

    @Test
    fun `init加载已存储的用户信息`() = runTest {
        avatarState = "content://avatar"
        nicknameState = "小明"

        val vm = makeViewModel()
        advanceMain()

        assertEquals("content://avatar", vm.uiState.value.avatarUri)
        assertEquals("小明", vm.uiState.value.nickname)
    }

    @Test
    fun `init加载空存储时默认null`() = runTest {
        avatarState = null
        nicknameState = null

        val vm = makeViewModel()
        advanceMain()

        assertEquals(null, vm.uiState.value.avatarUri)
        assertEquals(null, vm.uiState.value.nickname)
    }

    @Test
    fun `saveAvatar透传存储并刷新uiState`() = runTest {
        avatarState = "content://old"

        val vm = makeViewModel()
        advanceMain()
        assertEquals("content://old", vm.uiState.value.avatarUri)

        vm.saveAvatar("content://new")
        advanceMain()

        verify(VerifyMode.exactly(1)) {
            settingsPort.setUserAvatarUri("content://new")
        }
        assertEquals("content://new", vm.uiState.value.avatarUri)
    }

    @Test
    fun `saveNickname透传存储并刷新uiState`() = runTest {
        nicknameState = "旧昵称"

        val vm = makeViewModel()
        advanceMain()
        assertEquals("旧昵称", vm.uiState.value.nickname)

        vm.saveNickname("新昵称")
        advanceMain()

        verify(VerifyMode.exactly(1)) {
            settingsPort.setUserNickname("新昵称")
        }
        assertEquals("新昵称", vm.uiState.value.nickname)
    }

    @Test
    fun `clearAvatar写入null并刷新uiState`() = runTest {
        avatarState = "content://old"

        val vm = makeViewModel()
        advanceMain()
        assertEquals("content://old", vm.uiState.value.avatarUri)

        vm.clearAvatar()
        advanceMain()

        verify(VerifyMode.exactly(1)) {
            settingsPort.setUserAvatarUri(null)
        }
        assertEquals(null, vm.uiState.value.avatarUri)
    }
}
