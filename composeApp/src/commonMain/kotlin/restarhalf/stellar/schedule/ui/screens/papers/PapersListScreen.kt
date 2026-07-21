package restarhalf.stellar.schedule.ui.screens.papers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.StarVerificationDialog
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PapersListScreen(
    vm: PapersViewModel,
    onBack: () -> Unit,
    onPaperDetail: (String) -> Unit,
    onUploadClick: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val expandedFolders = remember { mutableStateSetOf<String>() }
    val colors = MiuixTheme.colorScheme

    val starState by vm.starVerification.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!starState.isVerified) {
            vm.starVerification.showDialog()
        }
    }

    StarVerificationDialog(
        show = starState.showDialog,
        username = starState.username,
        isVerifying = starState.isVerifying,
        error = starState.error,
        onUsernameChange = { vm.starVerification.onUsernameChange(it) },
        onVerify = { vm.starVerification.verify() },
        onDismiss = { onBack() },
    )

    LaunchedEffect(starState.isVerified) {
        if (starState.isVerified) {
            vm.loadFolders()
            vm.loadPapers()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "试卷共享", scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector = Back,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
                AnimatedVisibility(
                    visible = uiState.error != null && starState.showDialog.not(),
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
                onClick = onUploadClick,
            ) {
                Text(text = "上传试卷", color = colors.onPrimary)
            }
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = uiState.loading,
            onRefresh = { vm.refresh() },
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                AppCard {
                    TextField(
                        label = "搜索试卷",
                        value = uiState.searchQuery,
                        onValueChange = { vm.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (!uiState.loading && uiState.papers.isEmpty()) {
                item {
                    Text(
                        text = "暂无试卷",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 14.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }

            uiState.folders.forEach { folder ->
                val isExpanded = folder in expandedFolders
                val folderPapers = uiState.papers.filter { it.folder == folder }

                item(key = "folder_$folder") {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            BasicComponent(
                                title = folder,
                                summary = "${folderPapers.size}份试卷",
                                onClick = {
                                    if (isExpanded) expandedFolders.remove(folder)
                                    else expandedFolders.add(folder)
                                },
                            )

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    folderPapers.forEach { paper ->
                                        AppCard(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(start = 8.dp, end = 8.dp)
                                                .padding(vertical = 2.dp),
                                        ) {
                                            BasicComponent(
                                                title = paper.title.ifEmpty { "未命名试卷" },
                                                onClick = { onPaperDetail(paper.id) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val rootPapers = uiState.papers.filter { p ->
                uiState.folders.none { it == p.folder }
            }
            if (rootPapers.isNotEmpty()) {
                items(rootPapers, key = { it.id }) { paper ->
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        BasicComponent(
                            title = paper.title.ifEmpty { "未命名试卷" },
                            onClick = { onPaperDetail(paper.id) },
                        )
                    }
                }
            }

        }
        }
    }
}
