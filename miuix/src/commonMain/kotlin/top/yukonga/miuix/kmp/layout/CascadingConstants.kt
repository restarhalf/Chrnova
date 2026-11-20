// 级联弹窗的共享动效常量
//
// 原先定义在 CascadingListPopupLayout.kt（该项目自研级联下拉、:app 侧零引用，已按死代码删除）。
// CascadingMorphContent.kt 仍在使用这些常量，故先独立出来，免得 Morph 动画跟着消失；
// 级联布局本身是否恢复可再议。

package top.yukonga.miuix.kmp.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 级联弹窗的圆角。 */
internal val CascadingPopupCornerRadius: Dp = 16.dp

/** 入场缩放的起始倍率（从 15% 长上来）。 */
internal const val ENTER_SCALE_FROM = 0.15f

/** 入场缩放的跨度，凑成 ENTER_SCALE_FROM + ENTER_SCALE_RANGE = 1f。 */
internal const val ENTER_SCALE_RANGE = 1f - ENTER_SCALE_FROM