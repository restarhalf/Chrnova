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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PapersUploadScreen(
    vm: PapersViewModel,
    onBack: () -> Unit,
    onResult: (String) -> Unit,
    pdfFilePickerHost: @Composable (onPicked: (ByteArray, String, String) -> Unit) -> Unit = {},
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val overscrollEffect = MiuixOverscrollEffect()
    val uiState by vm.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("") }
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var selectedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileMime by remember { mutableStateOf("") }
    var showFilePicker by remember { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme

    val isFormValid = title.isNotBlank() &&
            folder.isNotBlank() &&
            selectedFileBytes != null

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(1500.milliseconds)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "上传试卷",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = { IconButton(
                    onClick = onBack
                ){
                    Icon(
                        imageVector = Back,
                        contentDescription = "返回"
                    )
                } }
            )
        },
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
                enabled = isFormValid && !uiState.uploading,
                onClick = {
                    vm.uploadPaper(
                        fileBytes = selectedFileBytes ?: return@Button,
                        fileName = selectedFileName,
                        mimeType = selectedFileMime,
                        title = title,
                        folder = folder,
                    )
                },
            ) {
                Text(
                    text = if (uiState.uploading) "上传中..." else "上传",
                    color = colors.onPrimary,
                )
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
                        bottom = 0.dp,
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
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            overscrollEffect = overscrollEffect,
        ) {
            item {
                SmallTitle(text = "试卷信息")
                AppCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextField(
                            label = "标题",
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextField(
                            label = "文件夹",
                            value = folder,
                            onValueChange = { folder = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                SmallTitle(text = "选择文件")
                AppCard {
                    BasicComponent(
                        title = if (selectedFilePath != null) "已选择文件" else "选择文件",
                        summary = selectedFilePath,
                        onClick = { showFilePicker = true },
                    )
                }
            }

            item {
                if (uiState.error != null) {
                    Text(
                        text = uiState.error ?: "",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 14.sp,
                        color = colors.error,
                    )
                }
                if (uiState.successMessage != null) {
                    Text(
                        text = uiState.successMessage ?: "",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 14.sp,
                        color = colors.primary,
                    )
                }
            }

        }
    }

    if (showFilePicker) {
        pdfFilePickerHost { bytes, name, mime ->
            selectedFileBytes = bytes
            selectedFileName = name
            selectedFileMime = mime
            selectedFilePath = name
            showFilePicker = false
            title = name.substringBeforeLast('.')
        }
    }
}
