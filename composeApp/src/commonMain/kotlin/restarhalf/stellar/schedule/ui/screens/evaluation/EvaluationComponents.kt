package restarhalf.stellar.schedule.ui.screens.evaluation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import restarhalf.stellar.schedule.core.text.DecimalFormatter
import restarhalf.stellar.schedule.domain.model.CourseEvaluationSummary
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Favorite
import restarhalf.stellar.schedule.ui.icons.Star
import restarhalf.stellar.schedule.ui.theme.StatusColors
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import kotlinx.datetime.toLocalDateTime
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 评分星星的主色（暖琥珀，亮暗模式均可读） */
private val StarFilledColor = Color(0xFFFFB300)

/** 顶部状态药丸的高度，参考 EMS 的 statusText */
private val StatusPillHeight = 28.dp

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
 * 半星评分展示：用于显示带小数的平均分（如 4.6）。
 *
 * 渲染策略：用 clip 把每颗星裁成两半，左半填充表示 floor，右半按小数比例填充。
 *
 * @param rating 0.0-5.0
 * @param starSize 单颗星尺寸
 */
@Composable
fun HalfStarRatingDisplay(
    rating: Double,
    modifier: Modifier = Modifier,
    starSize: Int = 14,
) {
    val safe = rating.coerceIn(0.0, 5.0)
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            val fillRatio = (safe - (i - 1)).coerceIn(0.0, 1.0).toFloat()
            Box(modifier = Modifier.size(starSize.dp)) {
                // 底色：空星
                Icon(
                    imageVector = Star,
                    contentDescription = null,
                    tint = summary.copy(alpha = 0.35f),
                    modifier = Modifier.size(starSize.dp),
                )
                // 填充层：按比例裁剪
                if (fillRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillRatio)
                    ) {
                        Icon(
                            imageVector = Star,
                            contentDescription = null,
                            tint = StarFilledColor,
                            modifier = Modifier.size(starSize.dp),
                        )
                    }
                }
            }
            if (i < 5) Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

/**
 * 可点击星级输入：大触控区，点击即评分（1-5），带放大回弹反馈。
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
            // 每颗星独立的 scale 状态，点击瞬间放大回弹
            val scale = remember { Animatable(1f) }
            LaunchedEffect(safe) {
                if (i == safe && safe > 0) {
                    scale.animateTo(
                        targetValue = 1.25f,
                        animationSpec = tween(120),
                    )
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    )
                } else {
                    scale.snapTo(1f)
                }
            }
            Icon(
                imageVector = Star,
                contentDescription = "$i 星",
                tint = if (filled) StarFilledColor else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
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
 * 点击时心形弹跳 + 数字 pop 反馈。
 */
@Composable
fun LikeButton(
    liked: Boolean,
    likes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val heartScale = remember { Animatable(1f) }
    LaunchedEffect(likes) {
        // 点赞数变化时弹一下
        heartScale.animateTo(1.3f, tween(120))
        heartScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
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
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    scaleX = heartScale.value
                    scaleY = heartScale.value
                },
        )
        Text(
            text = likes.toString(),
            fontSize = 12.sp,
            color = if (liked) colors.primary else colors.onSurfaceVariantSummary,
        )
    }
}

/**
 * 顶部状态药丸：参考 EMS 顶栏下方的状态条（圆形圆角 28dp 高）。
 *
 * 用于显示"加载中 / 错误 / 提示"等短文本，自动隐藏 null。
 */
