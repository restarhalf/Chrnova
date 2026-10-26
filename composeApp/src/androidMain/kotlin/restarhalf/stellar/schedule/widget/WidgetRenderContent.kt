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
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import restarhalf.stellar.schedule.R
import kotlin.math.min

private val widgetBg = ColorProvider(R.color.widget_bg)

private val widgetBgSmall = ColorProvider(R.color.widget_bg_small)

private val rowBgNeutral = ColorProvider(R.color.widget_row_bg_neutral)

private val widgetHeaderText = ColorProvider(R.color.widget_header_text)

private val widgetEmptyText = ColorProvider(R.color.widget_empty_text)

private val widgetTimeText = ColorProvider(R.color.widget_time_text)

private val widgetTimeSubText = ColorProvider(R.color.widget_time_sub_text)

private val widgetTitleText = ColorProvider(R.color.widget_title_text)

private val widgetSubtitleText = ColorProvider(R.color.widget_subtitle_text)

private val widgetStatusUpcoming = ColorProvider(R.color.widget_status_upcoming)

private val widgetStatusOngoing = ColorProvider(R.color.widget_status_ongoing)

private val rowBgUpcoming = ColorProvider(R.color.widget_row_bg_upcoming)

private val rowBgOngoing = ColorProvider(R.color.widget_row_bg_ongoing)

private val WIDGET_RADIUS = 16.3.dp
private val WIDGET_PADDING_H = 14.dp
private val WIDGET_PADDING_V = 14.dp
private val HEADER_GAP = 12.dp

private val ROW_RADIUS = 10.91.dp
private val ROW_PADDING_H = 9.1.dp
private val ROW_PADDING_V = 4.dp
private val ROW_GAP = 6.dp

private val TIME_COL_W = 38.dp
private val BAR_W = 3.64.dp
private val BAR_H = 29.9.dp
private val BAR_TO_TITLE = 10.18.dp

