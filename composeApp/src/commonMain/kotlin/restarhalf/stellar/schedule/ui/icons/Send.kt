package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Send: ImageVector
    get() {
        val current = _send
        if (current != null) return current

        return ImageVector.Builder(
            name = "Rounded.Send",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 960.0f,
            viewportHeight = 960.0f,
        ).apply {
            // M190.66 -211.09 q-18.12 7.24 -34.39 -3.08 T140 -244.23 v-168.85 L416.92 -480 140 -546.92 v-168.85 q0 -19.74 16.27 -30.06 t34.39 -3.08 l558.26 235.37 q22.31 9.99 22.31 33.61 t-22.31 33.47z
            path(
                fill = SolidColor(Color(0xFF1F1F1F)),
            ) {
                // M 190.66 748.91
                moveTo(x = 190.66f, y = 748.91f)
                // q -18.12 7.24 -34.39 -3.08
                quadToRelative(
                    dx1 = -18.12f,
                    dy1 = 7.24f,
                    dx2 = -34.39f,
                    dy2 = -3.08f,
                )
                // T 140 715.77
                reflectiveQuadTo(
                    x1 = 140.0f,
                    y1 = 715.77f,
                )
                // l 0 -168.85
                lineToRelative(dx = 0.0f, dy = -168.85f)
                // L 416.92 480
                lineTo(x = 416.92f, y = 480.0f)
                // L 140 413.08
                lineTo(x = 140.0f, y = 413.08f)
                // l 0 -168.85
                lineToRelative(dx = 0.0f, dy = -168.85f)
                // q 0 -19.74 16.27 -30.06
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -19.74f,
                    dx2 = 16.27f,
                    dy2 = -30.06f,
                )
                // t 34.39 -3.08
                reflectiveQuadToRelative(
                    dx1 = 34.39f,
                    dy1 = -3.08f,
                )
                // l 558.26 235.37
                lineToRelative(dx = 558.26f, dy = 235.37f)
                // q 22.31 9.99 22.31 33.61
                quadToRelative(
                    dx1 = 22.31f,
                    dy1 = 9.99f,
                    dx2 = 22.31f,
                    dy2 = 33.61f,
                )
                // t -22.31 33.47z
                reflectiveQuadToRelative(
                    dx1 = -22.31f,
                    dy1 = 33.47f,
                )
                close()
            }
        }.build().also { _send = it }
    }


@Suppress("ObjectPropertyName")
private var _send: ImageVector? = null
