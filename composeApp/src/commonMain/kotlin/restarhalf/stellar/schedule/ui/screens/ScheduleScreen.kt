package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.course.effectiveCoursesForWeek
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.ui.components.screen.schedule.CourseCard
import restarhalf.stellar.schedule.ui.components.screen.schedule.CourseDetailItem
import restarhalf.stellar.schedule.ui.components.screen.schedule.TransClassDialog
import restarhalf.stellar.schedule.ui.components.screen.schedule.WeekHeaderRow
import restarhalf.stellar.schedule.ui.components.screen.schedule.WeekPickerSheet
import restarhalf.stellar.schedule.ui.icons.Add
import restarhalf.stellar.schedule.ui.mapper.CourseRenderItem
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.theme.StatusColors
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("UNUSED_VALUE")
/**
 * 课程表屏幕
 * 
 * 显示完整的课程表，支持：
 * - 左右滑动切换周次
 * - 课程卡片显示
 * - 课程详情弹窗
 * - 调课功能
 * - 点击空格子添加实验课
 * 
 * @param onSync 同步回调
 * @param syncUiState 全局同步状态（用于展示同步中/失败提示）
 * @param showMessage 轻提示回调（Toast）
 * @param onSyncErrorConsumed 错误提示展示后的确认回调，避免重复弹出
 * @param campus 当前校区
 * @param termStartMs 学期开始时间戳
 * @param totalWeeks 学期总周数
 * @param onAddLabCourse 添加课程回调，参数为(dayOfWeek, startSection, selectedWeek)
 * @param onEditLabCourse 编辑实验课回调
 */
