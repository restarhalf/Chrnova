package restarhalf.stellar.schedule.ui.screens.courseselection

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.data.remote.JwxtSelectedCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.icons.Refresh
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel.SelectionLog
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel.SelectionTarget
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 自动抢课屏幕
 *
 * 提供完整的抢课流程 UI：选课轮次 → 选课分类 → 浏览课程 → 加入抢课目标 →
 * 调整优先级 → 启动/停止抢课 → 查看实时日志。
 */
@Composable
fun CourseSelectionScreen(
    vm: CourseSelectionViewModel,
    onBack: () -> Unit,
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(Unit) {
        if (uiState.rotations.isEmpty()) vm.loadRotations()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "自动抢课",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Back, contentDescription = "")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.loadRotations() }) {
                        Icon(
                            imageVector = Refresh,
                            contentDescription = "刷新"
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = 0.dp,
                    ),
                )
                .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = PaddingValues(),
                outerPadding = appScaffoldPadding,
                extraTop = 12.dp,
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 1. 选课轮次
            item {
                SmallTitle(text = "选课轮次")
                AppCard {
                    OverlayDropdownPreference(
                        title = "选课轮次",
                        summary = if (uiState.selectedRotationId.isBlank()) "请选择" else
                            uiState.rotations.firstOrNull { it.rotationId == uiState.selectedRotationId }?.rotationName.orEmpty(),
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

            // 2.5 搜索栏（选了分类后才显示）
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

            // 3. 课程列表
            if (uiState.courses.isNotEmpty()) {
                item {
                    SmallTitle(text = "可选课程（点击加入抢课目标，共 ${uiState.courses.size} 条）")
                }
                items(uiState.courses, key = { "${it.courseId}|${it.noticeId}|${it.kxh}" }) { course ->
                    CourseCard(
                        course = course,
                        added = uiState.targets.any { it.key == "${course.courseId}|${course.noticeId}|${course.kxh}" },
                        onAdd = { vm.addTarget(course) },
                    )
                }
            } else if (uiState.selectedClassificationCode.isNotBlank() && !uiState.loading) {
                // 已选分类且非加载中，但课程列表为空：提示无结果
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (uiState.courseSearchQuery.isNotBlank()) {
                                "未找到匹配的课程"
                            } else {
                                "该分类下暂无可选课程"
                            },
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }

            // 4. 抢课目标
            if (uiState.targets.isNotEmpty()) {
                item {
                    SmallTitle(text = "抢课目标（按优先级排序，共 ${uiState.targets.size} 个）")
                }
                itemsIndexed(uiState.targets) { index, target ->
                    TargetCard(
                        target = target,
                        index = index,
                        total = uiState.targets.size,
                        snatching = uiState.snatching,
                        sessionReady = uiState.sessionReady,
                        onMoveUp = { vm.moveTargetUp(index) },
                        onMoveDown = { vm.moveTargetDown(index) },
                        onRemove = { vm.removeTarget(target) },
                        onDrop = { vm.dropCourse(target.course.noticeId, target.course.courseName) },
                    )
                }
            }

            // 4.5 已选课程（可退课）
            if (uiState.sessionReady) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SmallTitle(text = "已选课程（可退课）")
                        Text(
                            text = if (uiState.loadingSelected) "加载中..." else "刷新",
                            fontSize = 12.sp,
                            color = colors.primary,
                            modifier = Modifier.clickable(enabled = !uiState.loadingSelected) {
                                vm.loadSelectedCourses()
                            },
                        )
                    }
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "暂无已选课程",
                                fontSize = 13.sp,
                                color = colors.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }

            // 5. 抢课配置
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

            // 6. 开始/停止按钮
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.snatching) {
                        Button(
                            onClick = { vm.stopSnatch() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(color = colors.error),
                        ) {
                            Text(text = "停止抢课", color = colors.onError)
                        }
                    } else {
                        Button(
                            onClick = { vm.startSnatch() },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.targets.isNotEmpty() && uiState.sessionReady,
                            colors = ButtonDefaults.buttonColorsPrimary(),
                        ) {
                            Text(text = "前台抢课", color = colors.onPrimary)
                        }
                        if (uiState.backgroundSupported) {
                            Button(
                                onClick = {
                                    // 后台抢课需要通知权限（Android 13+）
                                    ensureNotificationPermission { vm.startBackgroundSnatch() }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = uiState.targets.isNotEmpty() && uiState.sessionReady,
                                colors = ButtonDefaults.buttonColors(color = colors.secondary),
                            ) {
                                Text(text = "后台抢课", color = colors.onSecondary)
                            }
                        }
                    }
                }
            }
            // 后台抢课运行中提示
            if (uiState.backgroundRunning) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(colors.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "后台抢课服务运行中，可关闭应用或锁屏",
                            fontSize = 12.sp,
                            color = colors.onSecondaryContainer,
                        )
                    }
                }
            }

            // 错误提示
            if (uiState.error.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(colors.errorContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.error,
                            fontSize = 12.sp,
                            color = colors.onErrorContainer,
                        )
                    }
                }
            }

            // 7. 抢课日志
            if (uiState.logs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SmallTitle(text = "抢课日志")
                        Text(
                            text = "清空",
                            fontSize = 12.sp,
                            color = colors.primary,
                            modifier = Modifier.clickable { vm.clearLogs() },
                        )
                    }
                }
                items(uiState.logs, key = { "${it.time}-${it.message.hashCode()}-${uiState.logs.indexOf(it)}" }) { log ->
                    LogItem(log)
                }
            }

            // 底部留白
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

