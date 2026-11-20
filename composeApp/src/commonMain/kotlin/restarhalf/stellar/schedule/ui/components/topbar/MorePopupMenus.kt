package restarhalf.stellar.schedule.ui.components.topbar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haooz.softglass.backdrop.GlassBackdrop
import restarhalf.stellar.schedule.ui.icons.More
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 顶栏「更多」菜单的一项。每个页面传自己的一套，菜单本体复用同一套玻璃下拉。 */
data class AppMoreMenuItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val iconSize: Dp = 24.dp,
)

@Composable
fun MorePopupMenu(
    showMorePopup: Boolean,
    onMorePopupDismiss: () -> Unit,
    items: List<AppMoreMenuItem>,
    morePopupFraction: Animatable<Float, *>,
    moreSlotCoordinates: State<LayoutCoordinates?>,
    liquidGlassBackdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    onMorePopupExpand: () -> Unit = {},
    materialAlpha: () -> Float = { 0f },
    onMoreBackCancelled: () -> Unit = {},
    drawTrigger: Boolean = true,
    horizontalOffset: Dp = 9.dp,
    enabled: Boolean = true,
) {
    if (!enabled) {
        return
    }
    if (showMorePopup) {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onMorePopupDismiss() },
        )
    }
    val fallbackAnchorTopPx =
        with(LocalDensity.current) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            (if (statusBarTop > 0.dp) statusBarTop - 20.dp else 17.dp)
                .coerceAtLeast(0.dp)
                .toPx()
        }
    val shadowPaddingPx =
        with(LocalDensity.current) { LiquidGlassDropdownShadowPadding.toPx() }
    val containerCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { containerCoordinates.value = it }
                .graphicsLayer {
                    clip = false
                    val slot = moreSlotCoordinates.value
                    val container = containerCoordinates.value
                    val slotTopPx =
                        if (slot != null && container != null && slot.isAttached && container.isAttached) {
                            container.localPositionOf(slot, Offset.Zero).y
                        } else {
                            0f
                        }
                    val measured = slotTopPx - shadowPaddingPx
                    translationY = if (measured > 0f) measured else fallbackAnchorTopPx
                }
                .offset(x = horizontalOffset),
        contentAlignment = Alignment.TopEnd,
    ) {
        LiquidGlassDropdownMenu(
            show = showMorePopup,
            backdrop = liquidGlassBackdrop,
            fraction = morePopupFraction,
            onDismiss = onMorePopupDismiss,
            onBackCancelled = onMoreBackCancelled,
            triggerIcon = if (drawTrigger) More else null,
            triggerContentDescription = "更多",
            onExpand = if (drawTrigger) onMorePopupExpand else null,
            materialAlpha = materialAlpha,
        ) {
            items.forEach { item ->
                LiquidGlassDropdownMenuItem(
                    text = item.text,
                    onClick = {
                        onMorePopupDismiss()
                        item.onClick()
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(item.iconSize),
                        )
                    },
                )
            }
        }
    }
}
