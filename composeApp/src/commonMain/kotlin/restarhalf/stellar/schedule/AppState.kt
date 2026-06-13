package restarhalf.stellar.schedule

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.ui.sync.SyncUiState

/**
 * 应用全局状态数据类
 * 
 * 包含应用运行时的所有全局状态信息，通过CompositionLocal提供给子组件使用。
 * 使用@Stable注解确保Compose重组时的性能优化。
 */
@Stable
data class AppState(
    /** 当前校区设置，默认为金石滩校区 */
    val campus: Campus = Campus.Jinshitan,
    /** 学期开始时间戳（毫秒） */
    val termStartMs: Long = 0L,
    /** 学期总周数 */
    val totalWeeks: Int = 0,
    /** 同步UI状态，用于展示教务系统同步进度 */
    val syncUiState: SyncUiState = SyncUiState.Idle,
    /** 浮动导航栏模式：0=标准模式，1=紧凑模式 */
    val barMode: Int = 0,
    /** 待处理的更新信息，null表示无更新 */
    val pendingUpdate: AppUpdateInfo? = null,
    /** 是否显示更新确认对话框 */
    val showUpdateDialog: Boolean = false,
    /** 是否需要确认隐私协议（首次启动时为true） */
    val confirmPrivacy: Boolean = false,
    /** 是否显示APK下载进度对话框（仅Android） */
    val showApkDownloadDialog: Boolean = false,
    /** 是否允许页面用户滚动 */
    val enablePageUserScroll: Boolean = true,
    /** 是否启用页面角落裁剪效果 */
    val enableCornerClip: Boolean = true,
    /** 是否启用页面遮罩效果 */
    val enableDim: Boolean = true,
    /** 页面切换时是否阻塞用户输入 */
    val blockInputDuringTransition: Boolean = true,
    /** 弹出方向是否跟随滑动边缘 */
    val popDirectionFollowsSwipeEdge: Boolean = false,
)

/**
 * 应用状态的CompositionLocal
 * 
 * 用于在Compose组件树中提供AppState实例，子组件可通过LocalAppState.current访问。
 */
val LocalAppState = compositionLocalOf<AppState> {
    error("No AppState provided!")
}

/**
 * 应用状态更新函数的CompositionLocal
 * 
 * 用于提供状态更新函数，子组件可通过LocalUpdateAppState.current修改应用状态。
 * 使用staticCompositionLocalOf以提高性能，因为更新函数引用不会变化。
 */
val LocalUpdateAppState = staticCompositionLocalOf<((AppState) -> AppState) -> Unit> {
    error("No AppState updater provided!")
}
