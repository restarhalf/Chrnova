package restarhalf.stellar.schedule.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chrnova.composeapp.generated.resources.Res
import chrnova.composeapp.generated.resources.share_qr
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.resources.painterResource
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.ui.components.AvatarImage
import restarhalf.stellar.schedule.ui.components.screen.schedule.CourseCard
import restarhalf.stellar.schedule.ui.components.screen.schedule.WeekHeaderRow
import restarhalf.stellar.schedule.ui.icons.AppIcon
import restarhalf.stellar.schedule.ui.image.toAsyncImageModel
import restarhalf.stellar.schedule.ui.mapper.DayRenderData
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 分享卡固定宽度（dp）。导出按此宽度离屏合成，不跟随窗口/面板宽度。 */
val ShareCardWidth: Dp = 380.dp

private val ShareLabelWidth = 36.dp
private val ShareFooterHeight = 80.dp

/** 分享课表格子几何：构建 CourseCardModel 时必须使用同一套 Y/高度。 */
object ShareTimetableGeometry {
    val rowHeight = 60.dp
    val rowGap = 1.dp
    val restHeight = 28.dp
    val cellInset = 0.5.dp

    fun yForSection(section: Int): Dp {
        val base = (rowHeight + rowGap) * (section - 1)
        val rest = (if (section > 4) restHeight else 0.dp) + (if (section > 8) restHeight else 0.dp)
        return base + rest
    }

    fun heightForSections(sectionCount: Int): Dp {
        if (sectionCount <= 0) return 0.dp
        return rowHeight * sectionCount + rowGap * (sectionCount - 1)
    }

    val gridHeight: Dp get() = rowHeight * 12 + rowGap * 11 + restHeight * 2
}

/** 估算整卡高度，便于预览缩放与离屏画布尺寸。 */
fun shareCardHeight(showSharer: Boolean): Dp {
    val top = if (showSharer) 14.dp + 48.dp + 10.dp + 16.dp + 14.dp else 16.dp + 22.dp + 16.dp
    val gridBlock = 8.dp + 50.dp + 6.dp + ShareTimetableGeometry.gridHeight + 8.dp
    return top + gridBlock + ShareFooterHeight + 4.dp
}

/**
 * 课表分享卡片。
 *
 * 与应用一致：整卡铺背景（主题色或自定义壁纸），课表内容直接浮在背景上，
 * 不额外叠大面积色块；有昵称时顶部展示分享人，否则整块隐藏。
 */
