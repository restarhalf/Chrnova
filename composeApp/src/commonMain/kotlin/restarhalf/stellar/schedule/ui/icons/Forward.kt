package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Forward: ImageVector
    get() {
        val current = _forward
        if (current != null) return current

        return ImageVector.Builder(
            name = "Rounded.Forward",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 960.0f,
            viewportHeight = 960.0f,
        ).apply {
            // M579 -480 285 -774 q-15 -15 -14.5 -35.5 T286 -845 t35.5 -15 35.5 15 l307 308 q12 12 18 27 t6 30 -6 30 -18 27 L356 -115 q-15 15 -35 14.5 T286 -116 t-15 -35.5 15 -35.5z
            path(
                fill = SolidColor(Color(0xFF1F1F1F)),
            ) {
                // M 579 480
                moveTo(x = 579.0f, y = 480.0f)
                // L 285 186
                lineTo(x = 285.0f, y = 186.0f)
                // q -15 -15 -14.5 -35.5
                quadToRelative(
                    dx1 = -15.0f,
                    dy1 = -15.0f,
                    dx2 = -14.5f,
                    dy2 = -35.5f,
                )
                // T 286 115
                reflectiveQuadTo(
                    x1 = 286.0f,
                    y1 = 115.0f,
                )
                // t 35.5 -15
                reflectiveQuadToRelative(
                    dx1 = 35.5f,
                    dy1 = -15.0f,
                )
                // t 35.5 15
                reflectiveQuadToRelative(
                    dx1 = 35.5f,
                    dy1 = 15.0f,
                )
                // l 307 308
                lineToRelative(dx = 307.0f, dy = 308.0f)
                // q 12 12 18 27
                quadToRelative(
                    dx1 = 12.0f,
                    dy1 = 12.0f,
                    dx2 = 18.0f,
                    dy2 = 27.0f,
                )
                // t 6 30
                reflectiveQuadToRelative(
                    dx1 = 6.0f,
                    dy1 = 30.0f,
                )
                // t -6 30
                reflectiveQuadToRelative(
                    dx1 = -6.0f,
                    dy1 = 30.0f,
                )
                // t -18 27
                reflectiveQuadToRelative(
                    dx1 = -18.0f,
                    dy1 = 27.0f,
                )
                // L 356 845
                lineTo(x = 356.0f, y = 845.0f)
                // q -15 15 -35 14.5
                quadToRelative(
                    dx1 = -15.0f,
                    dy1 = 15.0f,
                    dx2 = -35.0f,
                    dy2 = 14.5f,
                )
                // T 286 844
                reflectiveQuadTo(
                    x1 = 286.0f,
                    y1 = 844.0f,
                )
                // t -15 -35.5
                reflectiveQuadToRelative(
                    dx1 = -15.0f,
                    dy1 = -35.5f,
                )
                // t 15 -35.5z
                reflectiveQuadToRelative(
                    dx1 = 15.0f,
                    dy1 = -35.5f,
                )
                close()
            }
        }.build().also { _forward = it }
    }

@Suppress("ObjectPropertyName")
private var _forward: ImageVector? = null
