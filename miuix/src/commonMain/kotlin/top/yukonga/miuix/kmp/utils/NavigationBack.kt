// 返回键预测 —— 跨平台封装（navigationevent 的唯一用途）。
//
// androidx.navigationevent 是 AndroidX 库，CMP 的 iOS / Desktop artifact 里没有；
// 上游 fork 曾在 commonMain 直接 import 22 处、靠 Android artifact 兜底。
// 这里收成一个最小 expect 接口，commonMain 不再直接依赖 AndroidX；
// Android 实现逐字保留原逻辑（含 onBackCancelled 分支），行为零变化。

package top.yukonga.miuix.kmp.utils

import androidx.compose.runtime.Composable

/** [NavigationBackState.handler] 的入参。用一个类打包，是为了能以**具名参数**调用。 */
class BackHandlerScope(
    val enabled: Boolean,
    val onBackCompleted: () -> Unit,
    val onBackCancelled: () -> Unit,
)

/**
 * 返回转场状态。
 *
 * ⚠ **[handler] 与本对象里的进度读取必须来自同一次 [rememberNavigationBack]。**
 *
 * 原 fork 由**一个** `navigationEventState` 同时喂 `NavigationBackHandler`
 * 和 `transitionState` 读取——必须是同一对象，否则 handler 收到的手势进度
 * 不会出现在另一个 state 上，跟手动画永远不动。
 * 迁移时曾拆成两个独立 expect 函数，编译全绿但 Android 预测性返回完全失效；
 * 现在把 handler 收进本类，从签名上杜绝拆错。
 */
class NavigationBackState(
    val isTransitioningBack: () -> Boolean,
    /**
     * 手势进度 0f→1f；非手势期间为 null。
     *
     * DialogContentLayout 的可回退退场动画要按进度连续插值（缩放 + 蒙层淡出），
     * 所以这里必须给出连续值，而不只是布尔 —— 布尔只能支持「禁止中途关闭」。
     */
    val backProgress: () -> Float?,
    /**
     * 手势起始边缘：0 = 左边缘，1 = 右边缘，2 = 非边缘滑动（返回键 / 无手势）。
     *
     * 数值与 `androidx.navigationevent.NavigationEvent.EDGE_*` 一致；commonMain 不引 AndroidX，
     * 所以这里只声明语义，不引用常量。
     */
    val backSwipeEdge: () -> Int,
    /**
     * 手势开始至今的**垂直**触摸位移（px，= 当前 touchY − 手势首帧 touchY）。
     * 非手势、非触摸事件（返回键）、无触摸数据时返回 null。
     * 用途：全屏 Surface 的上下跟随 —— progress 只归一化水平方向，垂直位移要调用方自己算。
     */
    val backSwipeOffsetY: () -> Float?,
    /**
     * 手势开始至今的**水平**触摸位移（px，= 当前 touchX − 手势首帧 touchX）。
     * 语义与 [backSwipeOffsetY] 对称；调用方用它驱动缩放进度等。
     */
    val backSwipeOffsetX: () -> Float?,
    private val onHandler: @Composable BackHandlerScope.() -> Unit,
) {
    /**
     * 注册系统返回处理器（与本 state 共用同一份底层状态）。
     *
     * @param enabled 是否启用（与原 NavigationBackHandler 的 isBackEnabled 同义）
     * @param onBackCompleted 完整返回（键按下 / 手势完成）
     * @param onBackCancelled 返回手势中途取消（老平台不会触发）
     */
    @Composable
    fun handler(
        enabled: Boolean,
        onBackCompleted: () -> Unit,
        onBackCancelled: () -> Unit,
    ) {
        onHandler(BackHandlerScope(enabled, onBackCompleted, onBackCancelled))
    }
}

/**
 * 取得返回转场状态与返回处理器，**同一份状态**。
 *
 * 用法：
 * ```
 * val back = rememberNavigationBack()
 * back.handler(enabled = show, onBackCompleted = { … }, onBackCancelled = { … })
 * val progress = back.backProgress()
 * ```
 */
@Composable
expect fun rememberNavigationBack(): NavigationBackState

/**
 * 处理系统返回键 —— 只处理不读进度的便捷入口（如 MiuixPopupUtils）。
 *
 * 若调用点还要读手势进度驱动动画，**必须改用 [rememberNavigationBack] 后调用
 * `state.handler(...)`**，否则会各自持有一份独立状态，跟手动画不会动。
 */
@Composable
fun NavigationBackHandlerFor(
    enabled: Boolean,
    onBackCompleted: () -> Unit,
    onBackCancelled: () -> Unit,
    state: NavigationBackState = rememberNavigationBack(),
) {
    state.handler(enabled, onBackCompleted, onBackCancelled)
}