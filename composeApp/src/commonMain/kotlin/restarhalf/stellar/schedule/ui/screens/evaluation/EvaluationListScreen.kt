package restarhalf.stellar.schedule.ui.screens.evaluation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Add
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.chrome.AppPageTopBar
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.chrome.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EvaluationListScreen(
    vm: CourseEvaluationViewModel,
    onBack: () -> Unit,
    onCourseClick: (courseName: String, teacher: String) -> Unit,
    onEvaluationDetail: (String) -> Unit,
    onSubmitClick: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val colors = MiuixTheme.colorScheme

    val selectedTab by remember(pagerState) {
        derivedStateOf { pagerState.currentPage }
    }

    LaunchedEffect(Unit) {
        vm.loadCourseSummaries()

        vm.loadEvaluations()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "课程评价",
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
                    actions = {
                        IconButton(onClick = onSubmitClick) {
                            Icon(
                                imageVector = Add,
                                contentDescription = "写评价",
                                tint = colors.onBackground,
                            )
                        }
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabRowWithContour(
                        tabs = listOf("全部评价", "我的评价"),
                        selectedTabIndex = selectedTab,
                        onTabSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                    )
                }

                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    EvaluationStatusPill(text = uiState.error)
                }
            }
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = 0.dp,
                )
            ),
        ) { pageIndex ->
            when (pageIndex) {
                0 -> AllEvaluationsPage(
                    vm = vm,
                    topAppBarScrollBehavior = topAppBarScrollBehavior,
                    onCourseClick = onCourseClick,
                    outerPadding = paddingValues,
                )
                1 -> MyEvaluationsPage(
                    vm = vm,
                    topAppBarScrollBehavior = topAppBarScrollBehavior,
                    onEvaluationDetail = onEvaluationDetail,
                    outerPadding = paddingValues,
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllEvaluationsPage(
    vm: CourseEvaluationViewModel,
    topAppBarScrollBehavior: ScrollBehavior,
    onCourseClick: (courseName: String, teacher: String) -> Unit,
    outerPadding: PaddingValues,
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefresh(
        isRefreshing = uiState.loading,
        onRefresh = {
            vm.loadCourseSummaries()
            vm.loadEvaluations()
        },
        pullToRefreshState = pullToRefreshState,
        refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = PaddingValues(),
                outerPadding = outerPadding,
                extraTop = 12.dp,
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "search_bar") {
                AppCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            label = "搜索课程 / 教师",
                            value = uiState.searchQuery,
                            onValueChange = { vm.onSearchQueryChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            val items = uiState.filteredCourseSummaries
            items(items, key = { "${it.courseName}|${it.teacher}" }) { summary ->
                Box(
                    modifier = Modifier.animateItem(
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ),
                ) {
                    CourseSummaryCard(
                        summary = summary,
                        onClick = { onCourseClick(summary.courseName, summary.teacher) },
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MyEvaluationsPage(
    vm: CourseEvaluationViewModel,
    topAppBarScrollBehavior: ScrollBehavior,
    onEvaluationDetail: (String) -> Unit,
    outerPadding: PaddingValues,
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()


    LaunchedEffect(Unit) {
        if (!uiState.onlyMine) vm.setOnlyMine(true)
    }

    PullToRefresh(
        isRefreshing = uiState.loading,
        onRefresh = { vm.loadEvaluations() },
        pullToRefreshState = pullToRefreshState,
        refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = PaddingValues(),
                outerPadding = outerPadding,
                extraTop = 12.dp,
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "search_bar") {
                AppCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            label = "搜索课程 / 内容",
                            value = uiState.searchQuery,
                            onValueChange = { vm.onSearchQueryChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            val items = uiState.filteredEvaluations
            items(items, key = { it.id }) { evaluation ->
                Box(
                    modifier = Modifier.animateItem(
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ),
                ) {
                    EvaluationListItem(
                        evaluation = evaluation,
                        onClick = { onEvaluationDetail(evaluation.id) },
                        onLike = { vm.toggleLike(evaluation.id) },
                        showCourseName = true,
                        isMine = true,
                    )
                }
            }
        }
    }
}
