package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.data.remote.PECodeItem
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.FreeApplyTypes
import restarhalf.stellar.schedule.ui.viewmodel.PEFreeApplyViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 免测/缓测申请填写页
 */
@Composable
fun PEFreeApplyEditScreen(
    vm: PEFreeApplyViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    pdfFilePickerHost: @Composable (onPicked: (ByteArray, String, String) -> Unit) -> Unit = {},
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme
    var showFilePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.prepareApplyForm()
    }

    val years = uiState.schoolYears.ifEmpty {
        listOf(
            PECodeItem(
                code = uiState.nowSchoolYear.ifBlank { "2026" },
                label = uiState.nowSchoolYear.ifBlank { "2026" },
            ),
        )
    }
    val yearItems = years.map { it.label.ifBlank { it.code } }
    val yearSelectedIndex = years.indexOfFirst { it.code == uiState.selectedYearCode }
        .coerceAtLeast(0)

    val typeItems = FreeApplyTypes.map { it.label }
    val typeSelectedIndex = FreeApplyTypes.indexOfFirst { it.code == uiState.freeApplyType }
        .coerceAtLeast(0)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "我要申请",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.submitting,
                    onClick = onBack,
                ) {
                    Text(text = "取消")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    enabled = !uiState.submitting && !uiState.uploading,
                    onClick = { vm.submitApply() },
                ) {
                    Text(
                        text = if (uiState.submitting) "提交中..." else "提交申请",
                        color = colors.onPrimary,
                    )
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = paddingValues,
                outerPadding = appScaffoldPadding,
                extraTop = 4.dp,
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SmallTitle(text = "申请信息")
            }
            item {
                AppCard {
                    OverlayDropdownPreference(
                        title = "申请学年",
                        summary = if (uiState.selectedYearLabel.isBlank()) {
                            "请选择申请学年"
                        } else {
                            uiState.selectedYearLabel
                        },
                        items = yearItems,
                        selectedIndex = yearSelectedIndex,
                        onSelectedIndexChange = { index ->
                            val item = years.getOrNull(index) ?: return@OverlayDropdownPreference
                            vm.selectYear(item.code, item.label)
                        },
                    )
                    OverlayDropdownPreference(
                        title = "申请类型",
                        summary = if (uiState.freeApplyTypeLabel.isBlank()) {
                            "仅支持免测、缓测"
                        } else {
                            uiState.freeApplyTypeLabel
                        },
                        items = typeItems,
                        selectedIndex = typeSelectedIndex,
                        onSelectedIndexChange = { index ->
                            val item = FreeApplyTypes.getOrNull(index)
                                ?: return@OverlayDropdownPreference
                            vm.selectApplyType(item.code, item.label)
                        },
                    )
                }
            }
            item {
                SmallTitle(text = "证明材料")
            }
            item {
                AppCard {
                    ArrowPreference(
                        title = if (uiState.uploading) "上传中..." else "上传附件",
                        summary = when {
                            uiState.uploading -> "请稍候"
                            uiState.attachments.isEmpty() -> "请选择证明材料（≤5M）"
                            else -> "已选 ${uiState.attachments.size} 个文件"
                        },
                        onClick = { showFilePicker = true },
                    )
                    if (uiState.attachments.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.attachments.forEach { att ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = att.name,
                                        style = MiuixTheme.textStyles.footnote1,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Text(
                                        text = "移除",
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = colors.error,
                                        modifier = Modifier.clickable {
                                            vm.removeAttachment(att.attId)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            uiState.error?.let { err ->
                item {
                    Text(
                        text = err,
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.error,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showFilePicker) {
        pdfFilePickerHost { bytes, name, mime ->
            showFilePicker = false
            vm.addAttachment(fileName = name, mimeType = mime, bytes = bytes)
        }
    }

    val actionMessage = uiState.actionMessage
    if (actionMessage != null) {
        WindowDialog(
            show = true,
            title = "提示",
            summary = actionMessage,
            onDismissRequest = {
                vm.consumeActionMessage()
                onSubmitted()
            }
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = {
                    vm.consumeActionMessage()
                    onSubmitted()
                }
            ) {
                Text(text = "知道了", color = colors.onPrimary)
            }
        }
    }
}
