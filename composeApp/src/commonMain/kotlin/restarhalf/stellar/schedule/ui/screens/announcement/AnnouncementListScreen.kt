package restarhalf.stellar.schedule.ui.screens.announcement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 公告列表页
 *
 * 展示全部已发布公告（置顶优先、时间倒序）。进入本页时强制刷新拉取最新公告
 * （忽略 10 分钟缓存，保证新发布的公告立即可见）。未读公告（发布时间晚于
 * 最后阅读时间）在条目右上角显示红点，点开某条详情后该条才变为已读；
 * 首页红点随之更新（首页/列表/详情共享同一个 ViewModel）。
 */
@Composable
fun AnnouncementListScreen(
    vm: AnnouncementViewModel,
    onBack: () -> Unit,
    onAnnouncementClick: (String) -> Unit,
    onAdOpenUrl: (String) -> Unit = {},
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    // 进入列表页：强制刷新公告与广告位配置，忽略缓存拿到最新内容（广告后端改动实时生效）
    LaunchedEffect(Unit) {
        vm.refresh()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "公告",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                        ) {
                            Icon(
                                imageVector = Back,
                                contentDescription = "返回",
                            )
                        }
                    },
                )
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(CircleShape)
                                    .background(colors.surfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(style = MiuixTheme.textStyles.footnote1, text = uiState.error ?: "")
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = 0.dp,
                        ),
                    ),
        ) {
            // 广告位（顶栏下方、列表上方，固定不随列表滚动）
            // 配置由公告 Worker 的 /ad 接口下发；后端未配置时 uiState.adConfig 为 null，整条隐藏
            AdBanner(
                config = uiState.adConfig?.let {
                    AdBannerConfig(
                        imageUrl = it.imageUrl,
                        targetUrl = it.targetUrl,
                        announcementId = it.announcementId,
                    )
                },
                onOpenUrl = onAdOpenUrl,
                onOpenAnnouncement = onAnnouncementClick,
            )

            PullToRefresh(
                isRefreshing = uiState.loading,
                onRefresh = { vm.refresh() },
                pullToRefreshState = pullToRefreshState,
                refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
                contentPadding = appPageContentPadding(
                    innerPadding = PaddingValues(),
                    outerPadding = paddingValues,
                    extraTop = 12.dp,
                    extraStart = 12.dp,
                    extraEnd = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!uiState.loading && uiState.announcements.isEmpty() && uiState.error == null) {
                    item {
                        Text(
                            text = "暂无公告",
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            style = MiuixTheme.textStyles.body2,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }

                items(uiState.announcements, key = { it.id }) { announcement ->
                    val date = formatAnnouncementDate(announcement.createdAt)
                    val isUnread = announcement.lastChangeAtMs > uiState.lastReadAtMs
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onAnnouncementClick(announcement.id) },
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                AnnouncementTitleRow(
                                    title = announcement.title,
                                    pinned = announcement.pinned,
                                    important = announcement.isImportant,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (date.isNotEmpty()) {
                                    Text(
                                        text = date,
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = colors.onSurfaceVariantSummary,
                                    )
                                }
                            }
                            if (isUnread) {
                                UnreadBadge(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 16.dp, end = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
