package restarhalf.stellar.schedule.ui.screens.evaluation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 某课程的评价列表页（两层结构的第二层）。
 *
 * 设计要点（参考 EMS）：
 * - 顶栏：返回 + 课程名标题
 * - 顶部错误状态药丸
 * - Hero 卡片：左侧彩色强调条 + 课程名 + 教师 + 大数字平均分 + 半星 + 评价数
 * - 列表项：复用 EvaluationListItem（带入场动画 + 强调条）
 * - PullToRefresh
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
            Column {
                AppPageTopBar(
                    title = courseName.ifEmpty { "课程评价" },
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Back,
                                contentDescription = "返回",
                                tint = colors.onBackground,
                            )
                        }
                    },
                )
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    EvaluationStatusPill(text = uiState.error)
                }
            }
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
                // 顶部 Hero 卡片
                item(key = "hero") {
                    val summary = uiState.courseSummaries.firstOrNull {
                        it.courseName == courseName && it.teacher == teacher
                    }
                    CourseHeroCard(
                        courseName = courseName,
                        teacher = teacher,
                        summary = summary,
                    )
                }

                items(uiState.filteredEvaluations, key = { it.id }) { evaluation ->
                    Box(
                        modifier = Modifier.animateItem(
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                    ) {
                        EvaluationListItem(
                            evaluation = evaluation,
                            onClick = { onEvaluationDetail(evaluation.id) },
                            onLike = { vm.toggleLike(evaluation.id) },
                            isMine = evaluation.userHash == uiState.userHash && uiState.userHash.isNotBlank(),
                        )
                    }
                }
            }
        }
    }
}