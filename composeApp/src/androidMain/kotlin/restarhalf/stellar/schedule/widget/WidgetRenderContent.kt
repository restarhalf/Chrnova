@file:SuppressLint("RestrictedApi")

package restarhalf.stellar.schedule.widget

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.ui.theme.pickCourseSubColor
import kotlin.math.min
private val widgetBg = ColorProvider(R.color.widget_bg)

private val cardBg = ColorProvider(R.color.widget_divider)

private val widgetHeaderText = ColorProvider(R.color.widget_header_text)

private val widgetEmptyText = ColorProvider(R.color.widget_empty_text)

private val widgetTimeText = ColorProvider(R.color.widget_time_text)

private val widgetTimeSubText = ColorProvider(R.color.widget_time_sub_text)

private val widgetTitleText = ColorProvider(R.color.widget_title_text)

private val widgetSubtitleText = ColorProvider(R.color.widget_subtitle_text)

private val widgetStatusUpcoming = ColorProvider(R.color.widget_status_upcoming)

private val widgetStatusOngoing = ColorProvider(R.color.widget_status_ongoing)

private val rowBgNeutral = ColorProvider(R.color.widget_row_bg_neutral)

private val rowBgUpcoming = ColorProvider(R.color.widget_row_bg_upcoming)

private val rowBgOngoing = ColorProvider(R.color.widget_row_bg_ongoing)

@Composable
private fun widgetScale(
    baseWidthDp: Float,
    baseHeightDp: Float,
    minScale: Float = 1.0f,
    maxScale: Float = 1.4f,
): WidgetScale {
    val size = LocalSize.current
    val w = (size.width.value / baseWidthDp).coerceIn(minScale, maxScale)
    val h = (size.height.value / baseHeightDp).coerceIn(minScale, maxScale)
    val u = min(w, h)
    return WidgetScale(w = w, h = h, u = u)
}

private data class WidgetScale(val w: Float, val h: Float, val u: Float)

private fun scaledDpW(rawDp: Dp, scale: WidgetScale) =
    (rawDp.value * scale.w).dp

private fun scaledDpH(rawDp: Dp, scale: WidgetScale) =
    (rawDp.value * scale.h).dp

private fun scaledDpU(rawDp: Dp, scale: WidgetScale) =
    (rawDp.value * scale.u).dp

private fun scaledSp(rawSp: TextUnit, scale: WidgetScale) =
    (rawSp.value * scale.u).sp

