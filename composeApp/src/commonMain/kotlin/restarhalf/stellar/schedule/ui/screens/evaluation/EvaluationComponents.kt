package restarhalf.stellar.schedule.ui.screens.evaluation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.ui.icons.Favorite
import restarhalf.stellar.schedule.ui.icons.Star
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 评分星星的主色（暖琥珀，亮暗模式均可读） */
private val StarFilledColor = Color(0xFFFFB300)

/**
 * 只读星级展示：5 颗星，已得分为琥珀色，未得为浅灰。
 *
 * @param rating 0-5
 * @param starSize 单颗星尺寸
 */
@Composable
fun StarRatingDisplay(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: Int = 16,
) {
    val safe = rating.coerceIn(0, 5)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            val filled = i <= safe
            Icon(
                imageVector = Star,
                contentDescription = null,
                tint = if (filled) StarFilledColor else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.35f),
                modifier = Modifier.size(starSize.dp),
            )
            if (i < 5) Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

/**
 * 可点击星级输入：大触控区，点击即评分（1-5）。
 */
@Composable
fun StarRatingInput(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safe = rating.coerceIn(0, 5)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (i in 1..5) {
            val filled = i <= safe
            Icon(
                imageVector = Star,
                contentDescription = "$i 星",
                tint = if (filled) StarFilledColor else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onRatingChanged(i) }
                    .padding(4.dp),
            )
        }
    }
}

/**
 * 紧凑点赞按钮：心形图标 + 数量，已赞时填充主色。
 *
 * 列表项中使用，比完整 [top.yukonga.miuix.kmp.basic.Button] 视觉更轻。
 */
@Composable
fun LikeButton(
    liked: Boolean,
    likes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Favorite,
            contentDescription = if (liked) "已赞" else "点赞",
            tint = if (liked) colors.primary else colors.onSurfaceVariantSummary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = likes.toString(),
            fontSize = 12.sp,
            color = if (liked) colors.primary else colors.onSurfaceVariantSummary,
        )
    }
}

/**
 * 空状态：居中图标 + 主标题 + 副标题。
 */
@Composable
fun EvaluationEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = colors.onSurfaceVariantSummary,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.onSurfaceVariantSummary.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 评价卡片底部元信息行：作者（左）+ 任意右侧操作（如点赞）。
 */
@Composable
fun EvaluationMetaRow(
    authorText: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = authorText,
            fontSize = 12.sp,
            color = colors.onSurfaceVariantSummary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        trailing()
    }
}

/**
 * 评价列表项卡片：教师 + 星级 + 内容预览 + 作者/点赞。
 *
 * 用于"我的评价"、某课程评价列表等平铺场景。
 *
 * @param onClick 点击卡片（进入详情）
 * @param onLike 点击点赞
 */
@Composable
fun EvaluationListItem(
    evaluation: Evaluation,
    onClick: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
    showCourseName: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 标题行：课程名（可选，"我的评价"场景下需要显示）
            if (showCourseName) {
                Text(
                    text = evaluation.courseName.ifEmpty { "未命名课程" },
                    fontSize = 15.sp,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // 副标题行：教师 + 星级
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = evaluation.teacher.ifEmpty { "教师未知" },
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StarRatingDisplay(rating = evaluation.rating)
            }
            // 内容预览
            if (evaluation.content.isNotBlank()) {
                Text(
                    text = evaluation.content,
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariantSummary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider()
            // 底部元信息：作者 + 点赞
            EvaluationMetaRow(
                authorText = if (evaluation.anonymous) "匿名" else evaluation.author.ifEmpty { "匿名" },
            ) {
                LikeButton(
                    liked = evaluation.liked,
                    likes = evaluation.likes,
                    onClick = onLike,
                )
            }
        }
    }
}
