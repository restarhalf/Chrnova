package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

fun Modifier.pageScrollModifiers(
    showTopAppBar: Boolean = true,
    scrollBehavior: ScrollBehavior,
): Modifier =
    this
        .then(
            if (showTopAppBar) {
                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            } else {
                Modifier
            },
        )
        .scrollEndHaptic()
        .overScrollVertical()
        .fillMaxHeight()

@Composable
fun appPageContentPadding(
    innerPadding: PaddingValues,
    outerPadding: PaddingValues,
    extraTop: Dp = 0.dp,
    extraStart: Dp = 0.dp,
    extraEnd: Dp = 0.dp,
): PaddingValues {
    val topPadding = innerPadding.calculateTopPadding() + extraTop
    val bottomPadding = outerPadding.calculateBottomPadding() + 12.dp

    return remember(topPadding, bottomPadding, extraStart, extraEnd) {
        PaddingValues(
            top = topPadding,
            start = extraStart,
            end = extraEnd,
            bottom = bottomPadding,
        )
    }
}

@Composable
fun rememberAppPageScrollBehavior(): ScrollBehavior = MiuixScrollBehavior()

@Composable
fun AppPageTopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    color: Color = Color.Transparent,
) {
    SmallTopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        defaultWindowInsetsPadding = false,
        navigationIcon = navigationIcon,
        actions = actions,
        color = color
    )
}
