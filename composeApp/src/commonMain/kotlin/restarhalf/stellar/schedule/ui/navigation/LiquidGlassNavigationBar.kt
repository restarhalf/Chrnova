package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.softglass.backdrop.GlassBackdrop
import restarhalf.stellar.schedule.ui.components.bottombar.LiquidBottomTab
import restarhalf.stellar.schedule.ui.components.bottombar.LiquidBottomTabs
import restarhalf.stellar.schedule.ui.components.bottombar.LiquidBottomTrailing
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val BAR_HEIGHT = 56.dp

private val BAR_GAP = 10.dp

private val BAR_DRAG_ROOM = 18.dp

private const val TAB_WIDTH_FRACTION = 0.157f

private val PILL_MAX_WIDTH = 430.dp

data class LiquidBarTab(
    val icon: ImageVector,
    val label: String,
)

@Composable
fun LiquidGlassNavigationBar(
    tabs: List<LiquidBarTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: GlassBackdrop,
    trailing: LiquidBottomTrailing?,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val iconTint = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.8f)

    val navBarBottomPadding =
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPaddingValue = if (navBarBottomPadding > 0.dp) navBarBottomPadding + 8.dp else 28.dp

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = bottomPaddingValue)
                .height(BAR_HEIGHT),
    ) {
        val pillWidth = minOf(maxWidth * (TAB_WIDTH_FRACTION * tabs.size), PILL_MAX_WIDTH)
        val visibleWidth = pillWidth + BAR_GAP + BAR_HEIGHT
        val nodeWidth = visibleWidth + BAR_DRAG_ROOM
        val nodeLeft = (maxWidth - visibleWidth) / 2

        LiquidBottomTabs(
            selectedTabIndex = { selectedIndex },
            onTabSelected = onTabSelected,
            backdrop = backdrop,
            tabsCount = tabs.size,
            isExpanded = isExpanded,
            onExpandedChange = onExpandedChange,
            containerHeight = BAR_HEIGHT,
            trailing = trailing,
            modifier =
                Modifier
                    .offset(x = nodeLeft)
                    .width(nodeWidth)
                    .height(BAR_HEIGHT),
        ) {
            tabs.forEachIndexed { index, tab ->
                LiquidBottomTab(
                    index = index,
                    icon = {
                        Image(
                            imageVector = tab.icon,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconTint),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = iconTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

internal fun liquidTrailingTint(isDark: Boolean): Color =
    if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
