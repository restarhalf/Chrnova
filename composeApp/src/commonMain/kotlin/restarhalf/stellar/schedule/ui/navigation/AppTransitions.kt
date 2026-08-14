package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

/**
 * Chrnova 专用导航转场。
 *
 * 与 example 的关键差异：example 的页面全部使用不透明 `Scaffold`（默认背景色），
 * 顶层 entry 天然遮住被覆盖的下层；而 Chrnova 所有页面透明
 * （`Scaffold(containerColor = Color.Transparent)`），背景图从导航宿主层透出，
 * 因此不能依赖 miuix-nav 的"顶层 entry 不透明"假设。
 *
 * 若直接使用 [top.yukonga.miuix.kmp.nav.transition.NavTransitions.MiuixDefault]
 * （covered 处理保持下层可见：静止时 25% parallax + alpha 0.9），被覆盖的一级页
 * 会从透明二级页里直接透出来。这里把 covered 段改为随覆盖进度线性淡出，
 * d=1 静止时 alpha=0，复刻旧 navigation3 的"下层滑出+淡出"观感；
 * opaqueDepth 保持 1f，让被覆盖层持续 composition（状态保留），仅渲染为不可见。
 *
 * scrim 恒为 0：不绘制任何转场调暗遮罩。默认的深度线性曲线在静止时
 * （covered d=1）返回 1.0，配合 `dimAmount` 会在透明顶层下常驻一层黑幕；
 * 即便改成 `4*d*(1-d)` 帐篷曲线，转场中途仍有 50% 黑屏感。透明栈下
 * 直接禁用调暗，转场时背景图始终干净。
 */
val TransparentStackTransition: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    scrim = { 0f },
) { scope ->
    val width = scope.layoutSize.width.toFloat()
    val d = scope.relativeDepth
    val rtl = scope.layoutDirection == LayoutDirection.Rtl
    if (d <= 0f) {
        // 进入/离开顶层：与 MiuixDefault 一致的尾端全宽滑入。
        // 吸附到整像素：圆角裁剪页面的抗锯齿边缘在转场时不会因亚像素偏移而闪烁。
        translationX = ((if (rtl) -1f else 1f) * (-d).coerceIn(0f, 1f) * width).fastRoundToInt().toFloat()
    } else {
        // 被覆盖：向 leading 端 parallax 1/4 宽度，并随覆盖进度线性淡出，
        // 静止时（d=1）alpha=0，避免从透明二级页透出一级页内容。
        val p = d.coerceIn(0f, 1f)
        translationX = (if (rtl) 1f else -1f) * p * width * 0.25f
        alpha = 1f - p
    }
}
