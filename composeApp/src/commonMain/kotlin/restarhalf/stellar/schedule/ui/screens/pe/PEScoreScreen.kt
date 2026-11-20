package restarhalf.stellar.schedule.ui.screens.pe

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.itemsWithUniqueKeys
import restarhalf.stellar.schedule.ui.icons.Appointment
import restarhalf.stellar.schedule.ui.icons.Background
import restarhalf.stellar.schedule.ui.icons.Free
import restarhalf.stellar.schedule.ui.icons.QrCode
import restarhalf.stellar.schedule.ui.chrome.AppPageScaffold
import restarhalf.stellar.schedule.ui.chrome.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.components.topbar.AppMoreMenuItem
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.layout.collapsibleTopInset
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PEScoreScreen(
    vm: PEViewModel,
    onNavigateToDetail: (String) -> Unit,
    onLogin: () -> Unit,
    onQRCode: () -> Unit = {},
    onAppointment: () -> Unit = {},
    onFreeApply: () -> Unit = {},
    onCustomize: () -> Unit = {},
    jwxtAuthProfile: JwxtAuthProfile? = null,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val yearScores = uiState.yearScores
    val loading = uiState.loading
    val loggedIn by vm.isLoggedIn.collectAsStateWithLifecycle()
    val hasQRCodeInfo =
        loggedIn || (jwxtAuthProfile?.userNo?.isNotBlank() == true && jwxtAuthProfile.name.isNotBlank())
    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            vm.loadScoreList()
            vm.loadProfile()
        }
    }
    val statusText = vm.buildStatusText()
    val colors = MiuixTheme.colorScheme

    AppPageScaffold(
        title = "体测",
        moreMenuItems =
            buildList {
                add(AppMoreMenuItem(text = "体测预约", icon = Appointment, onClick = onAppointment))
                add(AppMoreMenuItem(text = "申请免修", icon = Free, onClick = onFreeApply))
                if (hasQRCodeInfo) {
                    add(AppMoreMenuItem(text = "体测二维码", icon = QrCode, onClick = onQRCode))
                }
                add(AppMoreMenuItem(text = "软件外观", icon = Background, onClick = onCustomize))
            },
        topBarExtra = {
            Column {
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
                            Text(style = MiuixTheme.textStyles.footnote1, text = statusText ?: "")
                        }
                    }
                }
            }
        },
    ) { scrollBehavior, paddingValues ->
        PullToRefresh(
            isRefreshing = loading,
            onRefresh = { vm.loadScoreList() },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
            modifier = Modifier.fillMaxSize()
                .collapsibleTopInset(scrollBehavior)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .pageScrollModifiers(scrollBehavior = scrollBehavior),
                contentPadding = appPageContentPadding(
                    innerPadding = paddingValues,
                    outerPadding = appScaffoldPadding,
                    extraTop = 12.dp,
                    extraStart = 12.dp,
                    extraEnd = 12.dp,
                ),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                itemsWithUniqueKeys(yearScores, keyOf = { it.schoolYear }) { score ->
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
                                    val nextYear = score.schoolYear.toIntOrNull()?.plus(1)
                                    Text(
                                        text = if (nextYear != null) "${score.schoolYear}-${nextYear}学年" else "${score.schoolYear}学年",
                                        style = MiuixTheme.textStyles.body1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "已测 ${score.done}/${score.nums}",
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = colors.onSurfaceVariantSummary
                                    )
                                }
                                Text(
                                    text = if (score.isFree == 0) "${score.total}分" else "免测",
                                    style = MiuixTheme.textStyles.title4,
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
