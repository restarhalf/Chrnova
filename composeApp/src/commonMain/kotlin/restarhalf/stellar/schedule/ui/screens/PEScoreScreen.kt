package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.domain.model.AuthProfile
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.QrCode
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect

/**
 * 体育成绩屏幕
 *
 * 显示体测成绩列表，支持：
 * - 登录体育系统
 * - 查看年度体测成绩
 * - 点击查看详情
 * - 下拉刷新
 * - 登出功能
 *
 * @param onNavigateToDetail 导航到成绩详情页面的回调
 * @param onQRCode 导航到二维码页面的回调
 */
@Composable
fun PEScoreScreen(
    vm: PEViewModel,
    onNavigateToDetail: (String) -> Unit,
    onLogin: () -> Unit,
    onQRCode: () -> Unit = {},
    authProfile: AuthProfile? = null,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val overscrollEffect = MiuixOverscrollEffect()
    val uiState by vm.uiState.collectAsState()
    val yearScores = uiState.yearScores
    val loading = uiState.loading
    val loggedIn = vm.isLoggedIn()
    val hasQRCodeInfo =
        loggedIn || (authProfile?.userNo?.isNotBlank() == true && authProfile.name.isNotBlank())
    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            vm.loadScoreList()
            vm.loadStudentInfo()
        }
    }

    val statusText = vm.buildStatusText()
    val colors = MiuixTheme.colorScheme

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "体测",
                    scrollBehavior = topAppBarScrollBehavior,
                    actions = {
                        if (hasQRCodeInfo) {
                            IconButton(onClick = onQRCode) {
                                Icon(imageVector = QrCode, contentDescription = "二维码")
                            }
                        }
                    },
                )
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.clip(CircleShape)
                                .background(colors.surfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(fontSize = 12.sp, text = statusText ?: "")
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = loading,
            onRefresh = { vm.loadScoreList() },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
            modifier = Modifier.fillMaxSize().padding(
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = 0.dp
                )
            )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
                contentPadding = appPageContentPadding(
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
                if (!loggedIn) {
                    item {
                        SmallTitle(text = "账号")
                        AppCard {
                            ArrowPreference(
                                title = "登录",
                                summary = "用于获取体测成绩",
                                onClick = onLogin
                            )
                        }
                    }
                }

                items(yearScores, key = { it.schoolYear }) { score ->
                    Box(
                        modifier = Modifier.animateItem(
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { if (score.isFree == 0) onNavigateToDetail(score.schoolYear) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val nextYear = score.schoolYear.toInt() + 1
                                    Text(
                                        text = "${score.schoolYear}-${nextYear}学年",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "已测 ${score.done}/${score.nums}",
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                }
                                Text(
                                    text = if (score.isFree == 0) "${score.total}分" else "免测",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}