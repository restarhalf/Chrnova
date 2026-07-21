package restarhalf.stellar.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.theme.StatusColors
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.Platform
import top.yukonga.miuix.kmp.utils.platform
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import kotlin.time.Clock

@Composable
fun LogScreen(
    onBack: () -> Unit,
    onExport: (fileName: String, content: String) -> Unit = { _, _ -> },
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val entries by AppLogger.entries.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val appInfo: AppInfoPort = koinInject()
    var selectedEntry by remember { mutableStateOf<AppLogger.LogEntry?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme

    Scaffold(
        topBar = {
            AppPageTopBar(
                title = "日志",
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Back,
                            contentDescription = "返回",
                            tint = colors.onBackground,
                        )
                    }
                },
            )
        },
        popupHost = {
            if (showClearConfirm) {
                OverlayDialog(
                    show = showClearConfirm,
                    title = "确认清空",
                    summary = "清空后不可恢复，确定要清空所有日志吗？",
                    onDismissRequest = { showClearConfirm = false }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showClearConfirm = false }
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = {
                                AppLogger.clear()
                                showClearConfirm = false
                                onBack()
                            }
                        ) {
                            Text(text = "确定", color = colors.onPrimary)
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            start =
                                paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = 0.dp
                        )
                    )
                    .pageScrollModifiers(scrollBehavior = topAppBarScrollBehavior),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            appPageContentPadding(
                                innerPadding = PaddingValues(),
                                outerPadding = appScaffoldPadding,
                                extraTop = 8.dp,
                                extraStart = 12.dp + WindowInsets.displayCutout
                                    .asPaddingValues()
                                    .calculateStartPadding(LocalLayoutDirection.current),
                                extraEnd = 12.dp + WindowInsets.displayCutout
                                    .asPaddingValues()
                                    .calculateEndPadding(LocalLayoutDirection.current),
                            )
                        ),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(entries.reversed(), key = { it.id }) { entry ->
                    LogEntryCard(
                        entry = entry,
                        onClick = { selectedEntry = entry },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                        ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showClearConfirm = true
                    },
                ) {
                    Text(text = "清空")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = {
                        val metadata = mapOf(
                            "App" to appInfo.appName,
                            "Version" to appInfo.versionName,
                            "Platform" to platformName(),
                        )
                        val text = AppLogger.toExportText(metadata)
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        val ts = "${now.date}T${now.time.hour.toString().padStart(2, '0')}:${now.time.minute.toString().padStart(2, '0')}:${now.time.second.toString().padStart(2, '0')}.${now.time.nanosecond.toString().take(5)}"
                        val fileName = "Chrnova-$ts.log"
                        onExport(fileName, text)
                    },
                ) {
                    Text(text = "导出", color = colors.onPrimary)
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        LogDetailBottomSheet(
            entry = entry,
            onDismiss = { selectedEntry = null },
        )
    }
}

@Composable
private fun LogEntryCard(
    entry: AppLogger.LogEntry,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val levelColor = when (entry.level) {
        AppLogger.Level.DEBUG -> colors.secondary
        AppLogger.Level.INFO -> colors.onSurfaceVariantSummary
        AppLogger.Level.WARN -> StatusColors.warning
        AppLogger.Level.ERROR -> colors.error
    }

    AppCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier.align(Alignment.CenterVertically)
                        .clip(CircleShape)
                        .background(levelColor)
                        .width(3.dp)
                        .fillMaxHeight(0.8f),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "[${entry.level.tag}/${entry.tag}]",
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = levelColor,
                    )
                    Text(
                        text = entry.timestamp,
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = colors.onSurfaceVariantSummary.copy(alpha = 0.6f),
                    )
                }
                Text(
                    text = entry.message.substringBefore("\n"),
                    style = MiuixTheme.textStyles.body2.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LogDetailBottomSheet(
    entry: AppLogger.LogEntry,
    onDismiss: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val show = remember { mutableStateOf(true) }
    val levelColor = when (entry.level) {
        AppLogger.Level.DEBUG -> colors.secondary
        AppLogger.Level.INFO -> colors.onSurfaceVariantSummary
        AppLogger.Level.WARN -> StatusColors.warning
        AppLogger.Level.ERROR -> colors.error
    }

    WindowBottomSheet(
        show = show.value,
        modifier = Modifier,
        title = "[${entry.level.tag}/${entry.tag}]",
        startAction = null,
        endAction = null,
        backgroundColor = BottomSheetDefaults.backgroundColor(),
        enableWindowDim = true,
        cornerRadius = BottomSheetDefaults.cornerRadius,
        sheetMaxWidth = BottomSheetDefaults.maxWidth,
        onDismissRequest = {
            show.value = false
            onDismiss()
        },
        onDismissFinished = null,
        outsideMargin = BottomSheetDefaults.outsideMargin,
        insideMargin = BottomSheetDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        dragHandleColor = colors.surface,
        allowDismiss = true,
        enableNestedScroll = true,
        content = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
            ) {
                Text(
                    text = entry.timestamp,
                    style = MiuixTheme.textStyles.body2.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = colors.onSurfaceVariantSummary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                            .squircleBackground(colors.surfaceContainerHigh, 8.dp)
                            .padding(12.dp),
                ) {
                    Text(
                        text = entry.message,
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                        ),
                        color = levelColor,
                    )
                }
            }
        },
    )
}

private fun platformName(): String = when (platform()) {
    Platform.Android -> "Android"
    Platform.IOS -> "iOS"
    else -> "Unknown"
}
