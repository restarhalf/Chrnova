package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.animation.AnimatedVisibility
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
 * 单科体测成绩历史屏幕
 *
 * 显示某学年中单个体测科目的全部历史成绩记录：
 * - 每次测试的成绩与时间
 * - 所属考核场次
 * - 详情页当前采用成绩的标记
 *
 * @param schoolYear 学年
 * @param subjectId 科目ID
 * @param subjectName 科目名称
 * @param unit 成绩单位
 * @param currentResult 详情页当前采用的成绩
 * @param onBack 返回回调
 */
@Composable
fun PESubjectHistoryScreen(
    vm: PEViewModel,
    schoolYear: String,
    subjectId: String,
    subjectName: String,
    unit: String,
    currentResult: String?,
    onBack: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val statusText = vm.buildSubjectHistoryStatusText()
    val colors = MiuixTheme.colorScheme
    val loggedIn by vm.isLoggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(schoolYear, subjectId, loggedIn) {
        if (loggedIn) {
            vm.loadSubjectHistory(
                schoolYear = schoolYear,
                subjectId = subjectId,
                subjectName = subjectName,
                unit = unit,
                currentResult = currentResult,
            )
        }
    }

    // 只采用与路由科目一致的历史状态，避免短暂显示上一次科目的记录
    val history = uiState.subjectHistory?.takeIf { it.subjectId == subjectId }
    // 详情页当前采用的成绩在历史记录中最新的那条（与 currentResult 数值一致）
    val currentSourceId = currentResult
        ?.takeIf { it.isNotBlank() }
        ?.let { result -> history?.records?.firstOrNull { it.result == result }?.sourceScoreId }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "${subjectName}成绩记录",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                        ) {
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
            isRefreshing = history?.loading == true,
            onRefresh = {
                vm.loadSubjectHistory(
                    schoolYear = schoolYear,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    unit = unit,
                    currentResult = currentResult,
                )
            },
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
                history?.let { data ->
                    if (data.records.isNotEmpty()) {
                        item {
                            Text(
                                text = "共${data.records.size}条记录 · 按时间倒序",
                                style = MiuixTheme.textStyles.footnote1,
                                color = colors.onSurfaceVariantSummary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    items(data.records, key = { it.sourceScoreId }) { record ->
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
                                        text = "${record.result ?: "--"}${data.unit}",
                                        style = MiuixTheme.textStyles.title4,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (record.sourceScoreId == currentSourceId) {
                                            Box(
                                                modifier = Modifier.clip(CircleShape)
                                                    .background(colors.primary.copy(alpha = 0.12f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "当前成绩",
                                                    style = MiuixTheme.textStyles.footnote1,
                                                    color = colors.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = record.scoreTime,
                                            style = MiuixTheme.textStyles.body2,
                                            color = colors.onSurfaceVariantSummary
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "考核场次",
                                        style = MiuixTheme.textStyles.body2,
                                        color = colors.onSurfaceVariantSummary
                                    )
                                    Text(
                                        text = record.sessionName,
                                        style = MiuixTheme.textStyles.body2,
                                        modifier = Modifier.padding(start = 16.dp)
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
