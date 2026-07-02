package restarhalf.stellar.schedule.ui.components.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DetailHeader(
    appIcon: ImageBitmap?,
    appName: String,
    version: String?,
    onIconTap: () -> Unit = {},
) {
    var tapCount by remember { mutableIntStateOf(0) }
    val colors = MiuixTheme.colorScheme

    Box(
        modifier =
            Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    tapCount++
                    if (tapCount >= 5) {
                        tapCount = 0
                        onIconTap()
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = appName, style = MiuixTheme.textStyles.title2)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "版本: $version",
        color = colors.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
    )
}
