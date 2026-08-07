package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.usecase.BuildHomeSurfaceUiUseCase
import restarhalf.stellar.schedule.ui.components.screen.home.HomeExamSection
import restarhalf.stellar.schedule.ui.components.screen.home.HomePeriodSection
import restarhalf.stellar.schedule.ui.icons.Notifications
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import restarhalf.stellar.schedule.ui.viewmodel.HomeViewModel
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页屏幕
 * 
 * 显示今日课程、问候语、当前时间等信息。
 * 
 * @param campus 当前校区
 * @param termStartMs 学期开始时间戳
 * @param totalWeeks 学期总周数
 */
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    announcementVm: AnnouncementViewModel,
    onAnnouncementClick: () -> Unit,
    hasBackground: Boolean = false,
    componentsAlpha: Float,
    campus: Campus,
    termStartMs: Long,
    totalWeeks: Int,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val colors = MiuixTheme.colorScheme
    val textPrimary = colors.onBackground
    val textSecondary = colors.onSurfaceVariantSummary
    val dividerColor = colors.surfaceContainerHigh
    val homeUiState by vm.uiState.collectAsStateWithLifecycle()
    val announcementUiState by announcementVm.uiState.collectAsStateWithLifecycle()
    val renderState =
        remember(
            homeUiState.courses,
            campus,
            termStartMs,
            totalWeeks,
            hasBackground,
            componentsAlpha,
            homeUiState.nowMs
        ) {
            vm.buildHomeRenderState(
                courses = homeUiState.courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                hasBackground = hasBackground,
                componentsAlpha = componentsAlpha,
                nowMs = homeUiState.nowMs
            )
        }
    val headerUi = renderState.headerUi
    val surfaceUi = renderState.surfaceUi
    val sectionRenders = renderState.sectionRenders
    val todayExams = remember(homeUiState.exams, homeUiState.nowMs) {
        vm.buildExamUiList(
            vm.getTodayExams(homeUiState.exams, homeUiState.nowMs),
            homeUiState.nowMs
        )
    }
    Scaffold(
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 0.dp)
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp + paddingValues.calculateTopPadding())
                        .background(
                            if (surfaceUi.headerBackgroundMode ==
                                BuildHomeSurfaceUiUseCase.HeaderBackgroundMode.IMAGE_OVERLAY
                            ) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            colors.primary.copy(0.8f),
                                            colors.primary.copy(0.8f)
                                        )
                                )
                            }
                        )
            ) {
                Column(
                    modifier =
                        Modifier.padding(top = paddingValues.calculateTopPadding() + 32.dp, start = 32.dp)
                ) {
                    Text(
                        text = headerUi.dateLabel,
                        color = colors.onPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = headerUi.greeting,
                        color = colors.onPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding() + 140.dp)
                        .squircleBackground(colors.surface.copy(alpha = surfaceUi.contentSurfaceAlpha), 24.dp)
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                            .padding(bottom = appScaffoldPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    sectionRenders.forEach { section ->
                        HomePeriodSection(
                            title = section.title,
                            rows = section.rows,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            dividerColor = dividerColor,
                        )
                    }
                    if (todayExams.isNotEmpty()) {
                        HomeExamSection(
                            exams = todayExams,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            dividerColor = dividerColor,
                        )
                    }
                }
            }

            // 右上角公告入口：通知图标 + 未读红点（置于 z 最顶层，避免被内容区拦截点击）
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding()+20.dp, end = 20.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                HomeNotificationButton(
                    unreadCount = announcementUiState.unreadCount,
                    onClick = onAnnouncementClick,
                )
            }
        }
    }
}

/**
 * 首页右上角公告入口：通知图标 + 未读数徽标。
 *
 * 徽标用 miuix [BadgedBox] + [Badge] 实现：带 content 时 Badge 自动扩展为
 * 数字超过 99 显示 "99+"。
 *
 * 红点在用户**点开公告详情页**后才清除（详见 AnnouncementDetailScreen），
 * 列表页期间红点保持，提示用户仍有未读。
 *
 * @param unreadCount 未读数量；大于 0 时显示带数字徽标
 * @param onClick 点击回调（进入公告列表页）
 */
@Composable
private fun HomeNotificationButton(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    BadgedBox(
        modifier =
            Modifier
                .clickable(onClick = onClick),
        badge = {
            if (unreadCount > 0) {
                Badge {
                    Text(if (unreadCount > 99) "99+" else "$unreadCount")
                }
            }
        },
    ) {
        Icon(
            imageVector = Notifications,
            contentDescription = "公告",
            modifier = Modifier.size(22.dp),
            tint = colors.onPrimary,
        )
    }
}

