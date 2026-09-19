package restarhalf.stellar.schedule.ui.screens.pe

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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.data.remote.PEFreeApplyItem
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.PEFreeApplyViewModel
import restarhalf.stellar.schedule.ui.viewmodel.freeApplyStatusText
import restarhalf.stellar.schedule.ui.viewmodel.freeApplyTypeText
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 免测/缓测申请记录
 *
 * 列表卡片与体测成绩页一致；底栏「我要申请」。
 */
@Composable
fun PEFreeApplyScreen(
    vm: PEFreeApplyViewModel,
    onLogin: () -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit,
) {
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val loggedIn by vm.isLoggedIn.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            vm.refresh()
        }
    }

    val statusText = when {
        uiState.error != null -> uiState.error
        !loggedIn -> null
        uiState.loaded && uiState.items.isEmpty() -> "暂无申请记录"
        else -> null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "免测申请",
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
        bottomBar = {
            if (loggedIn) {
                Button(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    onClick = onApply,
                ) {
                    Text(text = "我要申请", color = colors.onPrimary)
                }
            }
        },
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
                    outerPadding = appScaffoldPadding,
                    extraTop = 12.dp,
                    extraStart = 12.dp,
                    extraEnd = 12.dp,
                ),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!loggedIn) {
                    item {
                        AppCard {
                            ArrowPreference(
                                title = "登录",
                                summary = "登录后可查看免测/缓测申请",
                                onClick = onLogin
                            )
                        }
                    }
                }
                items(
                    uiState.items,
                    key = { it.applyId.ifBlank { "${it.schoolYear}-${it.freeApplyType}" } }
                ) { item ->
                    FreeApplyRecordCard(
                        item = item,
                        typeLabel = freeApplyTypeText(item.freeApplyType, uiState.typeLabelMap),
                        statusLabel = freeApplyStatusText(item.applyStatus, uiState.statusLabelMap),
                    )
                }
            }
        }
    }

    val actionMessage = uiState.actionMessage
    if (actionMessage != null) {
        WindowDialog(
            show = true,
            title = "提示",
            summary = actionMessage,
            onDismissRequest = { vm.consumeActionMessage() }
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = { vm.consumeActionMessage() }
            ) {
                Text(text = "知道了", color = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun FreeApplyRecordCard(
    item: PEFreeApplyItem,
    typeLabel: String,
    statusLabel: String,
) {
    val colors = MiuixTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.schoolYear + "【" + typeLabel + "】",
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold
                )
                if (item.applyTime.isNotBlank()) {
                    Text(
                        text = item.applyTime,
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceVariantSummary
                    )
                }
            }
            Text(
                text = statusLabel,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