fun ScheduleScreen(
    vm: ScheduleViewModel,
    onSync: suspend () -> Unit,
    syncUiState: SyncUiState = SyncUiState.Idle,
    showMessage: (String) -> Unit = {},
    onSyncErrorConsumed: () -> Unit = {},
    campus: Campus,
    termStartMs: Long,
    totalWeeks: Int,
    onAddLabCourse: (dayOfWeek: Int, startSection: Int, selectedWeek: Int) -> Unit,
    onEditLabCourse: (Long) -> Unit
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pageBottomPadding =
        appPageContentPadding(
            innerPadding = PaddingValues(),
            outerPadding = appScaffoldPadding,
        )
            .calculateBottomPadding()
    val scope = rememberCoroutineScope()
    val courses by vm.allCourses.collectAsStateWithLifecycle()
    val uiState =
        remember(campus, termStartMs, totalWeeks) {
            vm.buildScheduleUiState(
                campus = campus,
                totalWeeks = totalWeeks,
                termStartMs = termStartMs
            )
        }

    val pagerState =
        rememberPagerState(initialPage = uiState.pagerInitialPage) { uiState.pagerPageCount }
    val currentWeek =
        vm.pageToWeek(page = pagerState.currentPage, includeWeek0 = uiState.includeWeek0)
    val scheduleUiState by vm.uiState.collectAsStateWithLifecycle()
    var selectedEmptyCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showWeekPicker by remember { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(pagerState.currentPage) {
        selectedEmptyCell = null
    }

    // 自动/手动同步失败时给出可见反馈，并向上确认消费，避免切页后重复弹出
    LaunchedEffect(syncUiState) {
        val state = syncUiState
        if (state is SyncUiState.Error) {
            showMessage("课表同步失败：${state.message}")
            onSyncErrorConsumed()
        }
    }
    val primary = colors.primary
    val surfaceSoft = colors.surfaceContainerHigh
    val textPrimary = colors.onBackground
    val textSecondary = colors.onSurfaceVariantSummary
    val textHint = colors.onSurfaceVariantActions
    val isDarkMode = colors.background.luminance() < 0.5f
    val mutedCourseColor = StatusColors.mutedBackground
    val mutedTitleColor = StatusColors.mutedTitle
    val mutedSubColor = StatusColors.mutedSub
    val dayCount = 7
    val rowHeight = scheduleUiState.scheduleRowHeight.dp
    val rowGap = 1.dp
    val restHeight = 24.dp
    val cellInset = 0.5.dp
    val restBarTextMeasurer = rememberTextMeasurer()
    val footnoteStyle = MiuixTheme.textStyles.footnote1

    val effectiveCoursesCache =
        remember(courses) { mutableMapOf<Int, List<restarhalf.stellar.schedule.domain.model.Course>>() }

    fun yForSection(section: Int): Dp {
        val base = (rowHeight + rowGap) * (section - 1)
        val rest = (if (section > 4) restHeight else 0.dp) + (if (section > 8) restHeight else 0.dp)
        return base + rest
    }
    fun heightForSections(sectionCount: Int): Dp {
        if (sectionCount <= 0) return 0.dp
        return rowHeight * sectionCount + rowGap * (sectionCount - 1)
    }

    val totalHeight =
        remember(rowHeight, rowGap, restHeight) { rowHeight * 12 + rowGap * 11 + restHeight * 2 }

    val timetable: List<TimetableSlot> = uiState.timetable

    LaunchedEffect(Unit) {
        if (vm.shouldAutoSync()) onSync()
        vm.refreshCourseCalendar(
            campus = campus,
            termStartMs = termStartMs,
            totalWeeks = totalWeeks
        )
    }

    // 带动画跳转到指定周次（供"回到当前周"与周次选择弹窗复用）
    fun animateToWeek(targetWeek: Int) {
        scope.launch {
            val targetPage = vm.weekToPage(
                week = targetWeek,
                includeWeek0 = uiState.includeWeek0
            )
            pagerState.scroll(MutatePriority.UserInput) {
                val distance =
                    kotlin.math.abs(targetPage - pagerState.currentPage).coerceAtLeast(2)
                val duration = 100 * distance + 100
                val pageSize =
                    pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
                val currentDistanceInPages =
                    targetPage - pagerState.currentPage - pagerState.currentPageOffsetFraction
                val scrollPixels = currentDistanceInPages * pageSize

                var previousValue = 0f
                animate(
                    initialValue = 0f,
                    targetValue = scrollPixels,
                    animationSpec = tween(easing = EaseInOut, durationMillis = duration),
                ) { currentValue, _ ->
                    previousValue += scrollBy(currentValue - previousValue)
                }
            }
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Box {
                AppPageTopBar(
                    title = if (currentWeek == 0) "假期中" else "第${currentWeek}周",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        if (currentWeek != uiState.detectedWeekInfo.week) {
                            IconButton(onClick = {
                                animateToWeek(uiState.detectedWeekInfo.week)
                            }) {
                                Text(text = "回到当前周")
                            }
                        }
                    },
                    actions = {
                        if (syncUiState is SyncUiState.Loading) {
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    size = 18.dp,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    },
                )
                // 点击顶栏标题区域弹出周次选择（matchParentSize 跟随顶栏高度，不参与测量）
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showWeekPicker = true }
                    )
                }
            }
        },
        popupHost = {
            WeekPickerSheet(
                show = showWeekPicker,
                onDismiss = { showWeekPicker = false },
                totalWeeks = totalWeeks,
                viewingWeek = currentWeek,
                detectedWeek = uiState.detectedWeekInfo.week,
                onWeekSelected = { week ->
                    showWeekPicker = false
                    if (week != currentWeek) {
                        animateToWeek(week)
                    }
                }
            )

            val tc = scheduleUiState.transDialogUiState.course
            if (scheduleUiState.transDialogUiState.show && tc != null) {
                TransClassDialog(
                    show = scheduleUiState.transDialogUiState.show,
                    onDismiss = { vm.dismissTransDialog() },
                    totalWeeks = scheduleUiState.transDialogUiState.targetWeek,
                    onTotalWeeksChange = vm::updateTransTargetWeek,
                    newClassRoom = scheduleUiState.transDialogUiState.newClassRoom,
                    onNewClassRoomChange = vm::updateTransNewClassRoom,
                    dayOfWeek = scheduleUiState.transDialogUiState.dayOfWeek,
                    onDayOfWeekChange = vm::updateTransDayOfWeek,
                    startSection = scheduleUiState.transDialogUiState.startSection,
                    endSection = scheduleUiState.transDialogUiState.endSection,
                    onSectionRangeChange = vm::updateTransSectionRange,
                    onTrans = {
                        val input = vm.buildTransOperationInput() ?: return@TransClassDialog

                        scope.launch {
                            val result =
                                vm.buildTransCourseAndConflicts(
                                    originCourse = input.course,
                                    originWeek = input.originWeek,
                                    targetWeek = input.targetWeek,
                                    newRoom = input.newRoom,
                                    dayOfWeek = input.dayOfWeek,
                                    startSection = input.startSection,
                                    endSection = input.endSection
                                )

                            if (result.conflicts.isNotEmpty()) {
                                vm.dismissTransDialog()
                                vm.showTransConflict(result.conflicts, result.overrideCourse)
                                return@launch
                            }
                            val saved = vm.saveTransCourse(result.overrideCourse)
                            if (saved) {
                                vm.closeTransDialogAndClear()
                            } else {
                                showMessage("调课保存失败，请重试")
                            }
                        }
                    }
                )
            }
            val conflicts = scheduleUiState.transConflictUiState.conflicts
            if (scheduleUiState.transConflictUiState.show) {
                WindowDialog(
                    show = scheduleUiState.transConflictUiState.show,
                    modifier = Modifier,
                    title = "调课冲突",
                    titleColor = DialogDefaults.titleColor(),
                    summary = "目标时间段已有课程",
                    summaryColor = DialogDefaults.summaryColor(),
                    backgroundColor = DialogDefaults.backgroundColor(),
                    enableWindowDim = true,
                    onDismissRequest = {
                        vm.dismissTransConflict(reopenTransDialog = true)
                    },
                    onDismissFinished = null,
                    outsideMargin = DialogDefaults.outsideMargin,
                    insideMargin = DialogDefaults.insideMargin,
                    defaultWindowInsetsPadding = true,
                    content = {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 8.dp,
                                bottom = 0.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(conflicts, key = { it.id }) { c ->
                                Text(
                                    text = "${c.name} \n@${c.location}   |   第${c.startSection}-${c.startSection + c.sectionCount - 1}节",
                                    style = MiuixTheme.textStyles.body1
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            vm.clearTransConflict()
                                            vm.dismissTransConflict(reopenTransDialog = true)
                                        }) {
                                        Text(text = "取消")
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))
                                    Button(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            val pending = vm.consumePendingOverride()
                                            if (pending == null) {
                                                vm.clearTransConflict()
                                                vm.closeTransDialogAndClear()
                                                return@Button
                                            }
                                            scope.launch {
                                                val saved = vm.saveTransCourse(pending)
                                                if (saved) {
                                                    vm.closeTransDialogAndClear()
                                                } else {
                                                    showMessage("保存失败，请重试")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColorsPrimary()
                                    ) {
                                        Text(
                                            text = "强制保存",
                                            color = colors.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    })
            }

            val dc = scheduleUiState.detailSheetUiState.courses
            if (scheduleUiState.detailSheetUiState.show && dc.isNotEmpty()) {

                OverlayBottomSheet(
                    show = scheduleUiState.detailSheetUiState.show,
                    modifier = Modifier,
                    title = "课程详情",
                    startAction = null,
                    endAction = null,
                    backgroundColor = BottomSheetDefaults.backgroundColor(),
                    enableWindowDim = true,
                    cornerRadius = BottomSheetDefaults.cornerRadius,
                    sheetMaxWidth = BottomSheetDefaults.maxWidth,
                    onDismissRequest = {
                        vm.closeDetailSheet()
                    },
                    onDismissFinished = null,
                    outsideMargin = BottomSheetDefaults.outsideMargin,
                    insideMargin = BottomSheetDefaults.insideMargin,
                    defaultWindowInsetsPadding = true,
                    dragHandleColor = colors.background,
                    allowDismiss = true,
                    enableNestedScroll = true,
                    renderInRootScaffold = true,
                    content = {
                        LazyColumn {
                            items(dc.size, key = { dc[it].id }) { index ->
                                val c = dc[index]

                                val isCurrent = isCourseActiveInWeek(c, currentWeek)
                                val detailUi = remember(c.id, currentWeek, timetable) {
                                    vm.buildCourseDetailUi(c, currentWeek, timetable)
                                }

                                CourseDetailItem(
                                    modifier =
                                        Modifier.animateItem(
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        ),
                                    course = c,
                                    surfaceSoft = surfaceSoft,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    detailUi = detailUi,
                                    isCurrent = isCurrent,
                                    onEditLabCourse = { id ->
                                        vm.closeDetailSheet()
                                        onEditLabCourse(id)
                                    },
                                    onTransCourse = { courseToTrans ->
                                        vm.openTransDialog(courseToTrans, currentWeek)
                                        vm.closeDetailSheet()
                                    },
                                    onRevertTrans = { id ->
                                        vm.closeDetailSheet()
                                        val toDelete = courses.firstOrNull { it.id == id }
                                        if (toDelete != null) {
                                            scope.launch {
                                                val deleted = vm.deleteCourse(toDelete)
                                                if (!deleted) {
                                                    showMessage("撤销调课失败，请重试")
                                                }
                                            }
                                        }
                                    })
                            }
                        }

                        Spacer(Modifier.height(25.dp))
                    })
            }
        }) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = 0.dp,
                        )
                    )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val weekHeaderUi =
                    remember(
                        currentWeek,
                        uiState.detectedWeekInfo.diffDays,
                        uiState.detectedWeekInfo.week,
                        termStartMs,
                        dayCount
                    ) {
                        vm.buildWeekHeaderUi(
                            currentWeek = currentWeek,
                            detectedDiffDays = uiState.detectedWeekInfo.diffDays,
                            detectedWeek = uiState.detectedWeekInfo.week,
                            termStartMs = termStartMs,
                            dayCount = dayCount
                        )
                    }

                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .padding(vertical = 4.dp)
                ) {
                    WeekHeaderRow(
                        ui = weekHeaderUi, primary = primary, textSecondary = textSecondary
                    )
                }


                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                ) { page: Int ->
                    val actualWeek = vm.pageToWeek(page = page, includeWeek0 = uiState.includeWeek0)
                    val weekCourses = effectiveCoursesCache.getOrPut(actualWeek) {
                        effectiveCoursesForWeek(all = courses, week = actualWeek)
                    }

                    val pageRenderUi =
                        remember(
                            weekCourses,
                            page,
                            scheduleUiState.showNonCurrentWeek,
                            scheduleUiState.scheduleRowHeight,
                            isDarkMode,
                            mutedCourseColor,
                            mutedTitleColor,
                            mutedSubColor,
                            cellInset,
                        ) {
                            vm.buildPageRenderUi(
                                courses = courses,
                                page = page,
                                includeWeek0 = uiState.includeWeek0,
                                dayCount = dayCount,
                                showNonCurrentWeek = scheduleUiState.showNonCurrentWeek,
                                isDarkMode = isDarkMode,
                                mutedCourseColor = mutedCourseColor,
                                mutedTitleColor = mutedTitleColor,
                                mutedSubColor = mutedSubColor,
                                yForSection = ::yForSection,
                                heightForSections = ::heightForSections,
                                cellInset = cellInset,
                                effectiveCourses = weekCourses
                            )
                        }
                    val pageScrollState =
                        remember(page) { ScrollState(0) }

                    val pageRenderData = pageRenderUi.dayRenderData

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior)
                            .verticalScroll(pageScrollState)
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = pageBottomPadding)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(totalHeight)
                            ) {


                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sectionHeight = rowHeight.toPx()
                                    val gapPx = rowGap.toPx()
                                    val restPx = restHeight.toPx()
                                    val barHeight = restHeight.toPx()
                                    fun drawRestBar(afterSection: Int, label: String) {
                                        val y =
                                            afterSection * (sectionHeight + gapPx) +
                                                    (if (afterSection > 4) restPx else 0f)
                                        drawRect(
                                            color = surfaceSoft,
                                            topLeft = Offset(0f, y),
                                            size = androidx.compose.ui.geometry.Size(
                                                size.width,
                                                barHeight
                                            )
                                        )
                                        val textLayout =
                                            restBarTextMeasurer.measure(
                                                text = label,
                                                style = footnoteStyle.copy(color = textSecondary),
                                            )

                                        drawText(
                                            textLayoutResult = textLayout,
                                            topLeft = Offset(
                                                x = (size.width - textLayout.size.width) / 2f,
                                                y = y + (barHeight - textLayout.size.height) / 2f,
                                            ),
                                        )
                                    }

                                    drawRestBar(afterSection = 4, label = "午休")

                                    drawRestBar(afterSection = 8, label = "晚休")
                                }

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    ScheduleSectionLabels(
                                        timetable = timetable,
                                        rowHeight = rowHeight,
                                        rowGap = rowGap,
                                        restHeight = restHeight,
                                        textSecondary = textSecondary,
                                        textHint = textHint,
                                    )

                                    Row(modifier = Modifier.weight(1f)) {
                                        (1..dayCount).forEach { day ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            ) {
                                                val dayData = pageRenderData[day]
                                                val renderItems = dayData?.items.orEmpty()

                                                ScheduleDayContent(
                                                    renderItems = renderItems,
                                                    day = day,
                                                    rowHeight = rowHeight,
                                                    rowGap = rowGap,
                                                    selectedEmptyCell = selectedEmptyCell,
                                                    onEmptyCellClick = { d, s ->
                                                        if (selectedEmptyCell == Pair(d, s)) {
                                                            onAddLabCourse(d, s, currentWeek)
                                                            selectedEmptyCell = null
                                                        } else {
                                                            selectedEmptyCell = Pair(d, s)
                                                        }
                                                    },
                                                    onCourseClick = { item ->
                                                        selectedEmptyCell = null
                                                        vm.openDetailSheet(item.overlaps)
                                                    },
                                                    yForSection = ::yForSection,
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
        }
    }
}

