package restarhalf.stellar.schedule.ui.screens.evaluation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Delete
import restarhalf.stellar.schedule.ui.icons.Edit
import restarhalf.stellar.schedule.ui.chrome.AppPageScaffold
import restarhalf.stellar.schedule.ui.chrome.AppTopBarActionContainer
import restarhalf.stellar.schedule.ui.chrome.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import restarhalf.stellar.schedule.ui.components.LocalLiquidGlassBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.collapsibleTopInset
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun EvaluationDetailScreen(
    vm: CourseEvaluationViewModel,
    evaluationId: String,
    onBack: () -> Unit,
    onEditEvaluation: (String) -> Unit,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(evaluationId) {
        vm.loadDetail(evaluationId)
    }

    val evaluation = uiState.selectedEvaluation
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AppPageScaffold(
        title = "评价详情",
        onBack = onBack,
        actions = {
            if (evaluation != null && uiState.canDeleteSelected) {
                AppTopBarActionContainer {
                    IconButton(onClick = { onEditEvaluation(evaluation.id) }) {
                        Icon(
                            imageVector = Edit,
                            contentDescription = "编辑",
                            tint = colors.onBackground,
                        )
                    }
                }
                AppTopBarActionContainer {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Delete,
                            contentDescription = "删除",
                            tint = colors.onBackground,
                        )
                    }
                }
            }
        },
        topBarExtra = {
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                EvaluationStatusPill(text = uiState.error)
            }
        },
        popupHost = {
            if (showDeleteConfirm && evaluation != null) {
                OverlayDialog(
                    show = showDeleteConfirm,
                    liquidGlassBackdrop = LocalLiquidGlassBackdrop.current,
                    title = "删除评价",
                    summary = "删除后不可恢复，确定继续吗？",
                    onDismissRequest = { showDeleteConfirm = false },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showDeleteConfirm = false },
                        ) {
                            Text(text = "取消")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = {
                                showDeleteConfirm = false

                                vm.deleteEvaluation(evaluation.id) { deleted ->
                                    if (deleted) onBack()
                                }
                            },
                        ) {
                            Text(text = "确认删除", color = colors.onPrimary)
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (evaluation != null) {
                val isLoggedOut = uiState.userNo.isBlank()
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = if (evaluation.liked) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.buttonColorsPrimary()
                    },
                    onClick = { vm.toggleLike(evaluation.id) },
                ) {
                    Text(
                        text = when {
                            isLoggedOut -> "请先登录后再点赞"
                            evaluation.liked -> "已赞 ${evaluation.likes}"
                            else -> "点赞 · ${evaluation.likes}"
                        },
                        color = if (evaluation.liked) colors.onSurface else colors.onPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    ) { scrollBehavior, paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .collapsibleTopInset(scrollBehavior)
                .pageScrollModifiers(scrollBehavior = scrollBehavior),
            contentPadding = appPageContentPadding(
                innerPadding = paddingValues,
                outerPadding = paddingValues,
                extraTop = 12.dp,
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (uiState.loading && evaluation == null) {
                item(key = "loading") {
                    Text(
                        text = "加载中...",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }

            if (evaluation != null) {
                item(key = "hero") {
                    EvaluationDetailHero(evaluation = evaluation)
                }

                item(key = "content") {
                    SmallTitle(text = "评价内容")
                    AppCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = evaluation.content.ifEmpty { "（无内容）" },
                                modifier = Modifier.fillMaxWidth(),
                                style = MiuixTheme.textStyles.body2,
                                color = colors.onSurface,
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            val authorText = if (evaluation.anonymous) "匿名" else evaluation.author.ifEmpty { "匿名" }
                            BasicComponent(title = "作者", summary = authorText)
                            BasicComponent(
                                title = "发布时间",
                                summary = formatDate(evaluation.createdAt),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(epochSeconds: Long): String {
    if (epochSeconds <= 0) return "—"
    return runCatching {
        val dateTime = kotlin.time.Instant.fromEpochSeconds(epochSeconds)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        "${dateTime.date} ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
    }.getOrDefault("—")
}
