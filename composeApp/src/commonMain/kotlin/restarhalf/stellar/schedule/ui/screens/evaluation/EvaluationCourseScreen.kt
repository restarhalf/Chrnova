package restarhalf.stellar.schedule.ui.screens.evaluation

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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.core.text.DecimalFormatter
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 某课程的评价列表页（两层结构的第二层）。
 *
 * 顶部显示课程聚合信息（平均分 + 评价数），下方为该课程的所有评价。
 */
@Composable
fun EvaluationCourseScreen(
    vm: CourseEvaluationViewModel,
    courseName: String,
    teacher: String,
    onBack: () -> Unit,
    onEvaluationDetail: (String) -> Unit,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(courseName, teacher) {
        vm.loadEvaluations(course = courseName, teacher = teacher.ifBlank { null })
        // 兜底：若聚合列表为空（如深链接直接进入），加载一次用于顶部摘要
        if (uiState.courseSummaries.isEmpty()) {
            vm.loadCourseSummaries()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = courseName.ifEmpty { "课程评价" },
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Back, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = uiState.loading,
            onRefresh = { vm.loadEvaluations() },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
            modifier = Modifier.fillMaxSize().padding(
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = paddingValues.calculateBottomPadding(),
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 顶部聚合卡片
                item(key = "summary") {
                    val summary = uiState.courseSummaries.firstOrNull {
                        it.courseName == courseName && it.teacher == teacher
                    }
                    AppCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = courseName,
                                fontSize = 18.sp,
                                color = colors.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (teacher.isNotBlank()) {
                                Text(
                                    text = "教师：$teacher",
                                    fontSize = 13.sp,
                                    color = colors.onSurfaceVariantSummary,
                                )
                            }
                            if (summary != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = DecimalFormatter.format(summary.avgRating, 1),
                                        fontSize = 28.sp,
                                        color = colors.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        StarRatingDisplay(
                                            rating = summary.avgRating.toInt().coerceIn(0, 5),
                                            starSize = 14,
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${summary.evalCount} 条评价",
                                            fontSize = 12.sp,
                                            color = colors.onSurfaceVariantSummary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!uiState.loading && uiState.filteredEvaluations.isEmpty()) {
                    item(key = "empty") {
                        EvaluationEmptyState(
                            title = "暂无评价",
                            subtitle = "做第一个评价这门课的人吧",
                        )
                    }
                }

                items(uiState.filteredEvaluations, key = { it.id }) { evaluation ->
                    EvaluationListItem(
                        evaluation = evaluation,
                        onClick = { onEvaluationDetail(evaluation.id) },
                        onLike = { vm.toggleLike(evaluation.id) },
                    )
                }
            }
        }
    }
}
