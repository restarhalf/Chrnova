package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.ui.components.screen.grade.GradeDetailsDialog
import restarhalf.stellar.schedule.ui.components.screen.grade.GradeItemCard
import restarhalf.stellar.schedule.ui.components.screen.grade.GradeSummaryCard
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixCapsuleShape

@Composable
fun GradeScreen(onLoadGrades: suspend () -> TermGradeReport) {
    val vm: GradeViewModel = koinViewModel()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val report by vm.report.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val colors = MiuixTheme.colorScheme
    val surfaceSoft = colors.surfaceContainerHigh
    val screenState =
        remember(report, loading, error) { vm.buildScreenState(report, loading, error) }
    val cards = screenState.cards
    val showGradeDetailsDialog = remember { mutableStateOf(false) }
    var selectedGrade by remember { mutableStateOf<GradeCourse?>(null) }
    val statusText = screenState.statusText
    LaunchedEffect(onLoadGrades) { vm.bindLoader(onLoadGrades) }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(showGradeDetailsDialog.value) {
        if (!showGradeDetailsDialog.value) selectedGrade = null
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(title = "成绩", scrollBehavior = topAppBarScrollBehavior)
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val text = statusText ?: ""
                    Box(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier =
                                Modifier.clip(miuixCapsuleShape())
                                    .background(surfaceSoft)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(fontSize = 12.sp, text = text)
                        }
                    }
                }
            }
        },
        popupHost = {
            if (showGradeDetailsDialog.value) {
                selectedGrade?.let {
                    GradeDetailsDialog(
                        show = showGradeDetailsDialog,
                        title = vm.buildGradeTitle(it),
                        summary = vm.buildGradeDetailsSummary(it)
                    )
                }
            }
        }) { paddingValues ->
        PullToRefresh(
            isRefreshing = loading,
            onRefresh = { vm.load() },
            pullToRefreshState = pullToRefreshState,
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            start =
                                paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = 0.dp
                        )
                    )
        ) {
            LazyColumn(
                modifier =
                    Modifier.fillMaxSize()
                        .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
                contentPadding =
                    appPageContentPadding(
                        innerPadding = PaddingValues(),
                        outerPadding = appScaffoldPadding,
                        extraTop = 12.dp,
                        extraStart = 16.dp,
                        extraEnd = 16.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                overscrollEffect = null
            ) {
                screenState.summary?.let { summary ->
                    item(key = "summary") {
                        GradeSummaryCard(
                            summary = summary
                        )
                    }
                }
                items(cards, key = { it.idKey }) { card ->
                    GradeItemCard(
                        modifier =
                            Modifier.animateItem(
                                placementSpec =
                                    spring(stiffness = Spring.StiffnessMediumLow)
                            ),
                        card = card,
                        onClick = {
                            selectedGrade = card.grade
                            showGradeDetailsDialog.value = true
                        })
                }
            }
        }
    }
}

