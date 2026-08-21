package restarhalf.stellar.schedule.ui.screens.announcement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.LocalNavigator
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 公告详情页
 *
 * 与首页/列表页共享同一个 ViewModel 实例：进入时 load()（共享 VM 已加载过则
 * 直接命中缓存，不重复打网络），再从共享列表中按 id 选中当前公告；
 * 选中成功后把最后阅读时间推进到该条公告，此条在列表中的红点随之消失。
 */
@Composable
fun AnnouncementDetailScreen(
    vm: AnnouncementViewModel,
    announcementId: String,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit = {},
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(Unit) {
        vm.load()
    }
    LaunchedEffect(uiState.loaded) {
        if (uiState.loaded) {
            vm.selectAnnouncement(announcementId)
        }
    }
    // 选中成功后标记该条已读（共享 VM 同步推进 lastReadAtMs 并重算未读数量）
    LaunchedEffect(uiState.selectedAnnouncement) {
        val announcement = uiState.selectedAnnouncement
        if (announcement != null && announcement.id == announcementId) {
            vm.markAnnouncementRead(announcement)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "公告详情",
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
        },
    ) { paddingValues ->
        val appScaffoldPadding = LocalAppScaffoldPadding.current
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = paddingValues.calculateBottomPadding(),
                        ),
                    )
                    .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = PaddingValues(),
                outerPadding = appScaffoldPadding,
                extraTop = 12.dp,
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (uiState.loading && uiState.selectedAnnouncement == null) {
                item {
                    Text(
                        text = "加载中...",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }

            if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error ?: "",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = colors.error,
                    )
                }
            }

            val announcement = uiState.selectedAnnouncement
            if (announcement != null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (announcement.pinned) {
                                AnnouncementBadge(
                                    text = "置顶",
                                    color = colors.primary,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            if (announcement.isImportant) {
                                AnnouncementBadge(
                                    text = "重要",
                                    color = colors.error,
                                )
                            }
                        }
                        Text(
                            text = announcement.title,
                            style = MiuixTheme.textStyles.title3,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                        formatAnnouncementDate(announcement.createdAt).let { date ->
                            if (date.isNotEmpty()) {
                                Text(
                                    text = date,
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = colors.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val content = announcement.content
                        if (content.isBlank()) {
                            Text(
                                text = "暂无内容",
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                style = MiuixTheme.textStyles.body1,
                                color = colors.onSurfaceVariantSummary,
                            )
                        } else {
                        AnnouncementMarkdown(
                            content = content,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            onImageClick = onImageClick,
                        )
                        }
                    }
                }
            }
        }
    }
}
