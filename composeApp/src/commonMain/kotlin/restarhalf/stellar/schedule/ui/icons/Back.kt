package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Back: ImageVector
    get() =
        ImageVector.Builder(
            name = "Rounded.Back",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .apply {
                path(fill = SolidColor(Color(0xFF1F1F1F)), pathFillType = PathFillType.NonZero) {
                    moveTo(9.55f, 12f)
                    lineTo(16.9f, 19.35f)
                    curveTo(17.15f, 19.6f, 17.2708f, 19.8917f, 17.2625f, 20.225f)
                    curveTo(17.2542f, 20.5583f, 17.125f, 20.85f, 16.875f, 21.1f)
                    curveTo(16.625f, 21.35f, 16.3333f, 21.475f, 16f, 21.475f)
                    curveTo(15.6667f, 21.475f, 15.375f, 21.35f, 15.125f, 21.1f)
                    lineTo(7.425f, 13.425f)
                    curveTo(7.225f, 13.225f, 7.075f, 13f, 6.975f, 12.75f)
                    curveTo(6.875f, 12.5f, 6.825f, 12.25f, 6.825f, 12f)
                    curveTo(6.825f, 11.75f, 6.875f, 11.5f, 6.975f, 11.25f)
                    curveTo(7.075f, 11f, 7.225f, 10.775f, 7.425f, 10.575f)
                    lineTo(15.125f, 2.875f)
                    curveTo(15.375f, 2.625f, 15.6708f, 2.5042f, 16.0125f, 2.5125f)
                    curveTo(16.3542f, 2.5208f, 16.65f, 2.65f, 16.9f, 2.9f)
                    curveTo(17.15f, 3.15f, 17.275f, 3.4417f, 17.275f, 3.775f)
                    curveTo(17.275f, 4.1083f, 17.15f, 4.4f, 16.9f, 4.65f)
                    lineTo(9.55f, 12f)
                    close()
                }
            }
            .build()
