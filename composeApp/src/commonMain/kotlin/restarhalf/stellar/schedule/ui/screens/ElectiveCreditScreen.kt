package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.core.text.DecimalFormatter
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.screen.ems.GradeDetailsDialog
import restarhalf.stellar.schedule.ui.components.screen.ems.GradeItemCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.domain.usecase.CalculateElectiveCreditsUseCase
import restarhalf.stellar.schedule.ui.viewmodel.ElectiveCreditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect

/**
 * 选修课学分统计屏幕
 *
 * 显示X1-X5类别的选修课学分统计，支持：
 * - 学分统计概览
 * - 各类别学分详情
 * - 课程列表展开/收起
 * - 成绩详情弹窗
 * - 下拉刷新
 */
@Composable
fun ElectiveCreditScreen(
    vm: ElectiveCreditViewModel,
    onBack: () -> Unit,
) {
    val uiState by vm.uiState.collectAsState()

    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val colors = MiuixTheme.colorScheme
    val overscrollEffect = MiuixOverscrollEffect()

    val showGradeDetailsDialog = remember { mutableStateOf(false) }
    var selectedGrade by remember { mutableStateOf<GradeCourse?>(null) }

    LaunchedEffect(Unit) {
        vm.load()
    }

    LaunchedEffect(showGradeDetailsDialog.value) {
        if (!showGradeDetailsDialog.value) selectedGrade = null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                AppPageTopBar(
                    title = "选修课学分统计",
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector = Back,
                                contentDescription = ""
                            )
                        }
                    }
                )
                AnimatedVisibility(
                    visible = uiState.error.isNotBlank(),
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
                            Text(fontSize = 12.sp, text = uiState.error)
                        }
                    }
                }
            }

        },
        popupHost = {
            if (showGradeDetailsDialog.value) {
                selectedGrade?.let { grade ->
                    GradeDetailsDialog(
                        show = showGradeDetailsDialog.value,
                        onDismissRequest = { showGradeDetailsDialog.value = false },
                        title = grade.courseName.ifBlank { "未命名课程" },
                        summary = buildGradeDetailsSummary(grade)
                    )
                }
            }
        }
    ) { paddingValues ->
        PullToRefresh(
            isRefreshing = uiState.loading,
            onRefresh = { vm.load() },
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
                    extraStart = 16.dp,
                    extraEnd = 16.dp,
                ),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                overscrollEffect = overscrollEffect
            ) {
                // 各类别学分卡片
                items(uiState.categories, key = { it.code }) { category ->
                    CategoryCard(
                        category = category,
                        onCourseClick = { grade ->
                            selectedGrade = grade
                            showGradeDetailsDialog.value = true
                        }
                    )
                }
            }
        }
    }
}

/**
 * 类别学分卡片
 */
@Composable
private fun CategoryCard(
    category: CalculateElectiveCreditsUseCase.CreditCategory,
    onCourseClick: (GradeCourse) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 类别头部
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.code,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category.name,
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariantSummary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCredits(category.credits),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${category.courses.size}门课程",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariantSummary
                    )
                }
            }

            // 课程列表（展开时显示）
            AnimatedVisibility(
                visible = expanded && category.courses.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    category.courses.forEach { course ->
                        GradeItemCard(
                            card = buildGradeCardUi(course),
                            onClick = { onCourseClick(course) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 构建成绩卡片UI
 */
private fun buildGradeCardUi(grade: GradeCourse): GradeViewModel.GradeCardUi {
    return GradeViewModel.GradeCardUi(
        idKey = grade.gradeId.ifBlank { grade.courseCode + grade.courseName },
        grade = grade,
        title = grade.courseName.ifBlank { "未命名课程" },
        subtitle = listOf(
            grade.courseCode.takeIf { it.isNotBlank() },
            "学分:${grade.credit}",
        ).joinToString(" · ").ifBlank { "暂无补充信息" },
        scoreText = grade.score.ifBlank { grade.gradeLevel.ifBlank { grade.passStatus.ifBlank { "--" } } },
        jdText = "绩点：${DecimalFormatter.format(grade.gradePoint, 1)}",
        isRetakeExam = grade.examinationNature.contains("补考"),
        isFailed = grade.score.toDoubleOrNull()?.let { it < 60.0 } ?: false
    )
}

/**
 * 格式化学分显示
 */
private fun formatCredits(credits: Double): String {
    return if (credits % 1.0 == 0.0) {
        credits.toInt().toString()
    } else {
        DecimalFormatter.format(credits, 1)
    }
}

/**
 * 构建成绩详情摘要
 */
private fun buildGradeDetailsSummary(grade: GradeCourse): String {
    return buildString {
        appendLine("课程号：${grade.courseCode.ifBlank { "暂无" }}")
        appendLine("成绩：${grade.score.ifBlank { grade.gradeLevel.ifBlank { grade.passStatus.ifBlank { "--" } } }}")
        appendLine("学分：${grade.credit}")
        appendLine("绩点：${DecimalFormatter.format(grade.gradePoint, 1)}")
        appendLine("课程属性：${grade.curriculumAttributes.ifBlank { "暂无" }}")
        appendLine("课程性质：${grade.courseNature.ifBlank { "暂无" }}")
        appendLine("考核方式：${grade.examName.ifBlank { "暂无" }}")
        appendLine("考试性质：${grade.examinationNature.ifBlank { "暂无" }}")
        appendLine("是否及格：${grade.passStatus.ifBlank { "暂无" }}")
        if (grade.gradeLevel.isNotBlank()) appendLine("等级：${grade.gradeLevel}")
        if (grade.repeatSemester.isNotBlank()) appendLine("补重修学期：${grade.repeatSemester}")
        if (grade.markFlag.isNotBlank()) appendLine("成绩标识：${grade.markFlag}")
        if (grade.semester.isNotBlank()) appendLine("开课学期：${grade.semester}")
    }.trim()
}
