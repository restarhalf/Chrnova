package top.yukonga.miuix.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Skia（iOS / Desktop / wasm）实现：CMP 1.12 无 predictive back 公共 API
 * （上游 Miuix 0.9.3 也仅在 androidMain 用 androidx.navigationevent）。
 * handler 为空实现（iOS 系统返回手势由 UIKit 接管，Kotlin 侧拿不到事件，不会误触发）；
 * 将来在 iOS 入口层接系统手势后只需替换 `onHandler`，调用点无需改动。
 * 终态「确认返回」→ onBackCompleted，「取消」→ onBackCancelled，与 navigationevent 对齐。
 */
@Composable
actual fun rememberNavigationBack(): NavigationBackState = remember {
    NavigationBackState(
        // 非 Android 侧没有跨帧进度源：保守报告「不在转场」「无进度」。
        // 调用方的状态机仍可只靠 onBackCompleted 正常关闭，不会误判。
        isTransitioningBack = { false },
        backProgress = { null },
        // 没有手势源：不报边缘（2 = EDGE_NONE）
        backSwipeEdge = { 2 },
        // 没有触摸坐标源：无垂直/水平跟随
        backSwipeOffsetY = { null },
        backSwipeOffsetX = { null },
        onHandler = {},
    )
}