package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.screen.ems.ExamItemCard
import restarhalf.stellar.schedule.ui.components.screen.ems.GradeDetailsDialog
import restarhalf.stellar.schedule.ui.components.screen.ems.GradeItemCard
import restarhalf.stellar.schedule.ui.components.screen.ems.GradeSummaryCard
import restarhalf.stellar.schedule.ui.icons.Credit
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalFoundationApi::class)
@Composable
        /**
         * 考试与成绩屏幕
         *
         * 显示考试安排和成绩信息，支持Tab切换和左右滑动切换。
         * 包含以下功能：
         * - 考试安排列表（显示未结束的考试）
         * - 成绩列表（按学期筛选）
         * - 成绩详情弹窗
         * - 下拉刷新
         * - 左右滑动切换考试/成绩Tab
         *
         * @param onLoadExaminations 加载考试安排的挂起函数
         * @param onLoadGrades 加载成绩的挂起函数
         */
fun EMSScreen(
    examVm: ExaminationViewModel,
    gradeVm: GradeViewModel,
    onLoadExaminations: suspend () -> List<Examination>,
    onLoadGrades: suspend () -> TermGradeReport,
    onAddExam: () -> Unit = {},
    onEditExam: (Long) -> Unit = {},
    onNavigateToElectiveCredit: () -> Unit = {},
) {
    val examUiState by examVm.uiState.collectAsStateWithLifecycle()
    val gradeUiState by gradeVm.uiState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var nowMs by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    val showGradeDetailsDialog = remember { mutableStateOf(false) }
    var selectedGrade by remember { mutableStateOf<GradeCourse?>(null) }

    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(onLoadExaminations) { examVm.bindLoader(onLoadExaminations) }
    LaunchedEffect(onLoadGrades) { gradeVm.bindLoader(onLoadGrades) }
    LaunchedEffect(Unit) {
        examVm.load()
        gradeVm.load()
        examVm.refreshExamCalendar()
        while (true) {
            delay(300_000L.milliseconds)
            nowMs = Clock.System.now().toEpochMilliseconds()
        }
    }
    LaunchedEffect(showGradeDetailsDialog.value) {
        if (!showGradeDetailsDialog.value) selectedGrade = null
    }

    val examScreenUi = remember(examUiState, nowMs) {
        examVm.buildScreenUi(examUiState.items, examUiState.loading, examUiState.error, nowMs)
    }
    val gradeScreenUi = remember(gradeUiState) {
        gradeVm.buildScreenUi(gradeUiState.report, gradeUiState.loading, gradeUiState.error)
    }

    val selectedTab by remember { derivedStateOf { pagerState.currentPage } }
    val statusText = if (selectedTab == 0) examScreenUi.statusText else gradeScreenUi.statusText

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "考务",
                    scrollBehavior = topAppBarScrollBehavior,
                    actions = {
                        IconButton(
                            onClick = onNavigateToElectiveCredit
                        ) {
                            Icon(
                                imageVector = Credit,
                                contentDescription = "选修学分"
                            )
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRowWithContour(
                        tabs = listOf("考试", "成绩"),
                        selectedTabIndex = selectedTab,
                        onTabSelected = { index ->
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = statusText != null,
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
                            Text(style = MiuixTheme.textStyles.footnote1, text = statusText ?: "")
                        }
                    }
                }
            }
        },
        popupHost = {
            if (showGradeDetailsDialog.value) {
                selectedGrade?.let {
                    GradeDetailsDialog(
                        show = showGradeDetailsDialog.value,
                        onDismissRequest = { showGradeDetailsDialog.value = false },
                        title = gradeVm.buildGradeTitle(it),
                        summary = gradeVm.buildGradeDetailsSummary(it)
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
                .padding(
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = 0.dp
                    )
                )
        ) { pageIndex ->
            val isPageLoading = if (pageIndex == 0) examUiState.loading else gradeUiState.loading
            PullToRefresh(
                isRefreshing = isPageLoading,
                onRefresh = { if (pageIndex == 0) examVm.load() else gradeVm.load() },
                pullToRefreshState = pullToRefreshState,
                refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新成功"),
                modifier = Modifier.fillMaxSize()
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
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (pageIndex) {
                        0 -> {
                            items(examScreenUi.cards, key = { it.idKey }) { card ->
                                Box(
                                    modifier = Modifier.animateItem(
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                ) {
                                    ExamItemCard(
                                        card = card,
                                        onClick = {
                                            if (card.exam.source == "manual" && card.exam.id > 0) {
                                                onEditExam(card.exam.id)
                                            }
                                        },
                                    )
                                }
                            }
                            item(key = "add_exam_card") {
                                val animProgress = remember { Animatable(0f) }
                                LaunchedEffect(Unit) {
                                    animProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                }
                                Box(
                                    modifier = Modifier.animateItem(
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                ) {
                                    AppCard(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                alpha = animProgress.value
                                                translationY = 50f * (1f - animProgress.value)
                                            }
                                            .clickable { onAddExam() }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .height(IntrinsicSize.Min).padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "",
                                                    style = MiuixTheme.textStyles.footnote1,
                                                    color = colors.onSurfaceVariantSummary
                                                )
                                                Text(
                                                    text = "",
                                                    style = MiuixTheme.textStyles.footnote1,
                                                    color = colors.onSurfaceVariantSummary
                                                )
                                                Text(
                                                    text = "添加考试",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MiuixTheme.textStyles.body1,
                                                    color = colors.primary
                                                )
                                                Text(
                                                    text = "",
                                                    style = MiuixTheme.textStyles.footnote1,
                                                    color = colors.onSurfaceVariantSummary
                                                )
                                                Text(
                                                    text = "",
                                                    style = MiuixTheme.textStyles.footnote1,
                                                    color = colors.onSurfaceVariantSummary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            item(key = "grade_summary") {
                                GradeSummaryCard(
                                    summary = gradeScreenUi.summary,
                                )
                            }
                            items(gradeScreenUi.cards, key = { it.idKey }) { card ->
                                Box(
                                    modifier = Modifier.animateItem(
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                ) {
                                    GradeItemCard(
                                        card = card,
                                        onClick = {
                                            selectedGrade = card.grade
                                            showGradeDetailsDialog.value = true
                                        }
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
