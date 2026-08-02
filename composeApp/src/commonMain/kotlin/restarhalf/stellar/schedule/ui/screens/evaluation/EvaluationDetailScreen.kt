package restarhalf.stellar.schedule.ui.screens.evaluation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EvaluationDetailScreen(
    vm: CourseEvaluationViewModel,
    evaluationId: String,
    onBack: () -> Unit,
    onEditEvaluation: (String) -> Unit,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    LaunchedEffect(evaluationId) {
        vm.loadDetail(evaluationId)
    }

    val evaluation = uiState.selectedEvaluation

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppPageTopBar(
                title = "评价详情", scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Back, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (evaluation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = { vm.toggleLike(evaluation.id) },
                    ) {
                        Text(
                            text = if (evaluation.liked) "已赞 ${evaluation.likes}"
                            else "点赞 ${evaluation.likes}",
                            color = colors.onPrimary,
                        )
                    }
                    if (uiState.canDeleteSelected) {
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(),
                            onClick = { onEditEvaluation(evaluation.id) },
                        ) {
                            Text(text = "编辑", color = colors.onSurface)
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(),
                            onClick = {
                                vm.deleteEvaluation(evaluation.id)
                                onBack()
                            },
                        ) {
                            Text(text = "删除", color = colors.onSurface)
                        }
                    }
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.loading) {
                item(key = "loading") {
                    Text(
                        text = "加载中...",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 14.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }

            if (uiState.error != null) {
                item(key = "error") {
                    Text(
                        text = uiState.error ?: "",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 14.sp,
                        color = colors.error,
                    )
                }
            }

            if (evaluation != null) {
                item(key = "hero") {
                    EvaluationHeroCard(evaluation = evaluation)
                }

                item(key = "content") {
                    SmallTitle(text = "评价内容")
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                text = evaluation.content.ifEmpty { "（无内容）" },
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 14.sp,
                                color = colors.onSurface,
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            val authorText = if (evaluation.anonymous) "匿名" else evaluation.author.ifEmpty { "匿名" }
                            BasicComponent(title = "作者", summary = authorText)
                            BasicComponent(
                                title = "发布时间",
                                summary = runCatching {
                                    kotlin.time.Instant.fromEpochSeconds(evaluation.createdAt)
                                        .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                                }.getOrDefault("-"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvaluationHeroCard(evaluation: Evaluation) {
    val colors = MiuixTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 课程名（主标题）
            Text(
                text = evaluation.courseName.ifEmpty { "未命名课程" },
                fontSize = 18.sp,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // 教师名（副标题）
            Text(
                text = evaluation.teacher.ifEmpty { "教师未知" },
                fontSize = 13.sp,
                color = colors.onSurfaceVariantSummary,
            )
            // 评分行：星级 + 分值 + 状态徽标
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRatingDisplay(rating = evaluation.rating, starSize = 20)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${evaluation.rating}/5",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariantSummary,
                )
            }
        }
    }
}
