package restarhalf.stellar.schedule.ui.screens.announcement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 广告位配置（UI 层），字段与后端 /ad 下发及 domain.model.AdConfig 一一对应。
 */
data class AdBannerConfig(
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val announcementId: String? = null,
)

/**
 * 公告列表页顶部的广告位横幅。
 *
 * 视觉：圆角横幅 + 右上角"广告"字样（文字标签，无图标，对齐极简规范）。
 * 点击：有单链接直接跳转；有公告 id 则复用公告详情；皆无则无操作。
 * [config] 为 null 时整条不渲染（后端未下发广告时隐藏）。
 */
@Composable
fun AdBanner(
    config: AdBannerConfig?,
    onOpenUrl: (String) -> Unit,
    onOpenAnnouncement: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (config == null) return
    val colors = MiuixTheme.colorScheme

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    when {
                        config.targetUrl != null -> onOpenUrl(config.targetUrl)
                        config.announcementId != null -> onOpenAnnouncement(config.announcementId)
                    }
                },
    ) {
        if (config.imageUrl != null) {
            SubcomposeAsyncImage(
                model = config.imageUrl,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                contentScale = ContentScale.Crop,
                loading = { AdSurface() },
                error = { AdSurface() },
            )
        } else {
            // 无横幅图时（仅配置了跳转目标）：渲染可点击的占位面，由右上角"广告"标签提示
            AdSurface()
        }

        // 右上角"广告"字样
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.surface.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = "广告",
                style = MiuixTheme.textStyles.footnote1,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
}

/** 横幅底版：浅色表面占位（加载中 / 加载失败 / 无图时展示），不含任何文案 */
@Composable
private fun AdSurface() {
    val colors = MiuixTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(colors.surfaceContainerHigh),
    )
}
