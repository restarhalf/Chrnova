package restarhalf.stellar.schedule.ui.screens.evaluation

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.domain.model.CourseEvaluationSummary
import restarhalf.stellar.schedule.core.text.DecimalFormatter
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EvaluationListScreen(
    vm: CourseEvaluationViewModel,
    onBack: () -> Unit,
    onCourseClick: (courseName: String, teacher: String) -> Unit,
    onEvaluationDetail: (String) -> Unit,
    onSubmitClick: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(Unit) {
        vm.loadCourseSummaries()
    }
    // "仅看我的"开启时需要加载评价列表（用于本地过滤出本人提交的）
    LaunchedEffect(uiState.onlyMine) {
        if (uiState.onlyMine) {
            vm.loadEvaluations()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "课程评价", scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Back, contentDescription = "返回")
                        }
                    }
                )
                AnimatedVisibility(
                    visible = uiState.error != null,
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
                            Text(fontSize = 12.sp, text = uiState.error ?: "")
                        }
                    }
                }
            }
        },
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 25.dp, start = 10.dp, end = 10.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = onSubmitClick,
            ) {
                Text(text = "写评价", color = colors.onPrimary)
            }
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = uiState.loading,
            onRefresh = {
                if (uiState.onlyMine) vm.loadEvaluations() else vm.loadCourseSummaries()
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
                    outerPadding = paddingValues,
                    extraTop = 12.dp,
                    extraStart = 12.dp,
                    extraEnd = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "filter_bar") {
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TextField(
                                label = if (uiState.onlyMine) "搜索课程 / 内容"
                                else "搜索课程 / 教师",
                                value = uiState.searchQuery,
                                onValueChange = { vm.onSearchQueryChange(it) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            HorizontalDivider()
                            SwitchPreference(
                                title = "仅看我的评价",
                                summary = "只显示自己提交的评价",
                                checked = uiState.onlyMine,
                                onCheckedChange = { vm.setOnlyMine(it) },
                            )
                        }
                    }
                }

                if (uiState.onlyMine) {
                    // "我的评价"模式：平铺显示本人提交的评价
                    if (!uiState.loading && uiState.filteredEvaluations.isEmpty()) {
                        item(key = "empty_mine") {
                            EvaluationEmptyState(
                                title = "你还没有提交过评价",
                                subtitle = "写第一条评价吧",
                            )
                        }
                    }
                    items(uiState.filteredEvaluations, key = { it.id }) { evaluation ->
                        EvaluationListItem(
                            evaluation = evaluation,
                            onClick = { onEvaluationDetail(evaluation.id) },
                            onLike = { vm.toggleLike(evaluation.id) },
                            showCourseName = true,
                        )
                    }
                } else {
                    // 默认：课程聚合视图
                    if (!uiState.loading && uiState.filteredCourseSummaries.isEmpty()) {
                        item(key = "empty") {
                            EvaluationEmptyState(
                                title = "暂无评价",
                                subtitle = "下拉刷新或写一条新评价吧",
                            )
                        }
                    }
                    items(uiState.filteredCourseSummaries, key = { "${it.courseName}|${it.teacher}" }) { summary ->
                        CourseSummaryItem(
                            summary = summary,
                            onClick = { onCourseClick(summary.courseName, summary.teacher) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 课程聚合卡片：课程名 + 教师 + 平均分（大数字 + 星级）+ 评价数。
 */
@Composable
private fun CourseSummaryItem(
    summary: CourseEvaluationSummary,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 标题行：课程名
            Text(
                text = summary.courseName.ifEmpty { "未命名课程" },
                fontSize = 16.sp,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 评分行：大数字平均分 + 星级 + 评价数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DecimalFormatter.format(summary.avgRating, 1),
                    fontSize = 22.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                StarRatingDisplay(rating = summary.avgRating.toInt().coerceIn(0, 5), starSize = 14)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${summary.evalCount} 条评价",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
            // 教师行
            if (summary.teacher.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "教师",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = summary.teacher,
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
