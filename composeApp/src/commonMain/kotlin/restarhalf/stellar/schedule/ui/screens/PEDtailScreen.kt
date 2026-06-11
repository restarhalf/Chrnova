package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect

@Composable
fun PEDetailScreen(
    schoolYear: String,
    onBack : () -> Unit
) {
    val vm: PEViewModel = koinViewModel()
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val overscrollEffect = MiuixOverscrollEffect()
    val detailData by vm.detailData.collectAsState()
    val loading by vm.loading.collectAsState()
    val needsLogin by vm.needsLogin.collectAsState()
    val error by vm.error.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }

    LaunchedEffect(schoolYear) {
        vm.observeCachedDetailData(schoolYear)
        vm.loadScoreDetail(schoolYear)
    }

    LaunchedEffect(needsLogin) {
        if (needsLogin) {
            showLoginDialog = true
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            val nextYear=schoolYear.toInt()+1
            AppPageTopBar(
                title = "${schoolYear}-${nextYear}学年体测成绩",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = onBack,

                    ){
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        popupHost = {
            if (showLoginDialog) {
                PELoginDialog(
                    onDismiss = {
                        showLoginDialog = false
                        vm.onLoginDialogDismissed()
                    },
                    onLogin = { username, password ->
                        vm.login(
                            username, password,
                            onSuccess = {
                                showLoginDialog = false
                                vm.onLoginDialogDismissed()
                                vm.loadScoreDetail(schoolYear)
                            },
                            onError = {}
                        )
                    },
                    loading = loading,
                    error = error
                )
            }
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = loading,
            onRefresh = { vm.loadScoreDetail(schoolYear) },
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
                    outerPadding = appScaffoldPadding,
                    extraTop = 12.dp,
                    extraStart = 16.dp,
                    extraEnd = 16.dp,
                ),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                overscrollEffect = overscrollEffect
            ) {
                detailData?.let { data ->
                    item {
                        Box(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        ) {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "总分", fontSize = 16.sp)
                                        Text(
                                            text = "${data.totalScore}分",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "等级", fontSize = 16.sp)
                                        Text(
                                            text = data.totalGrade,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(data.dataArr, key = { it.subjectId }) { subject ->
                        Box(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        ) {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = subject.subName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (subject.isJoin == 1) {
                                            Text(
                                                text = "${subject.score ?: "--"}分",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(
                                                text = "未参加",
                                                fontSize = 14.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                            )
                                        }
                                    }
                                    if (subject.isJoin == 1) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "成绩",
                                                fontSize = 14.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                            )
                                            Text(
                                                text = "${subject.result ?: "--"}${subject.unit}",
                                                fontSize = 14.sp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "等级",
                                                fontSize = 14.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                            )
                                            Text(text = subject.grade ?: "--", fontSize = 14.sp)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "占比",
                                            fontSize = 14.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                        Text(text = "${subject.subRatio}%", fontSize = 14.sp)
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