@Composable
fun EvaluationStatusPill(
    text: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    if (text.isNullOrBlank()) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(StatusPillHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * 圆角小徽标：参考 EMS GradeItemCard 的"挂科/补考"标签样式。
 *
 * 用于评价列表项上的"匿名""我的""热门"等标识。
 *
 * @param text 标签文本
 * @param background 背景色（默认 warning 中性灰）
 * @param contentColor 文本色（默认 onSurface）
 */
@Composable
fun EvaluationTag(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = StatusColors.neutral.copy(alpha = 0.85f),
    contentColor: Color = MiuixTheme.colorScheme.onSecondary,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 评价列表项卡片：左侧彩色强调条 + 课程/教师/评分 + 内容预览 + 作者/点赞。
 *
 * 视觉风格参考 EMS 的 ExamItemCard / GradeItemCard。
 *
 * @param onClick 点击卡片（进入详情）
 * @param onLike 点击点赞
 * @param showCourseName 是否在顶部展示课程名（"我的评价"模式需要）
 * @param isMine 是否本人提交（用于显示"我的"徽标）
 * @param accentColor 左侧强调条颜色（按课程名取色保持一致）
 */
@Composable
fun EvaluationListItem(
    evaluation: Evaluation,
    onClick: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
    showCourseName: Boolean = false,
    isMine: Boolean = false,
    accentColor: Color = pickCourseSubColor(evaluation.courseName.ifBlank { evaluation.id }, false),
) {
    val animProgress = remember { Animatable(0f) }
    val colors = MiuixTheme.colorScheme
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(durationMillis = 300))
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = 50f * (1f - animProgress.value)
            }
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 左侧彩色强调条
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clip(CircleShape)
                    .background(accentColor)
                    .width(4.dp)
                    .fillMaxHeight(0.8f),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // 课程名（仅在"我的评价"模式展示）
                if (showCourseName) {
                    Text(
                        text = evaluation.courseName.ifEmpty { "未命名课程" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // 教师 + 评分 + 状态徽标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // "我的"徽标（仅本人提交时显示）
                    if (isMine) {
                        EvaluationTag(
                            text = "我的",
                            background = colors.primary.copy(alpha = 0.85f),
                            contentColor = colors.onPrimary,
                        )
                    }
                    Text(
                        text = evaluation.teacher.ifEmpty { "教师未知" },
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    StarRatingDisplay(rating = evaluation.rating, starSize = 13)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (evaluation.anonymous) "匿名" else evaluation.author.ifEmpty { "匿名" },
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    LikeButton(
                        liked = evaluation.liked,
                        likes = evaluation.likes,
                        onClick = onLike,
                    )
                }
            }
        }
    }
}

/**
 * 课程聚合摘要卡片：参考 EMS GradeSummaryCard 三段式布局。
 *
 * 顶部展示课程名 + 教师，下面三段式：[平均分 | 评价数 | 最新时间]，
 * 配以半星展示 + 大数字。
 *
 * @param summary 课程聚合数据
 * @param onClick 点击进入该课程的评价列表
 */
@Composable
fun CourseSummaryCard(
    summary: CourseEvaluationSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = pickCourseSubColor(summary.courseName.ifBlank { summary.teacher }, false),
) {
    val animProgress = remember { Animatable(0f) }
    val colors = MiuixTheme.colorScheme
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(durationMillis = 300))
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = 50f * (1f - animProgress.value)
            }
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 顶部：课程名 + 教师
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = summary.courseName.ifEmpty { "未命名课程" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary.teacher.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EvaluationTag(text = summary.teacher)
                    }
                }
            }

            // 三段式摘要：平均分 | 评价数 | 最新时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 平均分（大数字 + 半星）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = DecimalFormatter.format(summary.avgRating, 1),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                    HalfStarRatingDisplay(rating = summary.avgRating, starSize = 11)
                    Text(
                        text = "平均分",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(colors.outline.copy(alpha = 0.4f)),
                )
                // 评价数
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = summary.evalCount.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = "条评价",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(colors.outline.copy(alpha = 0.4f)),
                )
                // 最新时间（相对时间）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatLatestTime(summary.latestAt),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = "最近评价",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

/**
 * 评分大数字 + 半星卡片：用于 EvaluationCourseScreen 顶部"这门课"的强烈视觉锚点。
 *
 * 设计上比 CourseSummaryCard 更突出，因为这是某个具体课程的二级页面。
 */
@Composable
fun CourseHeroCard(
    courseName: String,
    teacher: String,
    summary: CourseEvaluationSummary?,
    modifier: Modifier = Modifier,
    accentColor: Color = pickCourseSubColor(courseName.ifBlank { teacher }, false),
) {
    val animProgress = remember { Animatable(0f) }
    val colors = MiuixTheme.colorScheme
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(durationMillis = 300))
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = 50f * (1f - animProgress.value)
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = courseName.ifEmpty { "未命名课程" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (teacher.isNotBlank()) {
                    EvaluationTag(text = teacher)
                }
                if (summary != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = DecimalFormatter.format(summary.avgRating, 1),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            HalfStarRatingDisplay(rating = summary.avgRating, starSize = 14)
                            Text(
                                text = "${summary.evalCount} 条评价",
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 评价详情页 Hero 卡片：单条评价的视觉锚点。
 */
@Composable
fun EvaluationDetailHero(
    evaluation: Evaluation,
    modifier: Modifier = Modifier,
    accentColor: Color = pickCourseSubColor(evaluation.courseName.ifBlank { evaluation.id }, false),
) {
    val animProgress = remember { Animatable(0f) }
    val colors = MiuixTheme.colorScheme
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, tween(durationMillis = 300))
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animProgress.value
                translationY = 50f * (1f - animProgress.value)
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = evaluation.courseName.ifEmpty { "未命名课程" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (evaluation.teacher.isNotBlank()) {
                    EvaluationTag(text = evaluation.teacher)
                }
                // 评分行：大数字 + 半星 + 5分制
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${evaluation.rating}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarFilledColor,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        StarRatingDisplay(rating = evaluation.rating, starSize = 16)
                        Text(
                            text = "/ 5 分",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

/** 把 Unix 秒时间戳格式化成相对友好的简短日期（"刚刚"/"X 天前"/"YYYY-MM-DD"） */
private fun formatLatestTime(epochSeconds: Long): String {
    if (epochSeconds <= 0) return "—"
    return runCatching {
        val nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val tsMs = epochSeconds * 1000L
        val diff = nowMs - tsMs
        when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000} 分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
            diff < 30L * 86_400_000 -> "${diff / 86_400_000} 天前"
            else -> {
                val date = kotlin.time.Instant.fromEpochSeconds(epochSeconds)
                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                date.toString()
            }        }
    }.getOrDefault("—")
}