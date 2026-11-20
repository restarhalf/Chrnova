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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.domain.model.PESubjectScore
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.itemsWithUniqueKeys
import restarhalf.stellar.schedule.ui.icons.Forward
import restarhalf.stellar.schedule.ui.chrome.AppPageScaffold
import restarhalf.stellar.schedule.ui.chrome.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.collapsibleTopInset
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PEDetailScreen(
    vm: PEViewModel,
    schoolYear: String,
    onSubjectClick: (PESubjectScore) -> Unit,
    onBack: () -> Unit,
) {
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

    val nextYear = schoolYear.toIntOrNull()?.plus(1)
    AppPageScaffold(
        title = if (nextYear != null) "${schoolYear}-${nextYear}学年体测成绩" else "${schoolYear}学年体测成绩",
        onBack = onBack,
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
        popupHost = {
        }
    ) { scrollBehavior, paddingValues ->
        PullToRefresh(
            isRefreshing = loading,
            onRefresh = { vm.loadScoreDetail(schoolYear) },
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

                    itemsWithUniqueKeys(
                        data.dataArr,
                        keyOf = { it.subjectId.ifBlank { it.subName } },
                    ) { subject ->
                        Box(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        ) {
                            AppCard(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { onSubjectClick(subject) }
                            ) {
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
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
                                            Icon(
                                                imageVector = Forward,
                                                contentDescription = "查看成绩记录",
                                                tint = colors.onSurfaceVariantSummary,
                                                modifier = Modifier.size(16.dp)
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
