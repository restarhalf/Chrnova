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
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.text.DecimalFormatter
import restarhalf.stellar.schedule.domain.model.CourseEvaluationSummary
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.icons.Favorite
import restarhalf.stellar.schedule.ui.icons.Star
import restarhalf.stellar.schedule.ui.theme.StatusColors
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme


private val StarFilledColor = Color(0xFFFFB300)


private val StatusPillHeight = 28.dp


@Composable
private fun StarRatingDisplay(
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


@Composable
private fun HalfStarRatingDisplay(
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

                Icon(
                    imageVector = Star,
                    contentDescription = null,
                    tint = summary.copy(alpha = 0.35f),
                    modifier = Modifier.size(starSize.dp),
                )

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


@Composable
private fun LikeButton(
    liked: Boolean,
    likes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val heartScale = remember { Animatable(1f) }
    LaunchedEffect(likes) {

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
            style = MiuixTheme.textStyles.footnote1,
            color = if (liked) colors.primary else colors.onSurfaceVariantSummary,
        )
    }
}


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
                style = MiuixTheme.textStyles.footnote1,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
}


@Composable
private fun EvaluationTag(
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
            style = MiuixTheme.textStyles.footnote2,
            color = contentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}


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

                if (showCourseName) {
                    Text(
                        text = evaluation.courseName.ifEmpty { "未命名课程" },
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {

                    if (isMine) {
                        EvaluationTag(
                            text = "我的",
                            background = colors.primary.copy(alpha = 0.85f),
                            contentColor = colors.onPrimary,
                        )
                    }
                    Text(
                        text = evaluation.teacher.ifEmpty { "教师未知" },
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    StarRatingDisplay(rating = evaluation.rating, starSize = 13)
                }


                if (evaluation.content.isNotBlank()) {
                    Text(
                        text = evaluation.content,
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onSurfaceVariantSummary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HorizontalDivider()


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (evaluation.anonymous) "匿名" else evaluation.author.ifEmpty { "匿名" },
                        style = MiuixTheme.textStyles.footnote1,
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = summary.courseName.ifEmpty { "未命名课程" },
                    style = MiuixTheme.textStyles.body1,
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


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = DecimalFormatter.format(summary.avgRating, 1),
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                    HalfStarRatingDisplay(rating = summary.avgRating, starSize = 11)
                    Text(
                        text = "平均分",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(colors.outline.copy(alpha = 0.4f)),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = summary.evalCount.toString(),
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = "条评价",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(colors.outline.copy(alpha = 0.4f)),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatLatestTime(summary.latestAt),
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = "最近评价",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}


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
                    style = MiuixTheme.textStyles.title4,
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
                            style = MiuixTheme.textStyles.title1,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            HalfStarRatingDisplay(rating = summary.avgRating, starSize = 14)
                            Text(
                                text = "${summary.evalCount} 条评价",
                                style = MiuixTheme.textStyles.footnote1,
                                color = colors.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
        }
    }
}


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
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (evaluation.teacher.isNotBlank()) {
                    EvaluationTag(text = evaluation.teacher)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${evaluation.rating}",
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold,
                        color = StarFilledColor,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        StarRatingDisplay(rating = evaluation.rating, starSize = 16)
                        Text(
                            text = "/ 5 分",
                            style = MiuixTheme.textStyles.footnote2,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}


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