@Composable
internal fun SmallWidgetContent(state: SmallWidgetState) {
    val scale = widgetScale(baseWidthDp = 160f, baseHeightDp = 160f)

    Box(
        modifier =
            GlanceModifier.fillMaxSize()
                .background(widgetBg)
                .cornerRadius(scaledDpU(20.dp, scale))
                .clickable(onClick = openHomeAction())
                .padding(horizontal = scaledDpW(14.dp, scale), vertical = scaledDpH(20.dp, scale)),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            HeaderRow(left = state.dayLabel, right = state.weekLabel)

            Spacer(modifier = GlanceModifier.height(scaledDpH(12.dp, scale)))

            if (state.nextCourse == null) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.emptyText,
                        style =
                            TextStyle(
                                color = widgetEmptyText,
                                fontSize = scaledSp(15.sp, scale),
                                fontWeight = FontWeight.Normal,
                            ),
                    )
                }
            } else {
                val course = state.nextCourse

                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Text(
                        text = course.title,
                        maxLines = 1,
                        style =
                            TextStyle(
                                color = widgetTitleText,
                                fontSize = scaledSp(15.sp, scale),
                                fontWeight = FontWeight.Medium,
                            ),
                    )

                    Spacer(modifier = GlanceModifier.height(scaledDpH(7.dp, scale)))

                    Text(
                        text = "◷ ${course.start}-${course.end}",
                        maxLines = 1,
                        style =
                            TextStyle(
                                color = widgetSubtitleText,
                                fontSize = scaledSp(13.sp, scale),
                            ),
                    )

                    Spacer(modifier = GlanceModifier.height(scaledDpH(4.dp, scale)))

                    Text(
                        text = "⌖ ${course.location}",
                        maxLines = 1,
                        style =
                            TextStyle(
                                color = widgetSubtitleText,
                                fontSize = scaledSp(13.sp, scale),
                            ),
                    )

                    Spacer(modifier = GlanceModifier.height(scaledDpH(16.dp, scale)))

                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Text(
                            text = if (course.restCount == 0) "这是最后一节课了哦" else "还有${course.restCount}节课",
                            style =
                                TextStyle(
                                    color = widgetTimeSubText,
                                    fontSize = scaledSp(10.sp, scale),
                                ),
                        )

                        Spacer(modifier = GlanceModifier.width(scaledDpW(4.dp, scale)))
                        for (i in 1..course.restCount) {
                            Spacer(
                                modifier =
                                    GlanceModifier.size(scaledDpU(4.dp, scale))
                                        .background(ColorProvider(R.color.widget_dot))
                                        .cornerRadius(scaledDpU(10.dp, scale)),
                            )

                            if (i != course.restCount) {
                                Spacer(modifier = GlanceModifier.width(scaledDpW(4.dp, scale)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LargeWidgetContent(state: LargeWidgetState) {
    val scale = widgetScale(baseWidthDp = 260f, baseHeightDp = 180f)

    Box(
        modifier =
            GlanceModifier.fillMaxSize()
                .background(widgetBg)
                .cornerRadius(scaledDpU(20.dp, scale))
                .clickable(onClick = openHomeAction())
                .padding(horizontal = scaledDpW(14.dp, scale), vertical = scaledDpH(20.dp, scale)),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            HeaderRow(left = state.headerLabel, right = state.weekLabel)

            Spacer(modifier = GlanceModifier.height(scaledDpH(10.dp, scale)))

            if (state.rows.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.emptyText ?: "今日课程已上完",
                        style =
                            TextStyle(
                                color = widgetEmptyText,
                                fontSize = scaledSp(20.sp, scale),
                            ),
                    )
                }
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    state.rows.forEachIndexed { index, row ->
                        CourseRow(row)

                        if (index != state.rows.lastIndex) {
                            Spacer(modifier = GlanceModifier.height(scaledDpH(8.dp, scale)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(left: String, right: String) {
    val scale = widgetScale(baseWidthDp = 260f, baseHeightDp = 60f)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Row(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher_round),
                contentDescription = null,
                modifier = GlanceModifier.size(scaledDpU(18.dp, scale)).cornerRadius(16.dp),
            )

            Spacer(modifier = GlanceModifier.width(scaledDpW(6.dp, scale)))

            Text(
                text = left,
                maxLines = 1,
                style = TextStyle(color = widgetHeaderText, fontSize = scaledSp(15.sp, scale)),
            )
        }

        Text(
            text = right,
            maxLines = 1,
            style = TextStyle(color = widgetHeaderText, fontSize = scaledSp(15.sp, scale)),
        )
    }
}

private fun openHomeAction() =
    actionStartActivity(
        Intent().apply {
            setClassName(
                "restarhalf.stellar.schedule",
                "restarhalf.stellar.schedule.MainActivity"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    )

@Composable
private fun CourseRow(row: LargeCourseRow) {
    val scale = widgetScale(baseWidthDp = 260f, baseHeightDp = 180f)

    val background =
        when (row.statusType) {
            StatusType.NONE -> cardBg
            StatusType.UPCOMING -> rowBgUpcoming
            StatusType.ONGOING -> rowBgOngoing
        }

    val statusColor =
        when (row.statusType) {
            StatusType.UPCOMING -> widgetStatusUpcoming
            StatusType.ONGOING -> widgetStatusOngoing
            StatusType.NONE -> widgetSubtitleText
        }

    Row(
        modifier =
            GlanceModifier.fillMaxWidth()
                .background(background)
                .cornerRadius(scaledDpU(14.dp, scale))
                .padding(horizontal = scaledDpW(10.dp, scale), vertical = scaledDpH(7.dp, scale)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.width(scaledDpW(45.dp, scale))) {
            Text(
                text = row.start,
                style =
                    TextStyle(
                        color = widgetTimeText,
                        fontSize = scaledSp(12.sp, scale),
                        fontWeight = FontWeight.Medium,
                    ),
            )

            Text(
                text = row.end,
                style = TextStyle(color = widgetTimeSubText, fontSize = scaledSp(12.sp, scale)),
            )
        }

        Spacer(
            modifier =
                GlanceModifier.width(scaledDpW(4.dp, scale))
                    .height(scaledDpH(36.dp, scale))
                    .background(pickCourseSubColor(row.title, false))
                    .cornerRadius(scaledDpU(6.dp, scale)),
        )

        Spacer(modifier = GlanceModifier.width(scaledDpW(10.dp, scale)))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = row.title,
                maxLines = 1,
                style =
                    TextStyle(
                        color = widgetTitleText,
                        fontSize = scaledSp(14.sp, scale),
                        fontWeight = FontWeight.Medium,
                    ),
            )

            Text(
                text = row.subtitle,
                maxLines = 1,
                style = TextStyle(color = widgetSubtitleText, fontSize = scaledSp(11.sp, scale)),
            )
        }

        if (!row.statusText.isNullOrBlank()) {
            Spacer(modifier = GlanceModifier.width(scaledDpW(6.dp, scale)))

            Text(
                text = row.statusText,
                maxLines = 1,
                style =
                    TextStyle(
                        color = statusColor,
                        fontSize = scaledSp(13.sp, scale),
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }
    }
}