@Composable
fun ShareTimetableCard(
    nickname: String?,
    avatarUri: String?,
    weekLabel: String,
    weekHeaderUi: ScheduleViewModel.WeekHeaderUi,
    dayRenderData: ImmutableMap<Int, DayRenderData>,
    timetable: ImmutableList<TimetableSlot>,
    modifier: Modifier = Modifier,
    backgroundImageUri: String? = null,
    backgroundAlpha: Float = 1f,
    backgroundBlur: Float = 0f,
) {
    val showSharer = !nickname.isNullOrBlank()
    val rowHeight = ShareTimetableGeometry.rowHeight
    val rowGap = ShareTimetableGeometry.rowGap
    val restHeight = ShareTimetableGeometry.restHeight
    val gridHeight = ShareTimetableGeometry.gridHeight
    val cardShape = RoundedCornerShape(22.dp)
    val colors = MiuixTheme.colorScheme
    val textPrimary = colors.onBackground
    val textSecondary = colors.onSurfaceVariantSummary
    val textHint = colors.onSurfaceVariantActions

    Box(
        modifier = modifier
            .requiredWidth(ShareCardWidth)
            .wrapContentHeight()
            .clip(cardShape)
            .border(2.dp, colors.surfaceContainerHigh, cardShape)
    ) {
        // 与 AppContent 相同的背景铺法：surface 底 + 自定义壁纸
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(colors.surface)
        )
        if (backgroundImageUri != null) {
            AsyncImage(
                model = toAsyncImageModel(backgroundImageUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(25.dp * backgroundBlur)
                    .alpha(backgroundAlpha),
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // 顶部信息：直接落在背景上
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = if (showSharer) 14.dp else 16.dp)
            ) {
                if (showSharer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            if (avatarUri != null) {
                                AvatarImage(
                                    avatarUri = avatarUri,
                                    contentDescription = "分享人头像",
                                    size = 48.dp,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.28f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = nickname.first().toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .border(2.dp, colors.background.copy(alpha = 0.7f), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = nickname,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = weekLabel,
                    fontSize = if (showSharer) 13.sp else 20.sp,
                    fontWeight = if (showSharer) FontWeight.Medium else FontWeight.Bold,
                    color = if (showSharer) textSecondary else textPrimary,
                )
            }

            // 课表：与 ScheduleScreen 一样直接铺在背景上
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                WeekHeaderRow(
                    ui = weekHeaderUi,
                    primary = colors.primary,
                    textSecondary = textSecondary,
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    ShareSectionLabels(
                        timetable = timetable,
                        rowHeight = rowHeight,
                        rowGap = rowGap,
                        restHeight = restHeight,
                        textSecondary = textSecondary,
                        textHint = textHint,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(gridHeight)
                    ) {
                        ShareRestBars(
                            restHeight = restHeight,
                            rowHeight = rowHeight,
                            rowGap = rowGap,
                            restBarColor = colors.surfaceContainerHigh.copy(alpha = 0.55f),
                            textSecondary = textSecondary,
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            (1..7).forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(gridHeight)
                                ) {
                                    dayRenderData[day]?.items?.forEach { item ->
                                        CourseCard(model = item.model, onClick = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 底栏：轻透明，类似应用底栏浮在背景上
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ShareFooterHeight)
                    .background(colors.surfaceContainerHigh.copy(alpha = 0.72f))
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcon,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Chrnova",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                        )
                        Text(text = "校园课程表", fontSize = 11.sp, color = textSecondary)
                    }
                }
                Image(
                    painter = painterResource(Res.drawable.share_qr),
                    contentDescription = "官网二维码",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.5.dp, colors.surfaceContainerHighest, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun ShareRestBars(
    restHeight: Dp,
    rowHeight: Dp,
    rowGap: Dp,
    restBarColor: Color,
    textSecondary: Color,
) {
    Box(modifier = Modifier.fillMaxWidth().height(ShareTimetableGeometry.gridHeight)) {
        listOf(4 to "午休", 8 to "晚休").forEach { (afterSection, label) ->
            val top = (rowHeight + rowGap) * afterSection +
                (if (afterSection > 4) restHeight else 0.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = top)
                    .height(restHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(restBarColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label, fontSize = 9.sp, color = textSecondary)
            }
        }
    }
}

@Composable
private fun ShareSectionLabels(
    timetable: ImmutableList<TimetableSlot>,
    rowHeight: Dp,
    rowGap: Dp,
    restHeight: Dp,
    textSecondary: Color,
    textHint: Color,
) {
    Column(modifier = Modifier.width(ShareLabelWidth)) {
        (1..12).forEach { section ->
            val slot = timetable.getOrNull(section - 1)
            Box(
                modifier = Modifier.height(rowHeight),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = section.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(text = slot?.start ?: "", fontSize = 8.sp, color = textHint)
                    Text(text = slot?.end ?: "", fontSize = 8.sp, color = textHint)
                }
            }
            if (section == 4 || section == 8) {
                Spacer(modifier = Modifier.height(restHeight))
            }
            if (section != 12) {
                Spacer(modifier = Modifier.height(rowGap))
            }
        }
    }
}

/**
 * 离屏捕获容器：按固定像素尺寸记入 [layer]，供导出 PNG。
 *
 * Android/iOS 无 Desktop 的 ImageComposeScene；CMP 下用 GraphicsLayer 等价离屏合成。
 */
@Composable
fun ShareCaptureBox(
    layer: GraphicsLayer,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
        }
    ) {
        content()
    }
}

@Composable
fun rememberShareGraphicsLayer(): GraphicsLayer = rememberGraphicsLayer()
