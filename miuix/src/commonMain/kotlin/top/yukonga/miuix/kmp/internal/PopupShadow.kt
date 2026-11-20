// 面板投影阴影 —— 跨平台绘制
//
// expect/actual：
//   androidMain —— BlurMaskFilter（逐像素与 Android 原生路径一致）
//   skikoMain   —— 构造环形路径后复用 [drawBlurredPathShadow]（Skia MaskFilter）
//
// 形状语义：环形路径（外圈 CW 减内圈 CCW）再整体模糊 = 圆角矩形描边的模糊；
// 不用 backdrop 的 shadow 是因其 ShadowModifier 为「填充后模糊」，与「模糊描边」不是一回事。

package top.yukonga.miuix.kmp.internal

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 在当前绘制范围里，画一块圆角矩形的模糊投影。
 *
 * @param size       绘制区域尺寸
 * @param left/top   目标圆角矩形左上角（画布坐标）
 * @param width/height 目标圆角矩形尺寸
 * @param radius     圆角半径（px）
 * @param spread     投影外扩距离（px），0 表示不外扩
 * @param blurRadius 模糊半径（px）
 * @param color      阴影颜色
 */
expect fun DrawScope.drawBlurredRingShadow(
    size: Size,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
    spread: Float,
    blurRadius: Float,
    color: Color,
)
