package restarhalf.stellar.schedule.ui.components.screen.schedule

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.ui.icons.Change
import restarhalf.stellar.schedule.ui.icons.Edit
import restarhalf.stellar.schedule.ui.icons.Revert
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import restarhalf.stellar.schedule.ui.theme.StatusColors

/**
 * 课程详情项组件
 * 
 * 在课程详情弹窗中显示单个课程的详细信息，包括：
 * - 课程名称和标签（实验课、调课、非本周）
 * - 周次和节次时间
 * - 地点和教师
 * - 操作按钮（调课、编辑）
 * 
 * @param modifier Modifier修饰符
 * @param course 课程数据
 * @param isCurrent 是否为当前周课程
 * @param surfaceSoft 柔和表面颜色
 * @param uiState ViewModel UI状态
 * @param onEdit 编辑回调
 * @param onTrans 调课回调
 */
@Composable
fun CourseDetailItem(
    modifier: Modifier = Modifier,
    course: Course,
    isCurrent: Boolean,
    surfaceSoft: Color,
    textPrimary: Color,
    textSecondary: Color,
    detailUi: ScheduleViewModel.CourseDetailUi,
    onEditLabCourse: (Long) -> Unit,
    onTransCourse: (Course) -> Unit,
    onRevertTrans: (Long) -> Unit
) {

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(course.id) {
        animProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 300))
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .graphicsLayer {
                    alpha = animProgress.value

                    translationY = 50f * (1f - animProgress.value)
                }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = course.name,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                lineHeight = 24.sp
            )
            val tagText = detailUi.tagText
            if (tagText != null) {
                val tagBackground =
                    when (detailUi.tagStyle) {
                        ScheduleViewModel.CourseDetailTagStyle.LAB -> StatusColors.labTag.copy(0.8f)
                        ScheduleViewModel.CourseDetailTagStyle.TRANS -> StatusColors.transTag.copy(0.8f)
                        ScheduleViewModel.CourseDetailTagStyle.NON_CURRENT -> surfaceSoft
                        null -> surfaceSoft
                    }
                Box(
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(tagBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = tagText, fontSize = 11.sp, color = textSecondary)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = detailUi.weekLine,
                    fontSize = 12.sp,
                    color = textSecondary
                )

                Text(
                    text = detailUi.locationLine,
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }
            if (isCurrent) {
                when (course.type) {
                    1 -> {
                        IconButton(onClick = { onEditLabCourse(course.id) }) {
                            Icon(imageVector = Edit, contentDescription = "编辑实验课")
                        }
                    }

                    0 -> {
                        IconButton(onClick = { onTransCourse(course) }) {
                            Icon(imageVector = Change, contentDescription = "调课")
                        }
                    }

                    else -> {
                        IconButton(onClick = { onRevertTrans(course.id) }) {
                            Icon(imageVector = Revert, contentDescription = "撤回调课")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(15.dp))
    }
}