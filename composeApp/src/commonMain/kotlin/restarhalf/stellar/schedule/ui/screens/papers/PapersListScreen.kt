package restarhalf.stellar.schedule.ui.screens.papers

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect

@Composable
fun PapersListScreen(
    vm: PapersViewModel,
    onBack: () -> Unit,
    onPaperDetail: (String) -> Unit,
    onUploadClick: () -> Unit,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val overscrollEffect = MiuixOverscrollEffect()
    val uiState by vm.uiState.collectAsState()
    val expandedFolders = remember { mutableStateSetOf<String>() }
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(Unit) {
        if (!uiState.isStarVerified) {
            vm.showStarDialog()
        }
    }

    if (uiState.showStarDialog) {
        OverlayDialog(
            show = true,
            modifier = Modifier,
            title = "GitHub Star 验证",
            titleColor = DialogDefaults.titleColor(),
            summary = "请先 star Chrnova 仓库后才能使用",
            summaryColor = DialogDefaults.summaryColor(),
            backgroundColor = DialogDefaults.backgroundColor(),
            enableWindowDim = true,
            onDismissRequest = { onBack() },
            onDismissFinished = null,
            outsideMargin = DialogDefaults.outsideMargin,
            insideMargin = DialogDefaults.insideMargin,
            defaultWindowInsetsPadding = true,
            renderInRootScaffold = true,
            content = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextField(
                        label = "GitHub 用户名",
                        value = uiState.githubUsername,
                        onValueChange = { vm.onGitHubUsernameChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            fontSize = 12.sp,
                            color = colors.error,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        enabled = uiState.githubUsername.isNotBlank() && !uiState.verifyingStar,
                        onClick = { vm.verifyStar() },
                    ) {
                        Text(
                            text = if (uiState.verifyingStar) "验证中..." else "验证",
                            color = colors.onPrimary,
                        )
                    }
                }
            },
        )
    }

    LaunchedEffect(uiState.isStarVerified) {
        if (uiState.isStarVerified) {
            vm.loadFolders()
            vm.loadPapers()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(title = "试卷共享", scrollBehavior = topAppBarScrollBehavior,
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
        },
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = onUploadClick,
            ) {
                Text(text = "上传试卷", color = colors.onPrimary)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = paddingValues.calculateBottomPadding(),
                    )
                )
                .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = PaddingValues(),
                outerPadding = appScaffoldPadding,
                extraTop = 12.dp,
                extraStart = 16.dp,
                extraEnd = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            overscrollEffect = overscrollEffect,
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

            if (uiState.loading) {
                item {
                    Text(
                        text = "加载中...",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 14.sp,
                        color = colors.onSurfaceVariantSummary,
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

            if (uiState.error != null && uiState.showStarDialog.not()) {
                item {
                    Text(
                        text = uiState.error ?: "",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 14.sp,
                        color = colors.error,
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
                        BasicComponent(
                            title = folder,
                            summary = "${folderPapers.size}份试卷",
                            onClick = {
                                if (isExpanded) expandedFolders.remove(folder)
                                else expandedFolders.add(folder)
                            },
                        )
                    }
                }

                if (isExpanded) {
                    items(folderPapers, key = { it.id }) { paper ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        ) {
                            BasicComponent(
                                title = paper.title.ifEmpty { "未命名试卷" },
                                onClick = { onPaperDetail(paper.id) },
                            )
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
