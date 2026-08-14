package restarhalf.stellar.schedule.ui.screens.papers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PapersDetailScreen(
    vm: PapersViewModel,
    paperId: String,
    onBack: () -> Unit,
    onDownload: (url: String, title: String) -> Unit,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(paperId) {
        vm.loadPaperDetail(paperId)
    }

    LaunchedEffect(uiState.downloadUrl) {
        uiState.downloadUrl?.let { url ->
            val title = uiState.selectedPaper?.title ?: ""
            onDownload(url, title)
        }
    }

    val paper = uiState.selectedPaper
    val colors = MiuixTheme.colorScheme

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(title = "试卷详情", scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回"
                        )
                    }
                })
        },
        bottomBar = {
            if (paper!=null)
            {
                Button(
                    modifier = Modifier.fillMaxWidth().padding( 8.dp),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = { vm.downloadPaper(paper.id) },
                ) {
                    Text(text = "下载", color = colors.onPrimary)
                }
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
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (uiState.loading) {
                item {
                    Text(
                        text = "加载中...",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }

            if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error ?: "",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = colors.error,
                    )
                }
            }

            if (paper != null) {
                item {
                    SmallTitle(text = "试卷信息")
                    AppCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            BasicComponent(
                                title = "标题",
                                summary = paper.title.ifEmpty { "未命名" },
                            )
                            BasicComponent(
                                title = "文件夹",
                                summary = paper.folder.ifEmpty { "未指定" },
                            )
                        }
                    }
                }
            }
        }
    }
}
