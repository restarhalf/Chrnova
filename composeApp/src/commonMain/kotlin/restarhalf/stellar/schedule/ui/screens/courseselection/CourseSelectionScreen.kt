package restarhalf.stellar.schedule.ui.screens.courseselection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
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
import restarhalf.stellar.schedule.ui.theme.StatusColors
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel.SelectionLog
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel.SelectionTarget
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
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

/**
 * 自动抢课屏幕
 *
 * 采用 Tab 分区布局，每个区域职责单一：
 * - 选课：轮次 → 分类 → 搜索 → 课程列表（加入目标）
 * - 目标：抢课目标列表 + 优先级调整 + 抢课配置
 * - 已选：已选课程列表（退课）
 * - 日志：实时抢课日志
 *
 * 底部固定操作栏让开始/停止按钮永远可达，顶部状态条显示会话与抢课状态。
 *
 * 视觉语言与 EMS / 课程评价一致：
 * - 卡片左侧彩色强调条（按课程名取色，同课程同色）
 * - 列表项入场动画（透明度 + 上浮）
 * - 成功 / 失败 / 警告使用 StatusColors 语义色
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseSelectionScreen(
    vm: CourseSelectionViewModel,
    onBack: () -> Unit,
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val colors = MiuixTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        if (uiState.rotations.isEmpty()) vm.loadRotations()
    }

    val tabCount = 4
    val pagerState = rememberPagerState(pageCount = { tabCount })

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "自动抢课",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Back, contentDescription = "")
                        }
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabRowWithContour(
                        tabs = listOf(
                            "选课",
                            "目标(${uiState.targets.size})",
                            "已选(${uiState.selectedCourses.size})",
                            "日志(${uiState.logs.size})",
                        ),
                        selectedTabIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                    )
                }
                // 顶部状态条
                val statusText = buildStatusText(uiState)
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.surfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                style = MiuixTheme.textStyles.footnote1,
                                text = statusText ?: "",
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            val canStart = uiState.targets.isNotEmpty() && uiState.sessionReady
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 25.dp, start = 10.dp, end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.snatching) {
                    Button(
                        onClick = { vm.stopSnatch() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(
                            text = if (uiState.backgroundRunning) "停止后台抢课" else "停止抢课",
                            color = colors.onPrimary,
                        )
                    }
                } else {
                    Button(
                        onClick = { vm.startSnatch() },
                        modifier = Modifier.weight(1f),
                        enabled = canStart,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(text = "前台抢课", color = colors.onPrimary)
                    }
                    if (uiState.backgroundSupported) {
                        Button(
                            onClick = { ensureNotificationPermission { vm.startBackgroundSnatch() } },
                            modifier = Modifier.weight(1f),
                            enabled = canStart,
                        ) {
                            Text(text = "后台抢课")
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
                        bottom = 0.dp
                    ),
                ),
        ) { pageIndex ->
            when (pageIndex) {
                0 -> SelectTab(
                    uiState = uiState,
                    vm = vm,
                    paddingValues = paddingValues,
                    scrollBehavior = topAppBarScrollBehavior,
                    isRefreshing = uiState.loading,
                    onRefresh = { vm.loadRotations() },
                    pullToRefreshState = pullToRefreshState,
                )
                1 -> TargetTab(
                    uiState = uiState,
                    vm = vm,
                    paddingValues = paddingValues,
                    scrollBehavior = topAppBarScrollBehavior,
                    isRefreshing = uiState.loading,
                    onRefresh = { vm.loadRotations() },
                    pullToRefreshState = pullToRefreshState,
                )
                2 -> SelectedTab(
                    uiState = uiState,
                    vm = vm,
                    paddingValues = paddingValues,
                    scrollBehavior = topAppBarScrollBehavior,
                    isRefreshing = uiState.loadingSelected,
                    onRefresh = { vm.loadSelectedCourses() },
                    pullToRefreshState = pullToRefreshState,
                )
                3 -> LogTab(
                    uiState = uiState,
                    vm = vm,
                    paddingValues = paddingValues,
                    scrollBehavior = topAppBarScrollBehavior,
                    isRefreshing = false,
                    onRefresh = { },
                    pullToRefreshState = pullToRefreshState,
                )
            }
        }
    }
}

/** 顶部状态条文案 */
private fun buildStatusText(uiState: CourseSelectionViewModel.CourseSelectionUiState): String? {
    return when {
        uiState.backgroundRunning -> "后台抢课运行中，可关闭应用或锁屏"
        uiState.snatching -> "前台抢课中..."
        uiState.error.isNotBlank() -> uiState.error
        !uiState.sessionReady && uiState.selectedRotationId.isBlank() -> "请选择选课轮次"
        !uiState.sessionReady -> "正在进入选课..."
        else -> null
    }
}

