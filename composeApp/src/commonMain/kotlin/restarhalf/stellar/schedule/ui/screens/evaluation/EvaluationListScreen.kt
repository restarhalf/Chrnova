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
import androidx.compose.runtime.remember
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
import restarhalf.stellar.schedule.domain.model.Evaluation
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
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EvaluationListScreen(
    vm: CourseEvaluationViewModel,
    onBack: () -> Unit,
    onEvaluationDetail: (String) -> Unit,
    onSubmitClick: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(Unit) {
        vm.loadEvaluations()
    }

    val courseOptions = remember(uiState.myCourses) {
        buildList {
            add("全部课程")
            uiState.myCourses.map { it.name }.distinct().forEach { add(it) }
        }
    }
    val selectedCourseIndex = remember(uiState.selectedCourse, courseOptions) {
        val idx = courseOptions.indexOf(uiState.selectedCourse)
        if (idx < 0) 0 else idx
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
            onRefresh = { vm.loadEvaluations() },
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "filter_bar") {
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TextField(
                                label = "搜索课程 / 内容",
                                value = uiState.searchQuery,
                                onValueChange = { vm.onSearchQueryChange(it) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            HorizontalDivider()
                            OverlayDropdownPreference(
                                title = "按课程筛选",
                                summary = uiState.selectedCourse ?: "全部课程",
                                items = courseOptions,
                                selectedIndex = selectedCourseIndex,
                                onSelectedIndexChange = { index ->
                                    val course = if (index <= 0) null else courseOptions[index]
                                    vm.setCourseFilter(course)
                                },
                            )
                        }
                    }
                }

                if (!uiState.loading && uiState.filteredEvaluations.isEmpty()) {
                    item(key = "empty") {
                        EvaluationEmptyState(
                            title = "暂无评价",
                            subtitle = "下拉刷新或写一条新评价吧",
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

@Composable
private fun EvaluationListItem(
    evaluation: Evaluation,
    onClick: () -> Unit,
    onLike: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 标题行：课程名 + 状态徽标
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = evaluation.courseName.ifEmpty { "未命名课程" },
                    fontSize = 15.sp,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            // 副标题行：教师 + 星级
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = evaluation.teacher.ifEmpty { "教师未知" },
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StarRatingDisplay(rating = evaluation.rating)
            }
            // 内容预览
            if (evaluation.content.isNotBlank()) {
                Text(
                    text = evaluation.content,
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider()
            // 底部元信息：作者 + 点赞
            EvaluationMetaRow(
                authorText = if (evaluation.anonymous) "匿名" else evaluation.author.ifEmpty { "匿名" },
            ) {
                LikeButton(
                    liked = evaluation.liked,
                    likes = evaluation.likes,
                    onClick = onLike,
                )
            }
        }
    }
}
