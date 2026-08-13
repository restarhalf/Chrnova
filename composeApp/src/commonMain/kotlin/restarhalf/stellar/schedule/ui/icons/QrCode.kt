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
            // M120 -560 v-240 q0 -17 11.5 -28.5 T160 -840 h240 q17 0 28.5 11.5 T440 -800 v240 q0 17 -11.5 28.5 T400 -520 H160 q-17 0 -28.5 -11.5 T120 -560 m80 -40 h160 v-160 H200z m-80 440 v-240 q0 -17 11.5 -28.5 T160 -440 h240 q17 0 28.5 11.5 T440 -400 v240 q0 17 -11.5 28.5 T400 -120 H160 q-17 0 -28.5 -11.5 T120 -160 m80 -40 h160 v-160 H200z m320 -360 v-240 q0 -17 11.5 -28.5 T560 -840 h240 q17 0 28.5 11.5 T840 -800 v240 q0 17 -11.5 28.5 T800 -520 H560 q-17 0 -28.5 -11.5 T520 -560 m80 -40 h160 v-160 H600z m160 480 v-80 h80 v80z M520 -360 v-80 h80 v80z m80 80 v-80 h80 v80z m-80 80 v-80 h80 v80z m80 80 v-80 h80 v80z m80 -80 v-80 h80 v80z m0 -160 v-80 h80 v80z m80 80 v-80 h80 v80z
            path(
                fill = SolidColor(Color(0xFF1F1F1F)),
            ) {
                // M 120 400
                moveTo(x = 120.0f, y = 400.0f)
                // l 0 -240
                lineToRelative(dx = 0.0f, dy = -240.0f)
                // q 0 -17 11.5 -28.5
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -17.0f,
                    dx2 = 11.5f,
                    dy2 = -28.5f,
                )
                // T 160 120
                reflectiveQuadTo(
                    x1 = 160.0f,
                    y1 = 120.0f,
                )
                // l 240 0
                lineToRelative(dx = 240.0f, dy = 0.0f)
                // q 17 0 28.5 11.5
                quadToRelative(
                    dx1 = 17.0f,
                    dy1 = 0.0f,
                    dx2 = 28.5f,
                    dy2 = 11.5f,
                )
                // T 440 160
                reflectiveQuadTo(
                    x1 = 440.0f,
                    y1 = 160.0f,
                )
                // l 0 240
                lineToRelative(dx = 0.0f, dy = 240.0f)
                // q 0 17 -11.5 28.5
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 17.0f,
                    dx2 = -11.5f,
                    dy2 = 28.5f,
                )
                // T 400 440
                reflectiveQuadTo(
                    x1 = 400.0f,
                    y1 = 440.0f,
                )
                // L 160 440
                lineTo(x = 160.0f, y = 440.0f)
                // q -17 0 -28.5 -11.5
                quadToRelative(
                    dx1 = -17.0f,
                    dy1 = 0.0f,
                    dx2 = -28.5f,
                    dy2 = -11.5f,
                )
                // T 120 400
                reflectiveQuadTo(
                    x1 = 120.0f,
                    y1 = 400.0f,
                )
                // m 80 -40
                moveToRelative(dx = 80.0f, dy = -40.0f)
                // l 160 0
                lineToRelative(dx = 160.0f, dy = 0.0f)
                // l 0 -160
                lineToRelative(dx = 0.0f, dy = -160.0f)
                // L 200 200z
                lineTo(x = 200.0f, y = 200.0f)
                close()
                // m -80 440
                moveToRelative(dx = -80.0f, dy = 440.0f)
                // l 0 -240
                lineToRelative(dx = 0.0f, dy = -240.0f)
                // q 0 -17 11.5 -28.5
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -17.0f,
                    dx2 = 11.5f,
                    dy2 = -28.5f,
                )
                // T 160 520
                reflectiveQuadTo(
                    x1 = 160.0f,
                    y1 = 520.0f,
                )
                // l 240 0
                lineToRelative(dx = 240.0f, dy = 0.0f)
                // q 17 0 28.5 11.5
                quadToRelative(
                    dx1 = 17.0f,
                    dy1 = 0.0f,
                    dx2 = 28.5f,
                    dy2 = 11.5f,
                )
                // T 440 560
                reflectiveQuadTo(
                    x1 = 440.0f,
                    y1 = 560.0f,
                )
                // l 0 240
                lineToRelative(dx = 0.0f, dy = 240.0f)
                // q 0 17 -11.5 28.5
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 17.0f,
                    dx2 = -11.5f,
                    dy2 = 28.5f,
                )
                // T 400 840
                reflectiveQuadTo(
                    x1 = 400.0f,
                    y1 = 840.0f,
                )
                // L 160 840
                lineTo(x = 160.0f, y = 840.0f)
                // q -17 0 -28.5 -11.5
                quadToRelative(
                    dx1 = -17.0f,
                    dy1 = 0.0f,
                    dx2 = -28.5f,
                    dy2 = -11.5f,
                )
                // T 120 800
                reflectiveQuadTo(
                    x1 = 120.0f,
                    y1 = 800.0f,
                )
                // m 80 -40
                moveToRelative(dx = 80.0f, dy = -40.0f)
                // l 160 0
                lineToRelative(dx = 160.0f, dy = 0.0f)
                // l 0 -160
                lineToRelative(dx = 0.0f, dy = -160.0f)
                // L 200 600z
                lineTo(x = 200.0f, y = 600.0f)
                close()
                // m 320 -360
                moveToRelative(dx = 320.0f, dy = -360.0f)
                // l 0 -240
                lineToRelative(dx = 0.0f, dy = -240.0f)
                // q 0 -17 11.5 -28.5
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -17.0f,
                    dx2 = 11.5f,
                    dy2 = -28.5f,
                )
                // T 560 120
                reflectiveQuadTo(
                    x1 = 560.0f,
                    y1 = 120.0f,
                )
                // l 240 0
                lineToRelative(dx = 240.0f, dy = 0.0f)
                // q 17 0 28.5 11.5
                quadToRelative(
                    dx1 = 17.0f,
                    dy1 = 0.0f,
                    dx2 = 28.5f,
                    dy2 = 11.5f,
                )
                // T 840 160
                reflectiveQuadTo(
                    x1 = 840.0f,
                    y1 = 160.0f,
                )
                // l 0 240
                lineToRelative(dx = 0.0f, dy = 240.0f)
                // q 0 17 -11.5 28.5
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 17.0f,
                    dx2 = -11.5f,
                    dy2 = 28.5f,
                )
                // T 800 440
                reflectiveQuadTo(
                    x1 = 800.0f,
                    y1 = 440.0f,
                )
                // L 560 440
                lineTo(x = 560.0f, y = 440.0f)
                // q -17 0 -28.5 -11.5
                quadToRelative(
                    dx1 = -17.0f,
                    dy1 = 0.0f,
                    dx2 = -28.5f,
                    dy2 = -11.5f,
                )
                // T 520 400
                reflectiveQuadTo(
                    x1 = 520.0f,
                    y1 = 400.0f,
                )
                // m 80 -40
                moveToRelative(dx = 80.0f, dy = -40.0f)
                // l 160 0
                lineToRelative(dx = 160.0f, dy = 0.0f)
                // l 0 -160
                lineToRelative(dx = 0.0f, dy = -160.0f)
                // L 600 200z
                lineTo(x = 600.0f, y = 200.0f)
                close()
                // m 160 480
                moveToRelative(dx = 160.0f, dy = 480.0f)
                // l 0 -80
                lineToRelative(dx = 0.0f, dy = -80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 0 80z
                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()
                // M 520 600
                moveTo(x = 520.0f, y = 600.0f)
                // l 0 -80
                lineToRelative(dx = 0.0f, dy = -80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 0 80z
                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()
                // m 80 80
                moveToRelative(dx = 80.0f, dy = 80.0f)
                // l 0 -80
                lineToRelative(dx = 0.0f, dy = -80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 0 80z
                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()
                // m -80 80
                moveToRelative(dx = -80.0f, dy = 80.0f)
                // l 0 -80
                lineToRelative(dx = 0.0f, dy = -80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 0 80z
                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()
                // m 80 80
                moveToRelative(dx = 80.0f, dy = 80.0f)
                // l 0 -80
                lineToRelative(dx = 0.0f, dy = -80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 0 80z
                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()
                // m 80 -80
                moveToRelative(dx = 80.0f, dy = -80.0f)
                // l 0 -80
                lineToRelative(dx = 0.0f, dy = -80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 0 80z
                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()
                // m 0 -160
                moveToRelative(dx = 0.0f, dy = -160.0f)
                // l 0 -80
                lineToRelative(dx = 0.0f, dy = -80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 0 80z
                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()
                // m 80 80
                moveToRelative(dx = 80.0f, dy = 80.0f)
                // l 0 -80
                lineToRelative(dx = 0.0f, dy = -80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 0 80z
                lineToRelative(dx = 0.0f, dy = 80.0f)
                close()
            }
        }.build().also { _qrCode = it }
    }



@Suppress("ObjectPropertyName")
private var _qrCode: ImageVector? = null
