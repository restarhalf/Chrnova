package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Add: ImageVector
    get() =
        ImageVector.Builder(
            name = "Rounded.Add",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .apply {
                path(fill = SolidColor(Color(0xFF1F1F1F)), pathFillType = PathFillType.NonZero) {
                    moveTo(11f, 13f)
                    lineTo(6f, 13f)
                    curveTo(5.7167f, 13f, 5.4792f, 12.9042f, 5.2875f, 12.7125f)
                    curveTo(5.0958f, 12.5208f, 5f, 12.2833f, 5f, 12f)
                    curveTo(5f, 11.7167f, 5.0958f, 11.4792f, 5.2875f, 11.2875f)
                    curveTo(5.4792f, 11.0958f, 5.7167f, 11f, 6f, 11f)
                    lineTo(11f, 11f)
                    lineTo(11f, 6f)
                    curveTo(11f, 5.7167f, 11.0958f, 5.4792f, 11.2875f, 5.2875f)
                    curveTo(11.4792f, 5.0958f, 11.7167f, 5f, 12f, 5f)
                    curveTo(12.2833f, 5f, 12.5208f, 5.0958f, 12.7125f, 5.2875f)
                    curveTo(12.9042f, 5.4792f, 13f, 5.7167f, 13f, 6f)
                    lineTo(13f, 11f)
                    lineTo(18f, 11f)
                    curveTo(18.2833f, 11f, 18.5208f, 11.0958f, 18.7125f, 11.2875f)
                    curveTo(18.9042f, 11.4792f, 19f, 11.7167f, 19f, 12f)
                    curveTo(19f, 12.2833f, 18.9042f, 12.5208f, 18.7125f, 12.7125f)
                    curveTo(18.5208f, 12.9042f, 18.2833f, 13f, 18f, 13f)
                    lineTo(13f, 13f)
                    lineTo(13f, 18f)
                    curveTo(13f, 18.2833f, 12.9042f, 18.5208f, 12.7125f, 18.7125f)
                    curveTo(12.5208f, 18.9042f, 12.2833f, 19f, 12f, 19f)
                    curveTo(11.7167f, 19f, 11.4792f, 18.9042f, 11.2875f, 18.7125f)
                    curveTo(11.0958f, 18.5208f, 11f, 18.2833f, 11f, 18f)
                    lineTo(11f, 13f)
                    close()
                }
            }
            .build()
