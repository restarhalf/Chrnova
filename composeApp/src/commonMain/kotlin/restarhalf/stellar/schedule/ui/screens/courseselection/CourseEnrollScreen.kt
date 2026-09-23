package restarhalf.stellar.schedule.ui.screens.courseselection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.data.remote.JwxtSelectedCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import restarhalf.stellar.schedule.ui.viewmodel.CourseEnrollViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.PullToRefreshState
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 手动选课屏幕
 *
 * 两个 Tab：
 * - 可选：轮次 → 分类 → 搜索 → 课程列表（点击「选课」提交一次）
 * - 已选：已选课程列表（可退课）
 *
 * 不包含自动抢课的目标队列与循环提交。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseEnrollScreen(
    vm: CourseEnrollViewModel,
    onBack: () -> Unit,
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val colors = MiuixTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        if (uiState.rotations.isEmpty()) vm.loadRotations()
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val statusText = buildEnrollStatusText(uiState)
    val scrollBehavior = topAppBarScrollBehavior

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "选课",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Back, contentDescription = "返回")
                        }
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabRowWithContour(
                        tabs = listOf(
                            "可选",
                            "已选(${uiState.selectedCourses.size})",
                        ),
                        selectedTabIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                    )
                }
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isError = uiState.error.isNotBlank()
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isError) colors.errorContainer else colors.surfaceContainerHigh
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                style = MiuixTheme.textStyles.footnote1,
                                text = statusText ?: "",
                                color = if (isError) colors.onErrorContainer else colors.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = 0.dp,
                    ),
                ),
        ) { pageIndex ->
            when (pageIndex) {
                0 -> AvailableTab(
                    uiState = uiState,
                    vm = vm,
                    paddingValues = paddingValues,
                    pullToRefreshState = pullToRefreshState,
                    scrollBehavior = scrollBehavior,
                )
                1 -> EnrolledTab(
                    uiState = uiState,
                    vm = vm,
                    paddingValues = paddingValues,
                    pullToRefreshState = pullToRefreshState,
                    scrollBehavior = scrollBehavior,
                )
            }
        }
    }
}

private fun buildEnrollStatusText(uiState: CourseEnrollViewModel.UiState): String? {
    return when {
        uiState.error.isNotBlank() -> uiState.error
        uiState.notice.isNotBlank() -> uiState.notice
        uiState.submitting -> "提交中..."
        !uiState.sessionReady && uiState.selectedRotationId.isBlank() -> "请选择选课轮次"
        !uiState.sessionReady -> "正在进入选课..."
        else -> null
    }
}