/** 列表项入场动画：透明度 0→1 + 50dp 上浮，300ms 缓入 */
@Composable
private fun rememberItemEnterAnimation(): Animatable<Float, *> {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(durationMillis = 300))
    }
    return anim
}

/** 卡片左侧彩色强调条（参考 EMS / 评价列表项），调用方需在 RowScope 内传入 align 修饰符 */
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

// ---------------- Tab 1: 选课 ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectTab(
    uiState: CourseSelectionViewModel.CourseSelectionUiState,
    vm: CourseSelectionViewModel,
    paddingValues: PaddingValues,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    pullToRefreshState: PullToRefreshState,
) {
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
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
        // 1. 选课轮次
        item {
            SmallTitle(text = "选课轮次")
            AppCard {
                OverlayDropdownPreference(
                    title = "选课轮次",
                    items = uiState.rotations.map { it.rotationName.ifBlank { it.rotationId } },
                    selectedIndex = uiState.rotations.indexOfFirst { it.rotationId == uiState.selectedRotationId }.coerceAtLeast(0),
                    onSelectedIndexChange = { index ->
                        uiState.rotations.getOrNull(index)?.let { vm.selectRotation(it.rotationId) }
                    },
                )
            }
        }

        // 2. 选课分类
        if (uiState.classifications.isNotEmpty()) {
            item {
                SmallTitle(text = "选课分类")
                AppCard {
                    OverlayDropdownPreference(
                        title = "课程分类",
                        summary = uiState.classifications.firstOrNull { it.classificationCode == uiState.selectedClassificationCode }?.classificationName ?: "请选择",
                        items = uiState.classifications.map { it.classificationName },
                        selectedIndex = uiState.classifications.indexOfFirst { it.classificationCode == uiState.selectedClassificationCode }.coerceAtLeast(0),
                        onSelectedIndexChange = { index ->
                            uiState.classifications.getOrNull(index)?.let { vm.selectClassification(it.classificationCode) }
                        },
                    )
                }
            }
        }

        // 3. 搜索栏
        if (uiState.selectedClassificationCode.isNotBlank()) {
            item {
                SmallTitle(text = "搜索课程")
                AppCard {
                    SearchSection(
                        courseQuery = uiState.courseSearchQuery,
                        loading = uiState.loading,
                        onCourseQueryChange = { vm.onCourseSearchQueryChange(it) },
                        onClear = { vm.clearSearch() },
                    )
                }
            }
        }

        // 4. 课程列表
        if (uiState.courses.isNotEmpty()) {
            item {
                SmallTitle(text = "可选课程（点击卡片看详情，点击「加入」进目标，共 ${uiState.courses.size} 条）")
            }
            items(uiState.courses, key = { "${it.courseId}|${it.noticeId}|${it.kxh}" }) { course ->
                CourseCard(
                    course = course,
                    added = uiState.targets.any { it.key == "${course.courseId}|${course.noticeId}|${course.kxh}" },
                    checking = uiState.checkingTarget,
                    onAdd = { vm.addTargetWithCheck(course) },
                )
            }
        } else if (uiState.selectedClassificationCode.isNotBlank() && !uiState.loading) {
            item {
                EmptyHint(
                    text = if (uiState.courseSearchQuery.isNotBlank()) "未找到匹配的课程" else "该分类下暂无可选课程",
                )
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

// ---------------- Tab 2: 目标 ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TargetTab(
    uiState: CourseSelectionViewModel.CourseSelectionUiState,
    vm: CourseSelectionViewModel,
    paddingValues: PaddingValues,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    pullToRefreshState: PullToRefreshState,
) {
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
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
        if (uiState.targets.isEmpty()) {
            item {
                EmptyHint(text = "还没有抢课目标，去「选课」tab 添加吧")
            }
        } else {
            item {
                SmallTitle(text = "抢课目标（按优先级排序，共 ${uiState.targets.size} 个）")
            }
            itemsIndexed(uiState.targets) { index, target ->
                TargetCard(
                    target = target,
                    index = index,
                    total = uiState.targets.size,
                    snatching = uiState.snatching,
                    onMoveUp = { vm.moveTargetUp(index) },
                    onMoveDown = { vm.moveTargetDown(index) },
                    onRemove = { vm.removeTarget(target) },
                )
            }
        }

        // 抢课配置
        item {
            SmallTitle(text = "抢课配置")
            AppCard {
                SnatchConfigSection(
                    config = uiState.snatchConfig,
                    snatching = uiState.snatching,
                    onConfigChange = { vm.updateSnatchConfig(it) },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

// ---------------- Tab 3: 已选课程（退课） ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectedTab(
    uiState: CourseSelectionViewModel.CourseSelectionUiState,
    vm: CourseSelectionViewModel,
    paddingValues: PaddingValues,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    pullToRefreshState: PullToRefreshState,
) {
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
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
            SmallTitle(text = "已选课程（可退课）")
        }
        if (uiState.selectedCourses.isNotEmpty()) {
            items(uiState.selectedCourses, key = { it.noticeId }) { course ->
                SelectedCourseCard(
                    course = course,
                    onDrop = { vm.dropSelectedCourse(course) },
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
}

// ---------------- Tab 4: 日志 ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogTab(
    uiState: CourseSelectionViewModel.CourseSelectionUiState,
    vm: CourseSelectionViewModel,
    paddingValues: PaddingValues,
    scrollBehavior: ScrollBehavior,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    pullToRefreshState: PullToRefreshState,
) {
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
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
        if (uiState.logs.isNotEmpty()) {
            item {
                SmallTitle(text = "抢课日志")
            }
            items(
                uiState.logs,
                key = { "${it.time}-${it.message.hashCode()}-${uiState.logs.indexOf(it)}" },
            ) { log ->
                LogItem(log)
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.clearLogs() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "清空日志",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            item {
                EmptyHint(text = "暂无日志，启动抢课后会在这里实时显示")
            }
        }
        }
    }
}

// ---------------- 通用小组件 ----------------

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

/**
 * 可选课程卡片。
 *
 * 视觉参考 EMS / 评价列表项：左侧彩色强调条 + 入场动画。
 * 交互与旧版不同：整卡点击 = 展开详情；右侧「加入」pill = 加入抢课目标，
 * 避免误触整卡直接发起试探请求。
 */
@Composable
private fun CourseCard(
    course: JwxtSelectionCourse,
    added: Boolean,
    checking: Boolean,
    onAdd: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val animProgress = rememberItemEnterAnimation()
    var expanded by remember { mutableStateOf(false) }
    val accentColor = pickCourseSubColor(course.courseName.ifBlank { course.kxh }, false)
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = 50f * (1f - animProgress.value)
            },
    ) {
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
                // 课程名 + 加入按钮
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
                    AddTargetChip(
                        added = added,
                        checking = checking,
                        onClick = onAdd,
                    )
                }
                // 班次 · 教师
                Text(
                    text = "班次 ${course.kxh}" +
                        (if (course.classTeacher.isNotBlank()) " · ${course.classTeacher}" else ""),
                    style = MiuixTheme.textStyles.footnote1,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // 学分 + 详情切换（点击整行展开）
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
                        Text(text = "地点：$place", style = MiuixTheme.textStyles.footnote1, color = colors.onSurfaceVariantSummary)
                    }
                    val time = course.cleanTime()
                    if (time.isNotBlank()) {
                        Text(text = "时间：$time", style = MiuixTheme.textStyles.footnote1, color = colors.onSurfaceVariantSummary)
                    }
                }
            }
        }
    }
}

