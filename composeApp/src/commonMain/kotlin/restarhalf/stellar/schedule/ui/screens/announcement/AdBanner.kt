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

data class AdBannerConfig(
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val announcementId: String? = null,
)

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

            AdSurface()
        }


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
