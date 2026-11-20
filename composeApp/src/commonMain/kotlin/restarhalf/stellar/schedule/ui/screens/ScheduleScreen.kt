package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haooz.softglass.backdrop.backdrops.layerBackdrop as glassLayerBackdrop
import com.haooz.softglass.backdrop.backdrops.rememberCombinedBackdrop
import com.haooz.softglass.backdrop.backdrops.rememberLayerBackdrop as rememberGlassLayerBackdrop
import com.haooz.softglass.shape.ContinuousRoundedRectangle
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import restarhalf.stellar.schedule.domain.policy.effectiveCoursesForWeek
import restarhalf.stellar.schedule.domain.policy.isCourseActiveInWeek
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.ui.components.BackToNowFloatingButton
import restarhalf.stellar.schedule.ui.components.LocalLiquidGlassBackdrop
import restarhalf.stellar.schedule.ui.components.LocalWallpaperBackdrop
import restarhalf.stellar.schedule.ui.components.sheetContentBottomInset
import restarhalf.stellar.schedule.ui.components.screen.schedule.CourseCard
import restarhalf.stellar.schedule.ui.components.screen.schedule.CourseDetailItem
import restarhalf.stellar.schedule.ui.components.screen.schedule.TransClassDialog
import restarhalf.stellar.schedule.ui.components.screen.schedule.WeekPickerSheet
import restarhalf.stellar.schedule.ui.components.wallpaperGlass
import restarhalf.stellar.schedule.ui.components.topbar.AppMoreMenuItem
import restarhalf.stellar.schedule.ui.components.topbar.MorePopupMenu
import restarhalf.stellar.schedule.ui.components.topbar.LiquidGlassDropdownShadowPadding
import restarhalf.stellar.schedule.ui.components.topbar.LiquidTopBarButton
import restarhalf.stellar.schedule.ui.components.topbar.ScheduleTopBar
import restarhalf.stellar.schedule.ui.components.topbar.scheduleContentTopPadding
import restarhalf.stellar.schedule.ui.icons.Add
import restarhalf.stellar.schedule.ui.icons.Background
import restarhalf.stellar.schedule.ui.icons.FastForward
import restarhalf.stellar.schedule.ui.icons.Refresh
import restarhalf.stellar.schedule.ui.icons.Share
import restarhalf.stellar.schedule.ui.mapper.CourseRenderItem
import restarhalf.stellar.schedule.ui.share.ShareTimetableDialog
import restarhalf.stellar.schedule.ui.chrome.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.theme.StatusColors
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.layout.rememberSharedScrollBehavior
import top.yukonga.miuix.kmp.overlay.BlurBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("UNUSED_VALUE")

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
    onEditLabCourse: (Long) -> Unit,
    onWholeCourseTrans: (Long) -> Unit = {},
    canSaveImage: Boolean = false,
    onSaveImage: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    sharerNickname: String? = null,
    sharerAvatarUri: String? = null,
    backgroundImageUri: String? = null,
    backgroundAlpha: Float = 1f,
    backgroundBlur: Float = 0f,
    backgroundBrightness: Float = 0f,
    onCustomize: () -> Unit = {},
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberSharedScrollBehavior()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val contentTopPadding = scheduleContentTopPadding(statusBarHeight)
    val wallpaperBackdrop = LocalWallpaperBackdrop.current
    val surfaceColor = MiuixTheme.colorScheme.surface
    val hasWallpaper = wallpaperBackdrop != null
    // 使用默认 contentOnlyCapture = true，无壁纸时把底色放到 layerBackdrop 右侧 .background(surfaceColor)，
    // 避免整张课表在翻周重录期间画两遍。
    val barContentBackdrop = rememberGlassLayerBackdrop()
    val chromeBackdrop =
        if (wallpaperBackdrop != null) {
            rememberCombinedBackdrop(wallpaperBackdrop, barContentBackdrop)
        } else {
            barContentBackdrop
        }
    val topBarSampleBackdrop = rememberGlassLayerBackdrop()
    val overlayGlassBackdrop = rememberCombinedBackdrop(chromeBackdrop, topBarSampleBackdrop)
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
    var showShare by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val moreMaterialAlpha = remember { mutableFloatStateOf(0f) }
    val moreSlotCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val moreMenuFraction = remember { Animatable(0f) }
    var topBarSampled by remember { mutableStateOf(false) }
    if (showMoreMenu && !topBarSampled) topBarSampled = true
    LaunchedEffect(showMoreMenu) {
        if (showMoreMenu) return@LaunchedEffect
        snapshotFlow { moreMenuFraction.value }.first { it <= 0.5f }
        topBarSampled = false
    }
    var revertChoice by remember {
        mutableStateOf<Pair<restarhalf.stellar.schedule.domain.model.Course, List<restarhalf.stellar.schedule.domain.model.Course>>?>(null)
    }
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(pagerState.currentPage) {
        selectedEmptyCell = null
        topAppBarScrollBehavior.state.contentOffset = 0f
    }

    LaunchedEffect(syncUiState) {
        if (syncUiState is SyncUiState.Error) {
            showMessage("课表同步失败：${syncUiState.message}")
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

    // 采样层录制指纹：结构量一变强制重录一帧；滚动 / 翻页中每帧续录。
    // ⚠ 只给 recordKey 不给 mustRecord 会让采样冻结 —— 屏幕上内容在动、录进层里的像素不动，
    // 玻璃就成了贴在屏上的旧照片。两者必须成对提供。
    val contentRecordKey = remember(currentWeek, isDarkMode, courses, rowHeight, dayCount) {
        listOf(currentWeek, isDarkMode, courses, rowHeight, dayCount)
    }
    val contentMustRecord: () -> Boolean =
        remember(pagerState) {
            {
                pagerState.isScrollInProgress ||
                    abs(pagerState.currentPageOffsetFraction) > 0.001f
            }
        }
    val cellInset = 0.5.dp
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

    val timetable: ImmutableList<TimetableSlot> = uiState.timetable

    LaunchedEffect(Unit) {
        if (vm.shouldAutoSync()) onSync()
        vm.refreshCourseRemindersIfEnabled(
            campus = campus,
            termStartMs = termStartMs,
            totalWeeks = totalWeeks
        )
    }

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
        popupHost = {
            if (showShare) {
                val shareWeekHeaderUi =
                    remember(currentWeek, uiState.detectedWeekInfo.diffDays, uiState.detectedWeekInfo.week, termStartMs, dayCount) {
                        vm.buildWeekHeaderUi(
                            currentWeek = currentWeek,
                            detectedDiffDays = uiState.detectedWeekInfo.diffDays,
                            detectedWeek = uiState.detectedWeekInfo.week,
                            termStartMs = termStartMs,
                            dayCount = dayCount,
                        )
                    }
                val sharePageRenderUi =
                    remember(currentWeek, courses, scheduleUiState.showNonCurrentWeek) {
                        val geo = restarhalf.stellar.schedule.ui.share.ShareTimetableGeometry
                        vm.buildPageRenderUi(
                            courses = courses,
                            page = vm.weekToPage(week = currentWeek, includeWeek0 = uiState.includeWeek0),
                            includeWeek0 = uiState.includeWeek0,
                            dayCount = dayCount,
                            showNonCurrentWeek = scheduleUiState.showNonCurrentWeek,
                            isDarkMode = isDarkMode,
                            mutedCourseColor = mutedCourseColor,
                            mutedTitleColor = mutedTitleColor,
                            mutedSubColor = mutedSubColor,
                            yForSection = geo::yForSection,
                            heightForSections = geo::heightForSections,
                            cellInset = geo.cellInset,
                        )
                    }
                val weekLabel =
                    if (currentWeek == 0) "假期中" else "第 $currentWeek 周 · 课表"
                ShareTimetableDialog(
                    show = true,
                    nickname = sharerNickname,
                    avatarUri = sharerAvatarUri,
                    weekLabel = weekLabel,
                    weekHeaderUi = shareWeekHeaderUi,
                    dayRenderData = sharePageRenderUi.dayRenderData,
                    timetable = timetable,
                    canSaveImage = canSaveImage,
                    onSaveImage = onSaveImage,
                    showMessage = showMessage,
                    onDismiss = { showShare = false },
                    backgroundImageUri = backgroundImageUri,
                    backgroundAlpha = backgroundAlpha,
                    backgroundBlur = backgroundBlur,
                    backgroundBrightness = backgroundBrightness,
                )
            }
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

                            if (result.isNoOp) {
                                showMessage("时间和教室都没变，无需调课")
                                return@launch
                            }

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
                    },
                    onWholeCourseTrans = {
                        val courseId = tc.id
                        vm.closeTransDialogAndClear()
                        onWholeCourseTrans(courseId)
                    }
                )
            }
            val conflicts = scheduleUiState.transConflictUiState.conflicts
            if (scheduleUiState.transConflictUiState.show) {
                OverlayDialog(
                    show = scheduleUiState.transConflictUiState.show,
                    liquidGlassBackdrop = LocalLiquidGlassBackdrop.current,
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

                BlurBottomSheet(
                    show = scheduleUiState.detailSheetUiState.show,
                    title = "课程详情",
                    dimBackground = true,
                    glassBackdrop = LocalLiquidGlassBackdrop.current,
                    onDismissRequest = {
                        vm.closeDetailSheet()
                    },
                ) {
                    LazyColumn(modifier = Modifier.padding(horizontal = 24.dp)) {
                        item { Spacer(Modifier.height(58.dp)) }
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
                                    val target = dc.firstOrNull { it.id == id }
                                        ?: courses.firstOrNull { it.id == id }
                                    if (target != null) {
                                        val group = vm.buildTransGroup(courses, target)
                                        if (group.size > 1) {
                                            revertChoice = target to group
                                        } else {
                                            vm.closeDetailSheet()
                                            scope.launch {
                                                val deleted = vm.deleteCourse(target)
                                                if (!deleted) {
                                                    showMessage("撤销调课失败，请重试")
                                                }
                                            }
                                        }
                                    }
                                })
                        }
                        item { Spacer(Modifier.height(sheetContentBottomInset())) }
                    }
                }
            }

            val choice = revertChoice
            if (choice != null) {
                val (target, group) = choice
                OverlayDialog(
                    show = true,
                    liquidGlassBackdrop = LocalLiquidGlassBackdrop.current,
                    modifier = Modifier,
                    title = "撤销调课",
                    titleColor = DialogDefaults.titleColor(),
                    summary = "这门课共有 ${group.size} 条调课记录",
                    summaryColor = DialogDefaults.summaryColor(),
                    backgroundColor = DialogDefaults.backgroundColor(),
                    enableWindowDim = true,
                    onDismissRequest = { revertChoice = null },
                    onDismissFinished = null,
                    outsideMargin = DialogDefaults.outsideMargin,
                    insideMargin = DialogDefaults.insideMargin,
                    defaultWindowInsetsPadding = true,
                    content = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                onClick = {
                                    revertChoice = null
                                    vm.closeDetailSheet()
                                    scope.launch {
                                        val deleted = vm.deleteCourses(group)
                                        if (!deleted) {
                                            showMessage("撤销调课失败，请重试")
                                        }
                                    }
                                }
                            ) {
                                Text(text = "撤销全部", color = colors.onPrimary)
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    revertChoice = null
                                    vm.closeDetailSheet()
                                    scope.launch {
                                        val deleted = vm.deleteCourse(target)
                                        if (!deleted) {
                                            showMessage("撤销调课失败，请重试")
                                        }
                                    }
                                }
                            ) {
                                Text(text = "撤销选中")
                            }
                        }
                    })
            }
        }) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        PaddingValues(
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = 0.dp,
                        )
                    )
        ) {
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

            // 首帧仅组合当前周缩短冷启动，80ms 后再静默预热左右邻周，避免翻周首帧阻塞组合。
            var scheduleBeyondPageCount by remember(pagerState) { mutableIntStateOf(0) }
            LaunchedEffect(pagerState) {
                delay(80.milliseconds)
                scheduleBeyondPageCount = 1
            }

            val density = LocalDensity.current
            val rowHeightPx = with(density) { rowHeight.toPx() }
            val rowGapPx = with(density) { rowGap.toPx() }
            val restHeightPx = with(density) { restHeight.toPx() }
            val selectedCellProvider = remember { { selectedEmptyCell } }
            val currentPageProvider = remember(pagerState) { { pagerState.currentPage } }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = scheduleBeyondPageCount,
                modifier = Modifier
                    .fillMaxSize()
                    .glassLayerBackdrop(
                        backdrop = barContentBackdrop,
                        recordKey = contentRecordKey,
                        mustRecord = contentMustRecord,
                    )
                    .then(if (!hasWallpaper) Modifier.background(surfaceColor) else Modifier),
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
                val latestPageRenderData by rememberUpdatedState(pageRenderData)

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
                                .padding(horizontal = 8.dp)
                                .padding(top = contentTopPadding)
                                .padding(bottom = pageBottomPadding)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(totalHeight)
                        ) {

                            ScheduleRestBands(
                                rowHeight = rowHeight,
                                rowGap = rowGap,
                                restHeight = restHeight,
                                isDark = isDarkMode,
                                surfaceSoft = surfaceSoft,
                                textHint = textHint,
                            )

                            Row(modifier = Modifier.fillMaxWidth()) {
                                ScheduleSectionLabels(
                                    timetable = timetable,
                                    rowHeight = rowHeight,
                                    rowGap = rowGap,
                                    restHeight = restHeight,
                                    textSecondary = textSecondary,
                                    textHint = textHint,
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(dayCount, rowHeightPx, rowGapPx, restHeightPx) {
                                            detectTapGestures { offset ->
                                                val colWidth = size.width / dayCount.toFloat()
                                                if (colWidth <= 0f) return@detectTapGestures
                                                val day = ((offset.x / colWidth).toInt() + 1).coerceIn(1, dayCount)
                                                val section = findSectionAtY(
                                                    yPx = offset.y,
                                                    rowHeightPx = rowHeightPx,
                                                    rowGapPx = rowGapPx,
                                                    restHeightPx = restHeightPx,
                                                ) ?: return@detectTapGestures
                                                val dayItems = latestPageRenderData[day]?.items ?: persistentListOf()
                                                if (isSectionOccupied(dayItems, section, rowHeight, ::yForSection)) {
                                                    return@detectTapGestures
                                                }
                                                if (selectedEmptyCell == Pair(day, section)) {
                                                    onAddLabCourse(day, section, actualWeek)
                                                    selectedEmptyCell = null
                                                } else {
                                                    selectedEmptyCell = Pair(day, section)
                                                }
                                            }
                                        },
                                ) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        (1..dayCount).forEach { day ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            ) {
                                                val dayData = pageRenderData[day]
                                                val renderItems = dayData?.items ?: persistentListOf()

                                                ScheduleDayContent(
                                                    renderItems = renderItems,
                                                    onCourseClick = { item ->
                                                        selectedEmptyCell = null
                                                        vm.openDetailSheet(item.overlaps)
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    ScheduleEmptyCellSelectionOverlay(
                                        selectedCell = selectedCellProvider,
                                        activePage = currentPageProvider,
                                        page = page,
                                        dayCount = dayCount,
                                        rowHeight = rowHeight,
                                        yForSection = ::yForSection,
                                        onSelectedCellClick = { d, s ->
                                            onAddLabCourse(d, s, actualWeek)
                                            selectedEmptyCell = null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val showBackToNow =
                currentWeek != uiState.detectedWeekInfo.week &&
                    !scheduleUiState.detailSheetUiState.show &&
                    !showShare &&
                    !showWeekPicker
            AnimatedVisibility(
                visible = showBackToNow,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = appScaffoldPadding.calculateBottomPadding())
                        .zIndex(20f),
            ) {
                val appear by
                    transition.animateFloat(
                        transitionSpec = {
                            if (targetState == EnterExitState.Visible) {
                                tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            } else {
                                tween(durationMillis = 200, easing = FastOutSlowInEasing)
                            }
                        },
                        label = "BackToNowAppear",
                    ) { if (it == EnterExitState.Visible) 1f else 0f }
                BackToNowFloatingButton(
                    onClick = {
                        showMoreMenu = false
                        animateToWeek(uiState.detectedWeekInfo.week)
                    },
                    backdrop = chromeBackdrop,
                    label = "今",
                    modifier =
                        Modifier.graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            scaleX = 0.6f + 0.4f * appear
                            scaleY = 0.6f + 0.4f * appear
                            alpha = appear
                            clip = false
                        },
                )
            }

            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .then(
                            if (topBarSampled) {
                                Modifier.glassLayerBackdrop(
                                    backdrop = topBarSampleBackdrop,
                                    recordKey = contentRecordKey,
                                    mustRecord = contentMustRecord,
                                )
                            } else {
                                Modifier
                            },
                        ),
            ) {
                ScheduleTopBar(
                    title = if (currentWeek == 0) "假期中" else "第${currentWeek}周",
                    weekHeaderUi = weekHeaderUi,
                    backdrop = chromeBackdrop,
                    scrollBehavior = topAppBarScrollBehavior,
                    onMoreSlotCoordinates = { moreSlotCoordinates.value = it },
                    onMoreMaterial = { moreMaterialAlpha.floatValue = it },
                    onMoreExpand = { showMoreMenu = true },
                ) { backdropAlpha, shadowAlpha ->
                    if (syncUiState is SyncUiState.Loading) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                size = 18.dp,
                                strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        LiquidTopBarButton(
                            onClick = { scope.launch { onSync() } },
                            backdrop = chromeBackdrop,
                            icon = Refresh,
                            contentDescription = "刷新课表",
                            backdropAlpha = backdropAlpha,
                            shadowAlpha = shadowAlpha,
                        )
                    }
                }
            }

            MorePopupMenu(
                showMorePopup = showMoreMenu,
                onMorePopupDismiss = { showMoreMenu = false },
                onMorePopupExpand = { showMoreMenu = true },
                items =
                    listOf(
                        AppMoreMenuItem(
                            text = "分享",
                            icon = Share,
                            onClick = { showShare = true },
                        ),
                        AppMoreMenuItem(
                            text = "跳转周数",
                            icon = FastForward,
                            onClick = { showWeekPicker = true },
                        ),
                        AppMoreMenuItem(
                            text = "软件外观",
                            icon = Background,
                            iconSize = 23.dp,
                            onClick = onCustomize,
                        ),
                    ),
                morePopupFraction = moreMenuFraction,
                moreSlotCoordinates = moreSlotCoordinates,
                materialAlpha = { moreMaterialAlpha.floatValue },
                liquidGlassBackdrop = overlayGlassBackdrop,
            )
        }
    }
}