/** 「加入」pill：未加入可点，已加入 / 检查中只读展示 */
@Composable
private fun AddTargetChip(
    added: Boolean,
    checking: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    when {
        added -> Text(
            text = "已加入",
            style = MiuixTheme.textStyles.footnote1,
            color = colors.primary,
            fontWeight = FontWeight.Medium,
        )
        checking -> Text(
            text = "检查中…",
            style = MiuixTheme.textStyles.footnote1,
            color = colors.onSurfaceVariantSummary,
        )
        else -> Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(
                text = "＋ 加入",
                style = MiuixTheme.textStyles.footnote1,
                color = colors.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * 抢课目标卡片。
 *
 * 成功的目标：强调条与序号徽标转绿（StatusColors.healthy），整卡正向反馈。
 * 序号徽标强调优先级，操作 chips 置于底部。
 */
@Composable
private fun TargetCard(
    target: SelectionTarget,
    index: Int,
    total: Int,
    snatching: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val animProgress = rememberItemEnterAnimation()
    val course = target.course
    val accentColor = pickCourseSubColor(course.courseName.ifBlank { course.kxh }, false)
    val stateColor = if (target.succeeded) StatusColors.healthy else colors.primary
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = 50f * (1f - animProgress.value)
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccentBar(
                color = if (target.succeeded) StatusColors.healthy else accentColor,
                modifier = Modifier.align(Alignment.CenterVertically),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 序号徽标 + 课程信息 + 成功标识
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 优先级序号徽标
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(stateColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MiuixTheme.textStyles.footnote1,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = course.courseName.ifBlank { "未命名课程" },
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "班次 ${course.kxh}" +
                                (if (course.classTeacher.isNotBlank()) " · ${course.classTeacher}" else ""),
                            style = MiuixTheme.textStyles.footnote1,
                            color = colors.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (target.succeeded) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(StatusColors.healthy)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "成功",
                                style = MiuixTheme.textStyles.footnote1,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                            )
                        }
                    }
                }

                // 尝试次数 + 最近状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "尝试 ${target.attempts} 次",
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceVariantSummary,
                    )
                    if (target.lastMessage.isNotBlank()) {
                        Text(
                            text = target.lastMessage,
                            style = MiuixTheme.textStyles.footnote1,
                            color = when {
                                target.succeeded -> StatusColors.healthy
                                else -> colors.onSurfaceVariantSummary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                    }
                }

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ActionChip(text = "上移", enabled = index > 0 && !snatching, onClick = onMoveUp)
                    ActionChip(text = "下移", enabled = index < total - 1 && !snatching, onClick = onMoveDown)
                    ActionChip(text = "移除", enabled = !snatching, onClick = onRemove, destructive = true)
                }
            }
        }
    }
}

