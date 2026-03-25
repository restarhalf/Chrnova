package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.core.update.DEFAULT_QQ_GROUP_KEY
import restarhalf.stellar.schedule.platform.AppIoDispatcher

sealed interface AboutUiEvent {
    data class OpenUri(val uri: String) : AboutUiEvent

    data class JoinQqGroup(val key: String) : AboutUiEvent

    data object WxPayAwardRequested : AboutUiEvent
}

class AboutViewModel(
    private val appUpdate: AppUpdatePort,
) : ViewModel() {

    data class ScreenUi(
        val versionDisplay: String,
        val currentVersionForCheck: String,
        val updateActionSummary: String,
        val canCheckUpdate: Boolean,
    )

    private val _events = MutableSharedFlow<AboutUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AboutUiEvent> = _events

    private val _updateChecking = MutableStateFlow(false)
    val updateChecking: StateFlow<Boolean> = _updateChecking.asStateFlow()

    private val _updateSummary = MutableStateFlow("检查")
    val updateSummary: StateFlow<String> = _updateSummary.asStateFlow()

    private val _pendingUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val pendingUpdate: StateFlow<AppUpdateInfo?> = _pendingUpdate.asStateFlow()

    fun buildScreenUi(
        isInPreview: Boolean,
        versionName: String,
        updateChecking: Boolean,
        updateSummary: String,
    ): ScreenUi {
        val versionDisplay = if (isInPreview) "预览版" else versionName.ifBlank { "未知版本" }
        return ScreenUi(
            versionDisplay = versionDisplay,
            currentVersionForCheck = if (isInPreview) "" else versionDisplay,
            updateActionSummary = if (updateChecking) "检查中..." else updateSummary,
            canCheckUpdate = !updateChecking
        )
    }

    fun clearPendingUpdate() {
        _pendingUpdate.value = null
    }

    fun checkUpdate(currentVersionName: String) {
        if (_updateChecking.value) return

        _updateChecking.value = true

        viewModelScope.launch {
            runCatching {
                val latest =
                    withContext(AppIoDispatcher) {
                        appUpdate.check(currentVersionName = currentVersionName)
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
                .onFailure { _updateSummary.value = it.message ?: "检查更新失败" }

            _updateChecking.value = false
        }
    }

    fun requestJoinQqGroup(key: String) {
        _events.tryEmit(AboutUiEvent.JoinQqGroup(key))
    }

    fun requestJoinDefaultQqGroup() {
        requestJoinQqGroup(DEFAULT_QQ_GROUP_KEY)
    }

    fun requestOpenJwxtPc() {
        _events.tryEmit(AboutUiEvent.OpenUri("http://jwxt.dlnu.edu.cn/jsxsd/"))
    }

    fun requestOpenJwxtMobile() {
        _events.tryEmit(AboutUiEvent.OpenUri("http://jwyd.dlnu.edu.cn/sjd/#/login"))
    }

    fun requestOpenGithub() {
        _events.tryEmit(AboutUiEvent.OpenUri("https://github.com/restarhalf/scheduleKMP"))
    }

    fun requestPublishPage() {
        _events.tryEmit(AboutUiEvent.OpenUri("https://schedule.restarhalf.dpdns.org"))
    }

    fun requestOpenAlipayAward() {
        _events.tryEmit(
            AboutUiEvent.OpenUri(
                "alipayqr://platformapi/startapp?saId=10000007&qrcode=https://qr.alipay.com/lpx14068ip9zzsydreka745"
            )
        )
    }

    fun requestWxPayAward() {
        _events.tryEmit(AboutUiEvent.WxPayAwardRequested)
    }
}
