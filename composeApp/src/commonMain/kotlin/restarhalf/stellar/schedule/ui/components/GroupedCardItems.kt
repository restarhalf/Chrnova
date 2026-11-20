package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haooz.softglass.shape.ContinuousRoundedRectangle
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 一组设置项共用的卡面。
 *
 * **整组是一体的一块玻璃**：圆角只在整组的外框上，组内的行没有各自的玻璃边缘 ——
 * 否则每行都会画出自己的折射与边光，接缝处会叠出一圈亮线。
 */
@Composable
fun CardGroup(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
    contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    cornerRadius: Dp = 16.dp,
    outerHorizontalPadding: Dp = 12.dp,
    outerTopPadding: Dp = 0.dp,
    outerBottomPadding: Dp = 6.dp,
    insidePadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val alpha = LocalComponentsAlpha.current
    val wallpaperBackdrop = LocalWallpaperBackdrop.current
    val cardColor = color.copy(alpha = alpha)
    val shape = remember(cornerRadius) { ContinuousRoundedRectangle(cornerRadius) }
    val background =
        if (wallpaperBackdrop != null && alpha < 0.999f) {
            Modifier.wallpaperGlass(
                backdrop = wallpaperBackdrop,
                color = cardColor,
                shape = shape,
                cornerRadius = cornerRadius,
            )
        } else {
            Modifier.squircleBackground(cardColor, cornerRadius)
        }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(
                        start = outerHorizontalPadding,
                        end = outerHorizontalPadding,
                        top = outerTopPadding,
                        bottom = outerBottomPadding,
                    )
                    .then(background)
                    .padding(insidePadding),
            content = content,
        )
    }
}

class CardItem(
    val key: String,
    val content: @Composable ColumnScope.() -> Unit,
)

/**
 * 把一组设置项作为一个 lazy item 输出。
 *
 * 整组只占一个 item、只画一次卡面，因此玻璃是连续的一整块（见 [CardGroup]）。
 */
fun LazyListScope.groupedCardItems(
    keyPrefix: String,
    items: List<CardItem>,
    outerTopPadding: Dp = 0.dp,
    outerBottomPadding: Dp = 6.dp,
    outerHorizontalPadding: Dp = 12.dp,
    insidePadding: PaddingValues = PaddingValues(0.dp),
) {
    if (items.isEmpty()) return
    item(key = "$keyPrefix:group") {
        CardGroup(
            outerTopPadding = outerTopPadding,
            outerBottomPadding = outerBottomPadding,
            outerHorizontalPadding = outerHorizontalPadding,
            insidePadding = insidePadding,
        ) {
            items.forEach { cardItem -> cardItem.content(this) }
        }
    }
}
