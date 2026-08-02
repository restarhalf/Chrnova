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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import restarhalf.stellar.schedule.domain.model.EvaluationCreateRequest
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Back
import restarhalf.stellar.schedule.ui.navigation.AppPageTopBar
import restarhalf.stellar.schedule.ui.navigation.LocalAppScaffoldPadding
import restarhalf.stellar.schedule.ui.navigation.appPageContentPadding
import restarhalf.stellar.schedule.ui.navigation.pageScrollModifiers
import restarhalf.stellar.schedule.ui.navigation.rememberAppPageScrollBehavior
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
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

@Composable
fun EvaluationSubmitScreen(
    vm: CourseEvaluationViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val appScaffoldPadding = LocalAppScaffoldPadding.current
    val topAppBarScrollBehavior = rememberAppPageScrollBehavior()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    val courseOptions = remember(uiState.myCourses) {
        uiState.myCourses.map { it.name }.distinct()
    }

    var selectedCourseName by remember { mutableStateOf<String?>(null) }
    var teacher by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var content by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var author by remember { mutableStateOf("") }

    // 默认署名优先使用用户自定义昵称，其次档案姓名
    LaunchedEffect(uiState.userNickname, uiState.profileName) {
        if (author.isEmpty()) {
            author = uiState.userNickname ?: uiState.profileName
        }
    }

    // 提交成功后返回上一页
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
            AppPageTopBar(
                title = "写评价", scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Back, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
                enabled = canSubmit,
                onClick = {
                    val course = selectedCourseName ?: return@Button
                    vm.submitEvaluation(
                        EvaluationCreateRequest(
                            courseName = course,
                            teacher = teacher.trim(),
                            rating = rating,
                            content = content.trim(),
                            anonymous = anonymous,
                            author = if (anonymous) "" else author.trim(),
                            userNo = uiState.userNo,
                        )
                    )
                },
            ) {
                Text(text = if (uiState.submitting) "提交中..." else "提交", color = colors.onPrimary)
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (courseOptions.isEmpty()) {
                item(key = "no_courses") {
                    AppCard {
                        Text(
                            text = "你还没有已选课程，无法提交评价。请先在课表中同步你的课程。",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            fontSize = 14.sp,
                            color = colors.error,
                        )
                    }
                }
            } else {
                item(key = "course") {
                    SmallTitle(text = "课程")
                    AppCard {
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
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "评分",
                                        fontSize = 13.sp,
                                        color = colors.onSurfaceVariantSummary,
                                    )
                                    if (rating > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$rating/5",
                                            fontSize = 13.sp,
                                            color = colors.onSurfaceVariantSummary,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                StarRatingInput(rating = rating, onRatingChanged = { rating = it })
                            }
                            HorizontalDivider()
                            TextField(
                                label = "评价内容",
                                value = content,
                                onValueChange = { content = it },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 8,
                            )
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

            if (uiState.error != null) {
                item(key = "error") {
                    AppCard {
                        Text(
                            text = uiState.error ?: "",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            fontSize = 14.sp,
                            color = colors.error,
                        )
                    }
                }
            }
        }
    }
}