/** 课程列表卡片 */
@Composable
private fun CourseCard(
    course: JwxtSelectionCourse,
    added: Boolean,
    onAdd: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !added) { onAdd() }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.courseName.ifBlank { "未命名课程" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "课程号：${course.courseNumber}  班次：${course.kxh}",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
                if (added) {
                    Text(
                        text = "已加入",
                        fontSize = 12.sp,
                        color = colors.primary,
                    )
                } else {
                    Text(
                        text = "＋ 加入",
                        fontSize = 12.sp,
                        color = colors.primary,
                    )
                }
            }
            if (course.classTeacher.isNotBlank()) {
                Text(
                    text = "教师：${course.classTeacher}",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
            val place = course.cleanPlace()
            if (place.isNotBlank()) {
                Text(
                    text = "地点：$place",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
            val time = course.cleanTime()
            if (time.isNotBlank()) {
                Text(
                    text = "时间：$time",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "学分：${course.credit}",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
                Text(
                    text = "学时：${course.period}",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 抢课目标卡片 */
@Composable
private fun TargetCard(
    target: SelectionTarget,
    index: Int,
    total: Int,
    snatching: Boolean,
    sessionReady: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onDrop: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val course = target.course
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${index + 1}.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Column {
                        Text(
                            text = course.courseName.ifBlank { "未命名课程" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "班次 ${course.kxh} · ${course.classTeacher}",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
                if (target.succeeded) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "✓ 成功",
                            fontSize = 11.sp,
                            color = colors.onPrimary,
                        )
                    }
                }
            }

            // 状态行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "尝试 ${target.attempts} 次",
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariantSummary,
                )
                if (target.lastMessage.isNotBlank()) {
                    Text(
                        text = target.lastMessage,
                        fontSize = 11.sp,
                        color = if (target.succeeded) colors.primary else colors.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ActionChip(text = "↑", enabled = index > 0 && !snatching, onClick = onMoveUp)
                ActionChip(text = "↓", enabled = index < total - 1 && !snatching, onClick = onMoveDown)
                ActionChip(text = "退课", enabled = !snatching && sessionReady, onClick = onDrop, destructive = true)
                ActionChip(text = "移除", enabled = !snatching, onClick = onRemove, destructive = true)
            }
        }
    }
}

/** 已选课程卡片（用于退课） */
@Composable
private fun SelectedCourseCard(
    course: JwxtSelectedCourse,
    onDrop: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.courseName.ifBlank { "未命名课程" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "课程号：${course.courseNumber}  班次：${course.kxh}",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
                if (course.canDrop) {
                    ActionChip(text = "退课", enabled = true, onClick = onDrop, destructive = true)
                } else {
                    Text(
                        text = "不可退",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }
            if (course.classTeacher.isNotBlank()) {
                Text(
                    text = "教师：${course.classTeacher}",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
            val place = course.cleanPlace()
            if (place.isNotBlank()) {
                Text(
                    text = "地点：$place",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
            val time = course.cleanTime()
            if (time.isNotBlank()) {
                Text(
                    text = "时间：$time",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "学分：${course.credit.trim()}",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
                Text(
                    text = "学时：${course.period.trim()}",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                )
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
        Text(text = text, fontSize = 12.sp, color = fg)
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
                Text(
                    text = "搜索中...",
                    fontSize = 12.sp,
                    color = colors.primary,
                )
            } else if (hasFilter) {
                Text(
                    text = "已筛选",
                    fontSize = 12.sp,
                    color = colors.primary,
                )
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
                    Text(
                        text = "清除筛选",
                        fontSize = 12.sp,
                        color = colors.onSurfaceContainer,
                    )
                }
            }
        }
    }
}

/** 抢课配置区 */
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
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "请求间隔（毫秒，建议 500-2000）",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "最大尝试次数（0 = 无限，直到成功或手动停止）",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
        )
    }
}

/** 日志条目 */
@Composable
private fun LogItem(log: SelectionLog) {
    val colors = MiuixTheme.colorScheme
    val textColor = when (log.level) {
        SelectionLog.LogLevel.SUCCESS -> colors.primary
        SelectionLog.LogLevel.ERROR -> colors.error
        SelectionLog.LogLevel.WARN -> colors.secondary
        SelectionLog.LogLevel.INFO -> colors.onSurfaceVariantSummary
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        Text(
            text = log.time,
            fontSize = 11.sp,
            color = colors.onSurfaceVariantSummary,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = log.message,
            fontSize = 11.sp,
            color = textColor,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** String.isBlank() 简化扩展，避免可空调用 */
private fun String?.ifBlank(defaultValue: String): String =
    if (this.isNullOrBlank()) defaultValue else this