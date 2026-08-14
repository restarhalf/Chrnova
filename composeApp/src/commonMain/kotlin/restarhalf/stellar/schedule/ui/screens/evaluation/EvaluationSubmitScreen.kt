package restarhalf.stellar.schedule.ui.screens.evaluation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.domain.model.EvaluationCreateRequest
import restarhalf.stellar.schedule.domain.model.EvaluationUpdateRequest
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
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val MAX_CONTENT_LENGTH = 500

/**
 * 写评价 / 编辑评价页。
 *
 * 设计要点（参考 EMS 视觉语言 + 写评价特有的表单体验优化）：
 * - 顶栏返回 + 标题
 * - 顶部错误状态药丸（与列表页一致）
 * - 三个分组（课程 / 评分 / 署名），每组用 SmallTitle + AppCard
 * - 评分输入区域：星 + 实时分值 + 文案提示（点击星星时 StarRatingInput 自带放大回弹）
 * - 评价内容字数统计
 * - 提交按钮：底部全宽 primary，loading 时显示进度文案
 */
@Composable
fun EvaluationSubmitScreen(
    vm: CourseEvaluationViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    evaluationId: String? = null,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    val isEditMode = evaluationId != null

    val courseOptions = remember(uiState.myCourses) {
        uiState.myCourses.map { it.name }.distinct()
    }

    var selectedCourseName by remember { mutableStateOf<String?>(null) }
    var teacher by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var content by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var author by remember { mutableStateOf("") }

    // 编辑模式：进入页面后加载评价详情，并在首次拿到数据时预填表单
    LaunchedEffect(evaluationId) {
        if (evaluationId != null) {
            vm.loadDetail(evaluationId)
        }
    }
    val pendingEval = uiState.selectedEvaluation
    LaunchedEffect(pendingEval?.id, isEditMode) {
        if (isEditMode && pendingEval != null && pendingEval.id == evaluationId) {
            selectedCourseName = pendingEval.courseName
            teacher = pendingEval.teacher
            rating = pendingEval.rating
            content = pendingEval.content
            anonymous = pendingEval.anonymous
            author = pendingEval.author.ifEmpty { uiState.userNickname ?: uiState.profileName }
        }
    }

    // 新建模式：默认署名优先使用用户自定义昵称，其次档案姓名
    LaunchedEffect(uiState.userNickname, uiState.profileName) {
        if (!isEditMode && author.isEmpty()) {
            author = uiState.userNickname ?: uiState.profileName
        }
    }

    // 提交/编辑成功后返回上一页
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            onSubmitted()
        }
    }

    val selectedCourseIndex = remember(selectedCourseName, courseOptions) {
        val idx = courseOptions.indexOf(selectedCourseName)
        if (idx < 0) 0 else idx
    }

    val canSubmit = selectedCourseName != null &&
        rating in 1..5 &&
        content.isNotBlank() &&
        !uiState.submitting

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = if (isEditMode) "编辑评价" else "写评价",
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
                // 错误状态药丸
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    EvaluationStatusPill(text = uiState.error)
                }
            }
        },
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
                enabled = canSubmit,
                onClick = {
                    val course = selectedCourseName ?: return@Button
                    if (isEditMode) {
                        vm.updateEvaluation(
                            evaluationId,
                            EvaluationUpdateRequest(
                                teacher = teacher.trim(),
                                rating = rating,
                                content = content.trim(),
                                anonymous = anonymous,
                                author = if (anonymous) "" else author.trim(),
                            ),
                        )
                    } else {
                        vm.submitEvaluation(
                            EvaluationCreateRequest(
                                courseName = course,
                                teacher = teacher.trim(),
                                rating = rating,
                                content = content.trim(),
                                anonymous = anonymous,
                                author = if (anonymous) "" else author.trim(),
                            ),
                        )
                    }
                },
            ) {
                Text(
                    text = when {
                        uiState.submitting -> if (isEditMode) "保存中..." else "提交中..."
                        isEditMode -> "保存"
                        else -> "提交"
                    },
                    color = colors.onPrimary,
                    fontWeight = FontWeight.Medium,
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
                extraStart = 12.dp,
                extraEnd = 12.dp,
            ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!isEditMode && courseOptions.isEmpty()) {
                item(key = "no_courses") {
                    AppCard {
                        Text(
                            text = "你还没有已选课程，无法提交评价。请先在课表中同步你的课程。",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            style = MiuixTheme.textStyles.body2,
                            color = colors.error,
                        )
                    }
                }
            } else {
                item(key = "course") {
                    SmallTitle(text = "课程")
                    AppCard {
                        if (isEditMode) {
                            // 编辑模式：course_name 不可改，仅展示
                            BasicComponent(
                                title = "课程",
                                summary = selectedCourseName ?: "—",
                            )
                        } else {
                            OverlayDropdownPreference(
                                title = "选择课程",
                                summary = selectedCourseName ?: "请选择你要评价的课程",
                                items = courseOptions,
                                selectedIndex = selectedCourseIndex,
                                onSelectedIndexChange = { index ->
                                    val name = courseOptions.getOrNull(index)
                                    selectedCourseName = name
                                    // 预填教师：取已选课程中同名课程的教师
                                    teacher = uiState.myCourses
                                        .firstOrNull { it.name == name }?.teacher
                                        .orEmpty()
                                },
                            )
                        }
                    }
                }

                item(key = "review") {
                    SmallTitle(text = "评价内容")
                    AppCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TextField(
                                label = "教师（可选）",
                                value = teacher,
                                onValueChange = { teacher = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            HorizontalDivider()
                            // 评分区
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "评分",
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = colors.onSurfaceVariantSummary,
                                    )
                                    if (rating > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(colors.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "$rating",
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = colors.primary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Text(
                                            text = "/ 5",
                                            style = MiuixTheme.textStyles.footnote1,
                                            color = colors.onSurfaceVariantSummary,
                                        )
                                    }
                                }
                                StarRatingInput(rating = rating, onRatingChanged = { rating = it })
                                Text(
                                    text = ratingHint(rating),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = colors.onSurfaceVariantSummary.copy(alpha = 0.85f),
                                )
                            }
                            HorizontalDivider()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextField(
                                    label = "评价内容",
                                    value = content,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= MAX_CONTENT_LENGTH) {
                                            content = newValue
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 8,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    Text(
                                        text = "${content.length} / $MAX_CONTENT_LENGTH",
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = if (content.length >= MAX_CONTENT_LENGTH) {
                                            colors.error
                                        } else {
                                            colors.onSurfaceVariantSummary
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "authorship") {
                    SmallTitle(text = "署名")
                    AppCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SwitchPreference(
                                title = "匿名评价",
                                summary = "开启后不会显示你的名字",
                                checked = anonymous,
                                onCheckedChange = { anonymous = it },
                            )
                            if (!anonymous) {
                                HorizontalDivider()
                                TextField(
                                    label = "署名（可选，默认使用昵称）",
                                    value = author,
                                    onValueChange = { author = it },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 根据当前评分给出语义化提示 */
private fun ratingHint(rating: Int): String = when (rating) {
    0 -> "点击星星为这门课打分"
    1 -> "很不推荐，慎选"
    2 -> "不太推荐"
    3 -> "中规中矩"
    4 -> "推荐，质量不错"
    5 -> "强烈推荐，神课"
    else -> ""
}