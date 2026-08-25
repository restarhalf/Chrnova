package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.DEFAULT_QQ_GROUP_KEY
import restarhalf.stellar.schedule.domain.usecase.CheckAppUpdateUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 关于页面ViewModel
 * 
 * 管理关于页面的UI状态，包括：
 * - 版本信息显示
 * - 应用更新检查
 * - 外部链接跳转（GitHub、教务系统、赞赏等）
 */
class AboutViewModel(
    private val checkAppUpdate: CheckAppUpdateUseCase,
) : ViewModel() {
    /**
     * 关于页面UI事件密封接口
     */
    sealed interface AboutUiEvent {
        /** 打开URI */
        data class OpenUri(val uri: String) : AboutUiEvent

        /** 加入QQ群 */
        data class JoinQqGroup(val key: String) : AboutUiEvent

        /** 微信赞赏请求 */
        data object WxPayAwardRequested : AboutUiEvent
    }
    /**
     * 关于页面UI状态
     * 
     * @param updateChecking 是否正在检查更新
     * @param updateSummary 更新状态摘要
     * @param pendingUpdate 待处理的更新信息
     */
    @Immutable
    data class AboutUiState(
        val updateChecking: Boolean,
        val updateSummary: String,
        val pendingUpdate: AppUpdateInfo?,
    )

    /**
     * 关于页面屏幕UI
     * 
     * @param versionDisplay 版本显示文本
     * @param currentVersionForCheck 用于检查更新的当前版本
     * @param updateActionSummary 更新操作摘要
     * @param canCheckUpdate 是否可以检查更新
     */
    @Immutable
    data class AboutScreenUi(
        val versionDisplay: String,
        val currentVersionForCheck: String,
        val updateActionSummary: String,
        val canCheckUpdate: Boolean,
    )

    /** UI事件流，用于发送一次性事件 */
    private val _events = MutableSharedFlow<AboutUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AboutUiEvent> = _events

    private val _updateChecking = MutableStateFlow(false)

    private val _updateSummary = MutableStateFlow("检查")

    private val _pendingUpdate = MutableStateFlow<AppUpdateInfo?>(null)

    private val _uiState: StateFlow<AboutUiState> =
        combine(_updateChecking, _updateSummary, _pendingUpdate) { updateChecking, updateSummary, pendingUpdate ->
            AboutUiState(
                updateChecking = updateChecking,
                updateSummary = updateSummary,
                pendingUpdate = pendingUpdate,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AboutUiState(updateChecking = false, updateSummary = "检查", pendingUpdate = null),
            )

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<AboutUiState> = _uiState

    /**
     * 构建关于页面屏幕UI
     * 
     * @param isInPreview 是否为预览版
     * @param versionName 版本名称
     * @param updateChecking 是否正在检查更新
     * @param updateSummary 更新状态摘要
     * @return 关于页面屏幕UI
     */
    fun buildScreenUi(
        isInPreview: Boolean,
        versionName: String,
        updateChecking: Boolean,
        updateSummary: String,
    ): AboutScreenUi {
        val versionDisplay = if (isInPreview) "预览版" else versionName.ifBlank { "未知版本" }
        return AboutScreenUi(
            versionDisplay = versionDisplay,
            currentVersionForCheck = if (isInPreview) "" else versionDisplay,
            updateActionSummary = if (updateChecking) "检查中..." else updateSummary,
            canCheckUpdate = !updateChecking
        )
    }

    /** 清除待处理的更新信息 */
    fun clearPendingUpdate() {
        _pendingUpdate.value = null
    }

    /**
     * 检查应用更新
     * 
     * @param currentVersionName 当前版本名称
     */
    fun checkUpdate(currentVersionName: String) {
        if (_updateChecking.value) return

        _updateChecking.value = true

        viewModelScope.launch {
            runCatching {
                val latest =
                    withContext(AppIoDispatcher) {
                        checkAppUpdate(currentVersionName)
                    }

                if (latest == null) {
                    _updateSummary.value =
                        "已是最新版本" +
                                (currentVersionName.takeIf { it.isNotBlank() }?.let { "（$it）" }
                                    ?: "")
                } else {
                    _pendingUpdate.value = latest
                    _updateSummary.value = "发现新版本 ${latest.latestVersion}"
                }
            }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Update", "检查更新失败", e)
                    _updateSummary.value = e.toUserFacingMessage(UserFacingErrorKind.CheckUpdate)
                }

            _updateChecking.value = false
        }
    }

    /**
     * 请求加入QQ群
     * 
     * @param key QQ群加群链接中的key参数
     */
    private fun requestJoinQqGroup(key: String) {
        _events.tryEmit(AboutUiEvent.JoinQqGroup(key))
    }

    /** 请求加入默认QQ群 */
    fun requestJoinDefaultQqGroup() {
        requestJoinQqGroup(DEFAULT_QQ_GROUP_KEY)
    }

    /** 请求打开教务系统PC端 */
    fun requestOpenJwxtPc() {
        _events.tryEmit(AboutUiEvent.OpenUri("http://jwxt.dlnu.edu.cn/jsxsd/"))
    }

    /** 请求打开教务系统移动端 */
    fun requestOpenJwxtMobile() {
        _events.tryEmit(AboutUiEvent.OpenUri("http://jwyd.dlnu.edu.cn/sjd/#/login"))
    }

    /** 请求打开GitHub仓库 */
    fun requestOpenGithub() {
        _events.tryEmit(AboutUiEvent.OpenUri("https://github.com/restarhalf/Chrnova"))
    }

    /** 请求打开支付宝赞赏码 */
    fun requestOpenAlipayAward() {
        _events.tryEmit(
            AboutUiEvent.OpenUri(
                "alipayqr://platformapi/startapp?saId=10000007&qrcode=https://qr.alipay.com/lpx14068ip9zzsydreka745"
            )
        )
    }

    /** 请求微信赞赏 */
    fun requestWxPayAward() {
        _events.tryEmit(AboutUiEvent.WxPayAwardRequested)
    }
}