@Composable
private fun ScheduleRestBands(
    rowHeight: Dp,
    rowGap: Dp,
    restHeight: Dp,
    isDark: Boolean,
    surfaceSoft: Color,
    textHint: Color,
) {
    val wallpaperBackdrop = LocalWallpaperBackdrop.current
    val dividerShape = remember { ContinuousRoundedRectangle(10.dp) }
    val baseColor =
        when {
            wallpaperBackdrop != null -> Color.Transparent
            MiuixTheme.isDynamicColor -> surfaceSoft
            isDark -> Color(0xFF121212)
            else -> Color(0xFFF0F0F0)
        }
    val glassColor = surfaceSoft.copy(alpha = if (isDark) 0.64f else 0.50f)

    listOf(4 to "午休", 8 to "晚休").forEach { (afterSection, label) ->
        val top =
            (rowHeight + rowGap) * afterSection +
                (if (afterSection > 4) restHeight else 0.dp)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset(y = top)
                    .height(restHeight)
                    .padding(vertical = 2.dp)
                    .padding(horizontal = 4.dp)
                    .then(
                        if (wallpaperBackdrop != null) {
                            Modifier.wallpaperGlass(
                                backdrop = wallpaperBackdrop,
                                color = glassColor,
                                shape = dividerShape,
                                cornerRadius = 10.dp,
                            )
                        } else {
                            Modifier.background(baseColor, dividerShape)
                        }
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote2,
                color = textHint,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScheduleSectionLabels(
    timetable: ImmutableList<TimetableSlot>,
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
    renderItems: ImmutableList<CourseRenderItem>,
    onCourseClick: (CourseRenderItem) -> Unit,
) {
    renderItems.forEach { item ->
        CourseCard(
            model = item.model,
            onClick = { onCourseClick(item) },
        )
    }
}

@Composable
private fun ScheduleEmptyCellSelectionOverlay(
    selectedCell: () -> Pair<Int, Int>?,
    activePage: () -> Int,
    page: Int,
    dayCount: Int,
    rowHeight: Dp,
    yForSection: (Int) -> Dp,
    onSelectedCellClick: (day: Int, section: Int) -> Unit,
) {
    val cell = selectedCell() ?: return
    if (activePage() != page) return
    val (day, section) = cell
    val colors = MiuixTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val colWidth = maxWidth / dayCount
        Box(
            modifier = Modifier
                .width(colWidth)
                .height(rowHeight)
                .offset(x = colWidth * (day - 1), y = yForSection(section))
                .padding(horizontal = 1.dp)
                .squircleBackground(colors.surfaceContainerHighest, 8.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onSelectedCellClick(day, section)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Add,
                contentDescription = "添加课程",
                tint = colors.surfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private fun findSectionAtY(
    yPx: Float,
    rowHeightPx: Float,
    rowGapPx: Float,
    restHeightPx: Float,
): Int? {
    for (section in 1..12) {
        val rest = (if (section > 4) restHeightPx else 0f) + (if (section > 8) restHeightPx else 0f)
        val top = (rowHeightPx + rowGapPx) * (section - 1) + rest
        if (yPx >= top && yPx <= top + rowHeightPx) return section
    }
    return null
}

private inline fun isSectionOccupied(
    items: ImmutableList<CourseRenderItem>,
    section: Int,
    rowHeight: Dp,
    yForSection: (Int) -> Dp,
): Boolean {
    if (items.isEmpty()) return false
    val cellTop = yForSection(section).value
    val cellBottom = cellTop + rowHeight.value
    for (item in items) {
        val itemTop = item.model.topOffsetY.value
        val itemBottom = itemTop + item.model.height.value
        if (cellBottom > itemTop && cellTop < itemBottom) return true
    }
    return false
}
