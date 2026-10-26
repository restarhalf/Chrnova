package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val QrCode: ImageVector
    get() {
        val current = _qrCode
        if (current != null) return current

        return ImageVector.Builder(
            name = "QrCode",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 960.0f,
            viewportHeight = 960.0f,
        ).apply {

            path(
                fill = SolidColor(Color(0xFF1F1F1F)),
            ) {

                moveTo(x = 120.0f, y = 400.0f)

                lineToRelative(dx = 0.0f, dy = -240.0f)

                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -17.0f,
                    dx2 = 11.5f,
                    dy2 = -28.5f,
                )

                reflectiveQuadTo(
                    x1 = 160.0f,
                    y1 = 120.0f,
                )

                lineToRelative(dx = 240.0f, dy = 0.0f)

                quadToRelative(
                    dx1 = 17.0f,
                    dy1 = 0.0f,
                    dx2 = 28.5f,
                    dy2 = 11.5f,
                )

                reflectiveQuadTo(
                    x1 = 440.0f,
                    y1 = 160.0f,
                )

                lineToRelative(dx = 0.0f, dy = 240.0f)

                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 17.0f,
                    dx2 = -11.5f,
                    dy2 = 28.5f,
                )

                reflectiveQuadTo(
                    x1 = 400.0f,
                    y1 = 440.0f,
                )

                lineTo(x = 160.0f, y = 440.0f)

                quadToRelative(
                    dx1 = -17.0f,
                    dy1 = 0.0f,
                    dx2 = -28.5f,
                    dy2 = -11.5f,
                )

                reflectiveQuadTo(
                    x1 = 120.0f,
                    y1 = 400.0f,
                )

                moveToRelative(dx = 80.0f, dy = -40.0f)

                lineToRelative(dx = 160.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = -160.0f)

                lineTo(x = 200.0f, y = 200.0f)
                close()

                moveToRelative(dx = -80.0f, dy = 440.0f)

                lineToRelative(dx = 0.0f, dy = -240.0f)

                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -17.0f,
                    dx2 = 11.5f,
                    dy2 = -28.5f,
                )

                reflectiveQuadTo(
                    x1 = 160.0f,
                    y1 = 520.0f,
                )

                lineToRelative(dx = 240.0f, dy = 0.0f)

                quadToRelative(
                    dx1 = 17.0f,
                    dy1 = 0.0f,
                    dx2 = 28.5f,
                    dy2 = 11.5f,
                )

                reflectiveQuadTo(
                    x1 = 440.0f,
                    y1 = 560.0f,
                )

                lineToRelative(dx = 0.0f, dy = 240.0f)

                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 17.0f,
                    dx2 = -11.5f,
                    dy2 = 28.5f,
                )

                reflectiveQuadTo(
                    x1 = 400.0f,
                    y1 = 840.0f,
                )

                lineTo(x = 160.0f, y = 840.0f)

                quadToRelative(
                    dx1 = -17.0f,
                    dy1 = 0.0f,
                    dx2 = -28.5f,
                    dy2 = -11.5f,
                )

                reflectiveQuadTo(
                    x1 = 120.0f,
                    y1 = 800.0f,
                )

                moveToRelative(dx = 80.0f, dy = -40.0f)

                lineToRelative(dx = 160.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = -160.0f)

                lineTo(x = 200.0f, y = 600.0f)
                close()

                moveToRelative(dx = 320.0f, dy = -360.0f)

                lineToRelative(dx = 0.0f, dy = -240.0f)

                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -17.0f,
                    dx2 = 11.5f,
                    dy2 = -28.5f,
                )

                reflectiveQuadTo(
                    x1 = 560.0f,
                    y1 = 120.0f,
                )

                lineToRelative(dx = 240.0f, dy = 0.0f)

                quadToRelative(
                    dx1 = 17.0f,
                    dy1 = 0.0f,
                    dx2 = 28.5f,
                    dy2 = 11.5f,
                )

                reflectiveQuadTo(
                    x1 = 840.0f,
                    y1 = 160.0f,
                )

                lineToRelative(dx = 0.0f, dy = 240.0f)

                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 17.0f,
                    dx2 = -11.5f,
                    dy2 = 28.5f,
                )

                reflectiveQuadTo(
                    x1 = 800.0f,
                    y1 = 440.0f,
                )

                lineTo(x = 560.0f, y = 440.0f)

                quadToRelative(
                    dx1 = -17.0f,
                    dy1 = 0.0f,
                    dx2 = -28.5f,
                    dy2 = -11.5f,
                )

                reflectiveQuadTo(
                    x1 = 520.0f,
                    y1 = 400.0f,
                )

                moveToRelative(dx = 80.0f, dy = -40.0f)

                lineToRelative(dx = 160.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = -160.0f)

                lineTo(x = 600.0f, y = 200.0f)
                close()

                moveToRelative(dx = 160.0f, dy = 480.0f)

                lineToRelative(dx = 0.0f, dy = -80.0f)

                lineToRelative(dx = 80.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()

                moveTo(x = 520.0f, y = 600.0f)

                lineToRelative(dx = 0.0f, dy = -80.0f)

                lineToRelative(dx = 80.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()

                moveToRelative(dx = 80.0f, dy = 80.0f)

                lineToRelative(dx = 0.0f, dy = -80.0f)

                lineToRelative(dx = 80.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()

                moveToRelative(dx = -80.0f, dy = 80.0f)

                lineToRelative(dx = 0.0f, dy = -80.0f)

                lineToRelative(dx = 80.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()

                moveToRelative(dx = 80.0f, dy = 80.0f)

                lineToRelative(dx = 0.0f, dy = -80.0f)

                lineToRelative(dx = 80.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()

                moveToRelative(dx = 80.0f, dy = -80.0f)

                lineToRelative(dx = 0.0f, dy = -80.0f)

                lineToRelative(dx = 80.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()

                moveToRelative(dx = 0.0f, dy = -160.0f)

                lineToRelative(dx = 0.0f, dy = -80.0f)

                lineToRelative(dx = 80.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()

                moveToRelative(dx = 80.0f, dy = 80.0f)

                lineToRelative(dx = 0.0f, dy = -80.0f)

                lineToRelative(dx = 80.0f, dy = 0.0f)

                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()
            }
        }.build().also { _qrCode = it }
    }

@Suppress("ObjectPropertyName")
private var _qrCode: ImageVector? = null
