package restarhalf.stellar.schedule.ui.components.topbar

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.layout.SharedScrollBehavior
import top.yukonga.miuix.kmp.utils.LocalOverScrollState

/** 顶栏按钮背景的出现阈值：内容滚过 10dp（与 CollapsibleTopAppBar 内部同一判据） */
private val MaterialScrollThreshold = 10.dp

/**
 * 顶栏材质 alpha：内容滚过阈值后按弹簧升到 1，滚回顶部再降回 0。
 *
 * 判据与弹簧都对齐 CollapsibleTopAppBar 的按钮背景，顶栏的模糊带 / 无材质档渐变遮罩
 * 靠它跟按钮玻璃一起出现、一起消失（停在顶部时整条留空）。
 *
 * 返回值只在绘制期读（graphicsLayer 阶段），动画值不进组合。
 */
@Composable
fun rememberTopBarMaterialAlpha(scrollBehavior: SharedScrollBehavior): () -> Float {
    val thresholdPx = with(LocalDensity.current) { MaterialScrollThreshold.toPx() }
    // 只作身份键：offset 在 derivedStateOf 内部读，本身就会被订阅
    val overScrollState = LocalOverScrollState.current
    val scrolled by
        remember(scrollBehavior, thresholdPx, overScrollState) {
            derivedStateOf {
                // contentOffset 是累计滚动量，回到顶部才自然归零，触底回弹不会把材质抖掉
                val contentOffset = scrollBehavior.state.contentOffset
                if (contentOffset < -thresholdPx) {
                    true
                } else {
                    contentOffset >= 0f && overScrollState.offset < 0f
                }
            }
        }
    val alpha = remember { Animatable(if (scrolled) 1f else 0f) }
    LaunchedEffect(scrolled) {
        alpha.animateTo(
            targetValue = if (scrolled) 1f else 0f,
            animationSpec =
                if (scrolled) {
                    folmeSpring(damping = 1.0f, response = 0.6f)
                } else {
                    folmeSpring(damping = 1.0f, response = 0.4f)
                },
        )
    }
    return remember(alpha) { { alpha.value } }
}