private val DOT_SIZE = 3.64.dp
private val DOT_GAP = 4.73.dp
private const val MAX_DOTS = 3

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
                .background(widgetBgSmall)
                .cornerRadius(scaledDpU(WIDGET_RADIUS, scale))
                .clickable(onClick = openHomeAction())
                .padding(
                    horizontal = scaledDpW(WIDGET_PADDING_H, scale),
                    vertical = scaledDpH(WIDGET_PADDING_V, scale),
                ),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            HeaderRow(
                left = state.dayLabel,
                right = state.weekLabel,
                scale = scale,
                showIcon = true,
            )

            Spacer(modifier = GlanceModifier.height(scaledDpH(HEADER_GAP, scale)))

            if (state.nextCourse == null) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.emptyText,
                        style =
                            TextStyle(
                                color = widgetEmptyText,
                                fontSize = scaledSp(12.sp, scale),
                                fontWeight = FontWeight.Bold,
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

                    Spacer(modifier = GlanceModifier.height(scaledDpH(8.8.dp, scale)))

                    IconTextRow(
                        iconRes = R.drawable.ic_widget_clock,
                        text = "${course.start}-${course.end}",
                        scale = scale,
                    )

                    Spacer(modifier = GlanceModifier.height(scaledDpH(3.64.dp, scale)))

                    if (course.location.isNotBlank()) {
                        IconTextRow(
                            iconRes = R.drawable.ic_widget_pin,
                            text = course.location,
                            scale = scale,
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    FooterRow(restCount = course.restCount, dots = course.dotColors, scale = scale)
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
                .cornerRadius(scaledDpU(WIDGET_RADIUS, scale))
                .clickable(onClick = openHomeAction())
                .padding(
                    horizontal = scaledDpW(WIDGET_PADDING_H, scale),
                    vertical = scaledDpH(WIDGET_PADDING_V, scale),
                ),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            HeaderRow(
                left = state.headerLabel,
                right = state.weekLabel,
                scale = scale,
                showIcon = true,
            )

            Spacer(modifier = GlanceModifier.height(scaledDpH(HEADER_GAP, scale)))

            if (state.rows.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.emptyText ?: "今日课程已上完",
                        style =
                            TextStyle(
                                color = widgetEmptyText,
                                fontSize = scaledSp(12.sp, scale),
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }
            } else {

                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    state.rows.forEachIndexed { index, row ->
                        CourseRow(row, scale)

                        if (index != state.rows.lastIndex) {
                            Spacer(modifier = GlanceModifier.height(scaledDpH(ROW_GAP, scale)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    left: String,
    right: String,
    scale: WidgetScale,
    showIcon: Boolean,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Row(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            if (showIcon) {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher_round),
                    contentDescription = null,
                    modifier =
                        GlanceModifier.size(scaledDpU(14.55.dp, scale))
                            .cornerRadius(scaledDpU(7.dp, scale)),
                )

                Spacer(modifier = GlanceModifier.width(scaledDpW(4.dp, scale)))
            }

            Text(
                text = left,
                maxLines = 1,
                style =
                    TextStyle(
                        color = widgetHeaderText,
                        fontSize = scaledSp(14.sp, scale),
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }

        Text(
            text = right,
            maxLines = 1,
            style =
                TextStyle(
                    color = widgetHeaderText,
                    fontSize = scaledSp(14.sp, scale),
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}

@Composable
private fun IconTextRow(iconRes: Int, text: String, scale: WidgetScale) {
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(scaledDpU(8.73.dp, scale)),
        )

        Spacer(modifier = GlanceModifier.width(scaledDpW(4.36.dp, scale)))

        Text(
            text = text,
            maxLines = 1,
            style =
                TextStyle(
                    color = widgetSubtitleText,
                    fontSize = scaledSp(13.09.sp, scale),
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}

@Composable
private fun FooterRow(restCount: Int, dots: List<Color>, scale: WidgetScale) {
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text(
            text = if (restCount <= 0) "没有其他课程" else "还有${restCount}节课",
            maxLines = 1,
            style =
                TextStyle(
                    color = widgetSubtitleText,
                    fontSize = scaledSp(10.sp, scale),
                    fontWeight = FontWeight.Bold,
                ),
        )

        repeat(min(dots.size, MAX_DOTS)) { index ->
            Spacer(modifier = GlanceModifier.width(scaledDpW(DOT_GAP, scale)))

            Spacer(
                modifier =
                    GlanceModifier.size(scaledDpU(DOT_SIZE, scale))
                        .background(ColorProvider(dots[index]))
                        .cornerRadius(scaledDpU(2.dp, scale)),
            )
        }
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
private fun CourseRow(row: LargeCourseRow, scale: WidgetScale) {
    val background =
        when (row.statusType) {
            StatusType.NONE -> rowBgNeutral
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
                .cornerRadius(scaledDpU(ROW_RADIUS, scale))
                .padding(
                    horizontal = scaledDpW(ROW_PADDING_H, scale),
                    vertical = scaledDpH(ROW_PADDING_V, scale),
                ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.width(scaledDpW(TIME_COL_W, scale))) {
            Text(
                text = row.start,
                style =
                    TextStyle(
                        color = widgetTimeText,
                        fontSize = scaledSp(12.sp, scale),
                        fontWeight = FontWeight.Bold,
                    ),
            )

            Text(
                text = row.end,
                style =
                    TextStyle(
                        color = widgetTimeSubText,
                        fontSize = scaledSp(12.sp, scale),
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }

        Spacer(
            modifier =
                GlanceModifier.width(scaledDpW(BAR_W, scale))
                    .height(scaledDpH(BAR_H, scale))
                    .background(ColorProvider(row.barColor))
                    .cornerRadius(scaledDpU(2.dp, scale)),
        )

        Spacer(modifier = GlanceModifier.width(scaledDpW(BAR_TO_TITLE, scale)))

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
                style =
                    TextStyle(
                        color = widgetSubtitleText,
                        fontSize = scaledSp(12.sp, scale),
                        fontWeight = FontWeight.Bold,
                    ),
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
                        fontSize = scaledSp(12.sp, scale),
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }
    }
}
