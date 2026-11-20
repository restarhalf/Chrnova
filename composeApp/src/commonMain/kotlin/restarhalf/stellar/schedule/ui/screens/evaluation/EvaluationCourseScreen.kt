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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.ui.chrome.AppPageScaffold
import restarhalf.stellar.schedule.ui.chrome.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.layout.collapsibleTopInset
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EvaluationCourseScreen(
    vm: CourseEvaluationViewModel,
    courseName: String,
    teacher: String,
    onBack: () -> Unit,
    onEvaluationDetail: (String) -> Unit,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val pullToRefreshState = rememberPullToRefreshState()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(courseName, teacher) {
        vm.loadEvaluations(course = courseName, teacher = teacher.ifBlank { null })

        if (uiState.courseSummaries.isEmpty()) {
            vm.loadCourseSummaries()
        }
    }

    AppPageScaffold(
        title = courseName.ifEmpty { "课程评价" },
        onBack = onBack,
        topBarExtra = {
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                EvaluationStatusPill(text = uiState.error)
            }
        },
    ) { scrollBehavior, paddingValues ->
        PullToRefresh(
            isRefreshing = uiState.loading,
            onRefresh = { vm.loadEvaluations() },
            pullToRefreshState = pullToRefreshState,
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
            modifier = Modifier.fillMaxSize().collapsibleTopInset(scrollBehavior)
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {

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
