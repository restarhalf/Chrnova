package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Close: ImageVector
    get() =
        ImageVector.Builder(
            name = "Rounded.Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
            .apply {
                path(
                    fill = SolidColor(Color(0xFF1F1F1F)),
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(18.3f, 5.7f)
                    curveTo(18.7f, 6.1f, 18.7f, 6.7f, 18.3f, 7.1f)
                    lineTo(13.4f, 12f)
                    lineTo(18.3f, 16.9f)
                    curveTo(18.7f, 17.3f, 18.7f, 17.9f, 18.3f, 18.3f)
                    curveTo(17.9f, 18.7f, 17.3f, 18.7f, 16.9f, 18.3f)
                    lineTo(12f, 13.4f)
                    lineTo(7.1f, 18.3f)
                    curveTo(6.7f, 18.7f, 6.1f, 18.7f, 5.7f, 18.3f)
                    curveTo(5.3f, 17.9f, 5.3f, 17.3f, 5.7f, 16.9f)
                    lineTo(10.6f, 12f)
                    lineTo(5.7f, 7.1f)
                    curveTo(5.3f, 6.7f, 5.3f, 6.1f, 5.7f, 5.7f)
                    curveTo(6.1f, 5.3f, 6.7f, 5.3f, 7.1f, 5.7f)
                    lineTo(12f, 10.6f)
                    lineTo(16.9f, 5.7f)
                    curveTo(17.3f, 5.3f, 17.9f, 5.3f, 18.3f, 5.7f)
                    close()
                }
            }
            .build()