// ---------------- 可选课程 ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AvailableTab(
    uiState: CourseEnrollViewModel.UiState,
    vm: CourseEnrollViewModel,
    paddingValues: PaddingValues,
    pullToRefreshState: PullToRefreshState,
    scrollBehavior: ScrollBehavior,
) {
    PullToRefresh(
        isRefreshing = uiState.loading,
        onRefresh = { vm.loadRotations() },
        pullToRefreshState = pullToRefreshState,
        refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pageScrollModifiers(scrollBehavior = scrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = PaddingValues(),
                outerPadding = paddingValues,
                extraTop = 12.dp,
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SmallTitle(text = "选课轮次")
                AppCard {
                    OverlayDropdownPreference(
                        title = "选课轮次",
                        items = uiState.rotations.map { it.rotationName.ifBlank { it.rotationId } },
                        selectedIndex = uiState.rotations
                            .indexOfFirst { it.rotationId == uiState.selectedRotationId }
                            .coerceAtLeast(0),
                        onSelectedIndexChange = { index ->
                            uiState.rotations.getOrNull(index)?.let { vm.selectRotation(it.rotationId) }
                        },
                    )
                }
            }

            if (uiState.classifications.isNotEmpty()) {
                item {
                    SmallTitle(text = "课程分类")
                    AppCard {
                        OverlayDropdownPreference(
                            title = "课程分类",
                            summary = uiState.classifications
                                .firstOrNull { it.classificationCode == uiState.selectedClassificationCode }
                                ?.classificationName ?: "请选择",
                            items = uiState.classifications.map { it.classificationName },
                            selectedIndex = uiState.classifications
                                .indexOfFirst { it.classificationCode == uiState.selectedClassificationCode }
                                .coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                uiState.classifications.getOrNull(index)
                                    ?.let { vm.selectClassification(it.classificationCode) }
                            },
                        )
                    }
                }
            }

            if (uiState.selectedClassificationCode.isNotBlank()) {
                item {
                    SmallTitle(text = "搜索课程")
                    AppCard {
                        EnrollSearchBar(
                            query = uiState.searchQuery,
                            loading = uiState.loading,
                            onQueryChange = vm::onSearchQueryChange,
                            onClear = vm::clearSearch,
                        )
                    }
                }
            }

            if (uiState.courses.isNotEmpty()) {
                item {
                    SmallTitle(text = "可选课程（共 ${uiState.courses.size} 条）")
                }
                items(uiState.courses, key = { "${it.courseId}|${it.noticeId}|${it.kxh}" }) { course ->
                    AvailableCourseCard(
                        course = course,
                        submitting = uiState.submitting,
                        onSelect = { vm.selectCourse(course) },
                    )
                }
            } else if (uiState.selectedClassificationCode.isNotBlank() && !uiState.loading) {
                item {
                    EmptyHint(
                        text = if (uiState.searchQuery.isNotBlank()) {
                            "未找到匹配的课程"
                        } else {
                            "该分类下暂无可选课程"
                        },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

// ---------------- 已选课程 ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnrolledTab(
    uiState: CourseEnrollViewModel.UiState,
    vm: CourseEnrollViewModel,
    paddingValues: PaddingValues,
    pullToRefreshState: PullToRefreshState,
    scrollBehavior: ScrollBehavior,
) {
    var pendingDrop by remember { mutableStateOf<JwxtSelectedCourse?>(null) }

    PullToRefresh(
        isRefreshing = uiState.loadingSelected,
        onRefresh = { vm.loadSelectedCourses() },
        pullToRefreshState = pullToRefreshState,
        refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pageScrollModifiers(scrollBehavior = scrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = PaddingValues(),
                outerPadding = paddingValues,
                extraTop = 12.dp,
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SmallTitle(text = "已选课程")
            }
            if (uiState.selectedCourses.isNotEmpty()) {
                items(uiState.selectedCourses, key = { it.noticeId }) { course ->
                    EnrolledCourseCard(
                        course = course,
                        submitting = uiState.submitting,
                        onDrop = { pendingDrop = course },
                    )
                }
            } else if (!uiState.loadingSelected) {
                item {
                    EmptyHint(text = if (uiState.sessionReady) "暂无已选课程" else "请先选择选课轮次")
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    val dropTarget = pendingDrop
    if (dropTarget != null) {
        WindowDialog(
            show = true,
            title = "确认退课",
            summary = "确定要退选「${dropTarget.courseName.ifBlank { "未命名课程" }}」（班次 ${dropTarget.kxh}）吗？退课后如需重选可能受人数限制。",
            onDismissRequest = { pendingDrop = null },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .clickable { pendingDrop = null }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "取消", style = MiuixTheme.textStyles.body2)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.errorContainer)
                        .clickable {
                            vm.dropCourse(dropTarget)
                            pendingDrop = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "退课",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

// ---------------- 小组件 ----------------

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun AccentBar(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .width(4.dp)
            .fillMaxHeight(0.8f),
    )
}

@Composable
private fun AvailableCourseCard(
    course: JwxtSelectionCourse,
    submitting: Boolean,
    onSelect: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val accentColor = pickCourseSubColor(course.courseName.ifBlank { course.kxh }, false)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccentBar(
                color = accentColor,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = course.courseName.ifBlank { "未命名课程" },
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    SelectChip(
                        label = if (submitting) "提交中…" else "选课",
                        enabled = !submitting,
                        onClick = onSelect,
                    )
                }
                Text(
                    text = "班次 ${course.kxh}" +
                        (if (course.classTeacher.isNotBlank()) " · ${course.classTeacher}" else ""),
                    style = MiuixTheme.textStyles.footnote1,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = course.creditText(),
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceVariantSummary,
                    )
                    Text(
                        text = if (expanded) "收起" else "详情",
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.primary,
                    )
                }
                if (expanded) {
                    val place = course.cleanPlace()
                    if (place.isNotBlank()) {
                        Text(
                            text = "地点：$place",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                    val time = course.cleanTime()
                    if (time.isNotBlank()) {
                        Text(
                            text = "时间：$time",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                    if (course.period.isNotBlank()) {
                        Text(
                            text = "学时：${course.period}",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnrolledCourseCard(
    course: JwxtSelectedCourse,
    submitting: Boolean,
    onDrop: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val accentColor = pickCourseSubColor(course.courseName.ifBlank { course.kxh }, false)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccentBar(
                color = accentColor,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = course.courseName.ifBlank { "未命名课程" },
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (course.canDrop) {
                        SelectChip(
                            label = if (submitting) "处理中…" else "退课",
                            enabled = !submitting,
                            destructive = true,
                            onClick = onDrop,
                        )
                    } else {
                        Text(
                            text = "不可退",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
                Text(
                    text = "班次 ${course.kxh}" +
                        (if (course.classTeacher.isNotBlank()) " · ${course.classTeacher}" else ""),
                    style = MiuixTheme.textStyles.footnote1,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = course.creditText(),
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceVariantSummary,
                    )
                    Text(
                        text = if (expanded) "收起" else "详情",
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.primary,
                    )
                }
                if (expanded) {
                    val place = course.cleanPlace()
                    if (place.isNotBlank()) {
                        Text(
                            text = "地点：$place",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                    val time = course.cleanTime()
                    if (time.isNotBlank()) {
                        Text(
                            text = "时间：$time",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectChip(
    label: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val bg = when {
        !enabled -> colors.surfaceContainerHigh
        destructive -> colors.errorContainer
        else -> colors.surfaceContainerHigh
    }
    val fg = when {
        !enabled -> colors.onSurfaceVariantSummary
        destructive -> colors.onErrorContainer
        else -> colors.primary
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = fg,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EnrollSearchBar(
    query: String,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val hasFilter = query.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            label = "输入课程关键词",
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                loading -> Text(text = "搜索中...", style = MiuixTheme.textStyles.footnote1, color = colors.primary)
                hasFilter -> Text(text = "已筛选", style = MiuixTheme.textStyles.footnote1, color = colors.primary)
                else -> Spacer(modifier = Modifier.size(0.dp))
            }
            if (hasFilter) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh)
                        .clickable { onClear() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "清除筛选",
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceContainer,
                    )
                }
            }
        }
    }
}

private fun JwxtSelectionCourse.creditText(): String =
    if (credit.isBlank()) "学分 -" else "学分 ${credit.trim()}"

private fun JwxtSelectedCourse.creditText(): String =
    if (credit.isBlank()) "学分 -" else "学分 ${credit.trim()}"
