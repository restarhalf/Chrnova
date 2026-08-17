package restarhalf.stellar.schedule.ui.screens.pe

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 体育成绩详情屏幕
 * 
 * 显示指定学年的体测详情，包括：
 * - 各项体测成绩
 * - 成绩等级
 * - 总分和总等级
 * 
 * @param schoolYear 学年
 * @param onBack 返回回调
 */
@Composable
fun PEDetailScreen(
    vm: PEViewModel,
    schoolYear: String,
    onBack: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val detailData = uiState.detailData
    val loading = uiState.loading
    val statusText = vm.buildStatusText(isDetail = true)
    val colors = MiuixTheme.colorScheme
    val loggedIn by vm.isLoggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(schoolYear, loggedIn) {
        if (loggedIn) {
            vm.observeCachedDetailData(schoolYear)
            vm.loadScoreDetail(schoolYear)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                val nextYear=schoolYear.toInt()+1
                AppPageTopBar(
                    title = "${schoolYear}-${nextYear}学年体测成绩",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,

                        ){
                            Icon(
                                imageVector = Back,
                                contentDescription = "返回"
                            )
                        }
                    }
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
                            Text(style = MiuixTheme.textStyles.footnote1, text = statusText ?: "")
                        }
                    }
                }
            }
        },
        popupHost = {
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = loading,
            onRefresh = { vm.loadScoreDetail(schoolYear) },
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
                        extraStart = 12.dp,
                        extraEnd = 12.dp,
                    ),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    detailData?.let { data ->
                    item {
                        Box(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        ) {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "总分", style = MiuixTheme.textStyles.body1)
                                        Text(
                                            text = "${data.totalScore}分",
                                            style = MiuixTheme.textStyles.title4,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "等级", style = MiuixTheme.textStyles.body1)
                                        Text(
                                            text = data.totalGrade,
                                            style = MiuixTheme.textStyles.body1,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(data.dataArr, key = { it.subjectId }) { subject ->
                        Box(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        ) {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = subject.subName,
                                            style = MiuixTheme.textStyles.body1,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (subject.isJoin == 1) {
                                            Text(
                                                text = "${subject.score ?: "--"}分",
                                                style = MiuixTheme.textStyles.title4,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(
                                                text = "未参加",
                                                style = MiuixTheme.textStyles.body2,
                                                color = colors.onSurfaceVariantSummary
                                            )
                                        }
                                    }
                                    if (subject.isJoin == 1) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "成绩",
                                                style = MiuixTheme.textStyles.body2,
                                                color = colors.onSurfaceVariantSummary
                                            )
                                            Text(
                                                text = "${subject.result ?: "--"}${subject.unit}",
                                                style = MiuixTheme.textStyles.body2
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "等级",
                                                style = MiuixTheme.textStyles.body2,
                                                color = colors.onSurfaceVariantSummary
                                            )
                                            Text(text = subject.grade ?: "--", style = MiuixTheme.textStyles.body2)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "占比",
                                            style = MiuixTheme.textStyles.body2,
                                            color = colors.onSurfaceVariantSummary
                                        )
                                        Text(text = "${subject.subRatio}%", style = MiuixTheme.textStyles.body2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