/** 已选课程卡片（用于退课），视觉与 CourseCard 一致 */
@Composable
private fun SelectedCourseCard(
    course: JwxtSelectedCourse,
    onDrop: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val animProgress = rememberItemEnterAnimation()
    var expanded by remember { mutableStateOf(false) }
    val accentColor = pickCourseSubColor(course.courseName.ifBlank { course.noticeId }, false)
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = 50f * (1f - animProgress.value)
            },
    ) {
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
                        ActionChip(text = "退课", enabled = true, onClick = onDrop, destructive = true)
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
                        Text(text = "地点：$place", style = MiuixTheme.textStyles.footnote1, color = colors.onSurfaceVariantSummary)
                    }
                    val time = course.cleanTime()
                    if (time.isNotBlank()) {
                        Text(text = "时间：$time", style = MiuixTheme.textStyles.footnote1, color = colors.onSurfaceVariantSummary)
                    }
                }
            }
        }
    }
}

/** 小操作按钮 */
@Composable
private fun ActionChip(
    text: String,
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
        else -> colors.onSurfaceContainer
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MiuixTheme.textStyles.footnote1, color = fg)
    }
}

/** 搜索栏：课程名称，带清空按钮 */
@Composable
private fun SearchSection(
    courseQuery: String,
    loading: Boolean,
    onCourseQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val hasFilter = courseQuery.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            label = "输入课程关键词",
            value = courseQuery,
            onValueChange = onCourseQueryChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                Text(text = "搜索中...", style = MiuixTheme.textStyles.footnote1, color = colors.primary)
            } else if (hasFilter) {
                Text(text = "已筛选", style = MiuixTheme.textStyles.footnote1, color = colors.primary)
            } else {
                Spacer(modifier = Modifier.size(0.dp))
            }
            if (hasFilter) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh)
                        .clickable { onClear() }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(text = "清除筛选", style = MiuixTheme.textStyles.footnote1, color = colors.onSurfaceContainer)
                }
            }
        }
    }
}

