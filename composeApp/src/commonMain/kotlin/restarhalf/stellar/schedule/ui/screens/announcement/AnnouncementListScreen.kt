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
import restarhalf.stellar.schedule.ui.chrome.AppPageScaffold
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.layout.collapsibleTopInset
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AnnouncementListScreen(
    vm: AnnouncementViewModel,
    onBack: () -> Unit,
    onAnnouncementClick: (String) -> Unit,
    onAdOpenUrl: (String) -> Unit = {},
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(Unit) {
        vm.refresh()
    }

    AppPageScaffold(
        title = "公告",
        onBack = onBack,
        topBarExtra = {
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
        },
    ) { scrollBehavior, paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        PullToRefresh(
            isRefreshing = uiState.loading,
            onRefresh = { vm.refresh() },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
            modifier =
                Modifier
                    .fillMaxSize()
                    .collapsibleTopInset(scrollBehavior)
                    .padding(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                    ),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pageScrollModifiers(scrollBehavior = scrollBehavior),
                contentPadding = appPageContentPadding(
                    innerPadding = paddingValues,
                    outerPadding = paddingValues,
                    extraTop = 12.dp,
                    extraStart = 12.dp,
                    extraEnd = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val adConfig = uiState.adConfig
                if (adConfig != null) {
                    item(key = "adBanner") {
                        AdBanner(
                            config = AdBannerConfig(
                                imageUrl = adConfig.imageUrl,
                                targetUrl = adConfig.targetUrl,
                                announcementId = adConfig.announcementId,
                            ),
                            onOpenUrl = onAdOpenUrl,
                            onOpenAnnouncement = onAnnouncementClick,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

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
                    val isUnread = announcement.id !in uiState.readIds
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