@Composable
private fun ScheduleSectionLabels(
    timetable: List<TimetableSlot>,
    rowHeight: Dp,
    rowGap: Dp,
    restHeight: Dp,
    textSecondary: Color,
    textHint: Color,
) {
    Column(
        modifier = Modifier.width(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        (1..12).forEach { section ->
            val slot = timetable.getOrNull(section - 1)

            Box(
                modifier = Modifier.height(rowHeight),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = section.toString(),
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = slot?.start ?: "",
                        fontSize = 9.sp,
                        color = textHint
                    )
                    Text(
                        text = slot?.end ?: "",
                        fontSize = 9.sp,
                        color = textHint
                    )
                }
            }

            if (section == 4 || section == 8) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(restHeight)
                )
            }

            if (section != 12) {
                Spacer(modifier = Modifier.height(rowGap))
            }
        }
    }
}

@Composable
private fun ScheduleDayContent(
    renderItems: List<CourseRenderItem>,
    day: Int,
    rowHeight: Dp,
    rowGap: Dp,
    selectedEmptyCell: Pair<Int, Int>?,
    onEmptyCellClick: (day: Int, section: Int) -> Unit,
    onCourseClick: (CourseRenderItem) -> Unit,
    yForSection: (Int) -> Dp,
) {
    val colors = MiuixTheme.colorScheme
    renderItems.forEach { item ->
        CourseCard(
            model = item.model,
            onClick = { onCourseClick(item) }
        )
    }

    val occupiedSections = remember(renderItems) {
        val rowHeightPx = rowHeight.value
        val rowGapPx = (rowHeight + rowGap).value
        val cellInsetPx = 0.5f
        buildSet {
            for (item in renderItems) {
                val courseStart =
                    ((item.model.topOffsetY.value - cellInsetPx) / rowGapPx).toInt() + 1
                val courseSections =
                    (item.model.height.value / rowHeightPx).toInt()
                        .coerceAtLeast(1)
                for (s in courseStart until (courseStart + courseSections)) {
                    add(s)
                }
            }
        }
    }
    val emptyCellInteractionSource = remember { MutableInteractionSource() }

    (1..12).forEach { section ->
        if (section !in occupiedSections) {
            val isSelected = selectedEmptyCell == Pair(day, section)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = 1.dp)
                    .offset(y = yForSection(section))
                    .clickable(
                        interactionSource = emptyCellInteractionSource,
                        indication = null
                    ) {
                        onEmptyCellClick(day, section)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .squircleBackground(colors.surfaceContainerHighest, 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Add,
                            contentDescription = "添加课程",
                            tint = colors.surfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