/**
 * 抢课配置区。
 *
 * 两组输入之间用分隔线隔开，每组带辅助说明文字；单位直接标在标签上，
 * 避免用户猜「毫秒」「次数」的含义。
 */
@Composable
private fun SnatchConfigSection(
    config: CourseSelectionViewModel.SnatchConfig,
    snatching: Boolean,
    onConfigChange: (CourseSelectionViewModel.SnatchConfig) -> Unit,
) {
    var intervalText by remember(config.intervalMs) {
        mutableStateOf(config.intervalMs.toString())
    }
    var maxAttemptsText by remember(config.maxAttempts) {
        mutableStateOf(config.maxAttempts.toString())
    }
    val summaryColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 组 1：请求间隔
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "请求间隔",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            TextField(
                value = intervalText,
                onValueChange = { value ->
                    intervalText = value.filter { it.isDigit() }
                    intervalText.toLongOrNull()?.let { ms ->
                        onConfigChange(config.copy(intervalMs = ms.coerceAtLeast(100L)))
                    }
                },
                enabled = !snatching,
                modifier = Modifier.fillMaxWidth(),
                label = "毫秒",
            )
            Text(
                text = "建议 500-2000ms，过快可能触发教务系统风控",
                style = MiuixTheme.textStyles.footnote1,
                color = summaryColor,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 组 2：最大尝试次数
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "最大尝试次数",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            TextField(
                value = maxAttemptsText,
                onValueChange = { value ->
                    maxAttemptsText = value.filter { it.isDigit() }
                    maxAttemptsText.toIntOrNull()?.let { n ->
                        onConfigChange(config.copy(maxAttempts = n.coerceAtLeast(0)))
                    }
                },
                enabled = !snatching,
                modifier = Modifier.fillMaxWidth(),
                label = "次",
            )
            Text(
                text = "0 = 无限重试，直到成功或手动停止",
                style = MiuixTheme.textStyles.footnote1,
                color = summaryColor,
            )
        }
    }
}

/** 日志条目：级别彩色圆点 + 时间戳 + 等宽消息 */
@Composable
private fun LogItem(log: SelectionLog) {
    val colors = MiuixTheme.colorScheme
    val dotColor = when (log.level) {
        SelectionLog.LogLevel.SUCCESS -> StatusColors.healthy
        SelectionLog.LogLevel.ERROR -> StatusColors.danger
        SelectionLog.LogLevel.WARN -> StatusColors.warning
        SelectionLog.LogLevel.INFO -> StatusColors.neutral
    }
    val textColor = when (log.level) {
        SelectionLog.LogLevel.SUCCESS -> StatusColors.healthy
        SelectionLog.LogLevel.ERROR -> StatusColors.danger
        SelectionLog.LogLevel.WARN -> colors.secondary
        SelectionLog.LogLevel.INFO -> colors.onSurfaceVariantSummary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = log.time,
            style = MiuixTheme.textStyles.footnote1,
            color = colors.onSurfaceVariantSummary,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = log.message,
            style = MiuixTheme.textStyles.footnote1,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 学分文案：空值显示占位符 */
private fun JwxtSelectionCourse.creditText(): String =
    if (credit.isBlank()) "学分 -" else "学分 ${credit.trim()}"

/** 学分文案：空值显示占位符 */
private fun JwxtSelectedCourse.creditText(): String =
    if (credit.isBlank()) "学分 -" else "学分 ${credit.trim()}"
