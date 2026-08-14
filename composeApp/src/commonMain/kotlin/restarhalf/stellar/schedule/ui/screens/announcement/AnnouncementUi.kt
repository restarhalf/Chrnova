package restarhalf.stellar.schedule.ui.screens.announcement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 公告发布时间（Unix 秒）格式化为 yyyy/MM/dd */
fun formatAnnouncementDate(createdAtSec: Long): String {
    if (createdAtSec <= 0) return ""
    return runCatching {
        val date =
            kotlin.time.Instant
                .fromEpochMilliseconds(createdAtSec * 1000L)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        val month = (date.month.ordinal + 1).toString().padStart(2, '0')
        val day = date.day.toString().padStart(2, '0')
        "${date.year}/$month/$day"
    }.getOrDefault("")
}

/**
 * 公告小徽标（置顶/重要）。
 *
 * @param text 徽标文字
 * @param color 徽标文字与底色（低透明度）颜色
 */
@Composable
fun AnnouncementBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

/**
 * 公告标题行：置顶/重要徽标 + 标题。
 *
 * @param title 公告标题
 * @param pinned 是否置顶
 * @param important 是否重要
 */
@Composable
fun AnnouncementTitleRow(
    title: String,
    pinned: Boolean,
    important: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pinned) {
            AnnouncementBadge(
                text = "置顶",
                color = colors.primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (important) {
            AnnouncementBadge(
                text = "重要",
                color = colors.error,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/**
 * 未读小圆点（无数字），用于列表页标记单条公告未读。
 */
@Composable
fun UnreadBadge(modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier =
            modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(colors.error),
    )
}

