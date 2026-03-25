package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Check: ImageVector
    get() =
        ImageVector.Builder(
            name = "Rounded.Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .apply {
                path(fill = SolidColor(Color(0xFF1F1F1F)), pathFillType = PathFillType.NonZero) {
                    moveTo(9.55f, 15.15f)
                    lineTo(18.025f, 6.675f)
                    curveTo(18.225f, 6.475f, 18.4583f, 6.375f, 18.725f, 6.375f)
                    curveTo(18.9917f, 6.375f, 19.225f, 6.475f, 19.425f, 6.675f)
                    curveTo(19.625f, 6.875f, 19.725f, 7.1125f, 19.725f, 7.3875f)
                    curveTo(19.725f, 7.6625f, 19.625f, 7.9f, 19.425f, 8.1f)
                    lineTo(10.25f, 17.3f)
                    curveTo(10.05f, 17.5f, 9.8167f, 17.6f, 9.55f, 17.6f)
                    curveTo(9.2833f, 17.6f, 9.05f, 17.5f, 8.85f, 17.3f)
                    lineTo(4.55f, 13f)
                    curveTo(4.35f, 12.8f, 4.2542f, 12.5625f, 4.2625f, 12.2875f)
                    curveTo(4.2708f, 12.0125f, 4.375f, 11.775f, 4.575f, 11.575f)
                    curveTo(4.775f, 11.375f, 5.0125f, 11.275f, 5.2875f, 11.275f)
                    curveTo(5.5625f, 11.275f, 5.8f, 11.375f, 6f, 11.575f)
                    lineTo(9.55f, 15.15f)
                    close()
                }
            }
            .build()
