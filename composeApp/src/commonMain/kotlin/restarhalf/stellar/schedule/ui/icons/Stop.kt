package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Stop: ImageVector
    get() {
        val current = _stop
        if (current != null) return current

        return ImageVector.Builder(
            name = "Rounded.Stop",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 960.0f,
            viewportHeight = 960.0f,
        ).apply {
            // M260 -332.31 v-295.38 q0 -29.83 21.24 -51.07 T332.31 -700 h295.38 q29.83 0 51.07 21.24 T700 -627.69 v295.38 q0 29.83 -21.24 51.07 T627.69 -260 H332.31 q-29.83 0 -51.07 -21.24 T260 -332.31
            path(
                fill = SolidColor(Color(0xFF1F1F1F)),
            ) {
                // M 260 627.69
                moveTo(x = 260.0f, y = 627.69f)
                // l 0 -295.38
                lineToRelative(dx = 0.0f, dy = -295.38f)
                // q 0 -29.83 21.24 -51.07
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -29.83f,
                    dx2 = 21.24f,
                    dy2 = -51.07f,
                )
                // T 332.31 260
                reflectiveQuadTo(
                    x1 = 332.31f,
                    y1 = 260.0f,
                )
                // l 295.38 0
                lineToRelative(dx = 295.38f, dy = 0.0f)
                // q 29.83 0 51.07 21.24
                quadToRelative(
                    dx1 = 29.83f,
                    dy1 = 0.0f,
                    dx2 = 51.07f,
                    dy2 = 21.24f,
                )
                // T 700 332.31
                reflectiveQuadTo(
                    x1 = 700.0f,
                    y1 = 332.31f,
                )
                // l 0 295.38
                lineToRelative(dx = 0.0f, dy = 295.38f)
                // q 0 29.83 -21.24 51.07
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 29.83f,
                    dx2 = -21.24f,
                    dy2 = 51.07f,
                )
                // T 627.69 700
                reflectiveQuadTo(
                    x1 = 627.69f,
                    y1 = 700.0f,
                )
                // L 332.31 700
                lineTo(x = 332.31f, y = 700.0f)
                // q -29.83 0 -51.07 -21.24
                quadToRelative(
                    dx1 = -29.83f,
                    dy1 = 0.0f,
                    dx2 = -51.07f,
                    dy2 = -21.24f,
                )
                // T 260 627.69
                reflectiveQuadTo(
                    x1 = 260.0f,
                    y1 = 627.69f,
                )
            }
        }.build().also { _stop = it }
    }


@Suppress("ObjectPropertyName")
private var _stop: ImageVector? = null
