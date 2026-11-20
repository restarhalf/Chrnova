package restarhalf.stellar.schedule.ui.components.bottombar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 浮动底栏聚焦与水溶展开/收起状态控制。
 *
 * - 向下滚动页面超出阈值时自动收起为双球（失焦）；
 * - 向上回拉或滑回顶部时自动分裂展开为导航胶囊；
 * - 支持外部显式调用 [expand] / [collapse]（如闲置超时与点击展开）。
 */
@Stable
class LiquidBottomBarFocusState(
    initialExpanded: Boolean = true,
    private val scrollThresholdPx: Float,
) {
    var isExpanded: Boolean by mutableStateOf(initialExpanded)
        private set

    private var accumulatedDy = 0f

    fun expand() {
        accumulatedDy = 0f
        if (!isExpanded) {
            isExpanded = true
        }
    }

    fun collapse() {
        accumulatedDy = 0f
        if (isExpanded) {
            isExpanded = false
        }
    }

    fun updateExpanded(expanded: Boolean) {
        if (expanded) expand() else collapse()
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val deltaY = consumed.y + available.y
            if (abs(deltaY) < 0.5f) return Offset.Zero

            // 方向反转时清零累积量，避免轻微回弹误触切换
            if (accumulatedDy != 0f && (deltaY > 0f) != (accumulatedDy > 0f)) {
                accumulatedDy = 0f
            }
            accumulatedDy += deltaY

            if (deltaY < 0f) {
                // 手指上滑（内容向下滚动）超过阈值 -> 失焦收起为双球
                if (accumulatedDy <= -scrollThresholdPx) {
                    collapse()
                }
            } else {
                // 手指下拉（内容向上回滚）或回弹/惯性触及顶部 -> 分裂展开
                val hitTopEdge = consumed.y == 0f && available.y > 2f
                if (accumulatedDy >= scrollThresholdPx || hitTopEdge) {
                    expand()
                }
            }
            return Offset.Zero
        }
    }
}

@Composable
fun rememberLiquidBottomBarFocusState(
    initialExpanded: Boolean = true,
    scrollThreshold: Dp = 18.dp,
): LiquidBottomBarFocusState {
    val density = LocalDensity.current
    val thresholdPx = with(density) { scrollThreshold.toPx() }
    return remember(thresholdPx) {
        LiquidBottomBarFocusState(
            initialExpanded = initialExpanded,
            scrollThresholdPx = thresholdPx,
        )
    }
}
