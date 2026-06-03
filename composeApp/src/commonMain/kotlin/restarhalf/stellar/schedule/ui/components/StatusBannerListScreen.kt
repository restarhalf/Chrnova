package restarhalf.stellar.schedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect

@Composable
fun <T> StatusBannerListScreen(
    title: String,
    statusText: String?,
    loading: Boolean,
    onRefresh: () -> Unit,
    items: List<T>,
    keySelector: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    popupHost: @Composable () -> Unit = {},
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val colors = MiuixTheme.colorScheme
    val surfaceSoft = colors.surfaceContainerHigh
    val overscrollEffect = MiuixOverscrollEffect()

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(title = title, scrollBehavior = topAppBarScrollBehavior)
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val text = statusText ?: ""
                    Box(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier =
                                Modifier.clip(CircleShape)
                                    .background(surfaceSoft)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(fontSize = 12.sp, text = text)
                        }
                    }
                }
            }
        },
        popupHost = popupHost,
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = loading,
            onRefresh = onRefresh,
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf(
                "下拉刷新",
                "释放刷新",
                "正在刷新...",
                "刷新成功"
            ),
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            start =
                                paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = 0.dp
                        )
                    )
        ) {
            LazyColumn(
                modifier =
                    Modifier.fillMaxSize()
                        .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
                contentPadding =
                    appPageContentPadding(
                        innerPadding = PaddingValues(),
                        outerPadding = appScaffoldPadding,
                        extraTop = 12.dp,
                        extraStart = 16.dp,
                        extraEnd = 16.dp,
                    ),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                overscrollEffect = overscrollEffect
            ) {
                items(items, key = keySelector) { item ->
                    Box(
                        modifier = Modifier.animateItem(
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        itemContent(item)
                    }
                }
            }
        }
    }
}
