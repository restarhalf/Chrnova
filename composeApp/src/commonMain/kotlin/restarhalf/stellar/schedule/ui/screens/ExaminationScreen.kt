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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.ui.components.screen.examination.ExamItemCard
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.miuixCapsuleShape
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ExaminationScreen(onLoadExaminations: suspend () -> List<Examination>) {
    val vm: ExaminationViewModel = koinViewModel()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val examUiState by vm.uiState.collectAsState()
    val colors = MiuixTheme.colorScheme
    val surfaceSoft = colors.surfaceContainerHigh
    val pullToRefreshState = rememberPullToRefreshState()
    LaunchedEffect(onLoadExaminations) {
        vm.bindLoader(onLoadExaminations)
    }

    LaunchedEffect(Unit) { vm.load() }

    val screenState = remember(examUiState) {
        vm.buildScreenState(
            items = examUiState.items,
            loading = examUiState.loading,
            error = examUiState.error,
            nowMs = Clock.System.now().toEpochMilliseconds(),
        )
    }

    val statusText = screenState.statusText

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(title = "考试安排", scrollBehavior = topAppBarScrollBehavior)
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
        }) { paddingValues ->
        PullToRefresh(
            isRefreshing = examUiState.loading,
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
                items(screenState.cards, key = { it.idKey }) { card ->
                    ExamItemCard(
                        modifier =
                            Modifier.animateItem(
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ),
                        card = card
                    )
                }
            }
        }
    }
}


