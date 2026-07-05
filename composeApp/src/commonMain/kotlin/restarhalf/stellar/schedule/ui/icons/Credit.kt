package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Credit: ImageVector
    get() {
        val current = _credit
        if (current != null) return current

        return ImageVector.Builder(
            name = "Rounded.Credit",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 960.0f,
            viewportHeight = 960.0f,
        ).apply {
            // M237 -120 q-23 0 -44.5 -16 T164 -175 q-25 -84 -41 -145.5 t-25.5 -108 T84 -511 t-4 -69 q0 -92 64 -156 t156 -64 h200 q27 -36 68.5 -58 t91.5 -22 q25 0 42.5 17.5 T720 -820 q0 6 -1.5 12 t-3.5 11 q-4 11 -7.5 22 t-5.5 24 l91 91 h47 q17 0 28.5 11.5 T880 -620 v210 q0 13 -7.5 23 T852 -372 l-85 28 -50 167 q-8 26 -29 41.5 T640 -120 h-80 q-33 0 -56.5 -23.5 T480 -200 h-80 q0 33 -23.5 56.5 T320 -120z m3 -80 h80 v-80 h240 v80 h80 l62 -206 98 -33 v-141 h-40 L620 -720 q0 -20 2.5 -39 t7.5 -37 q-29 8 -51 27.5 T547 -720 H300 q-58 0 -99 41 t-41 99 q0 41 21 140.5 T240 -200 m428.5 -331.5 Q680 -543 680 -560 t-11.5 -28.5 T640 -600 t-28.5 11.5 T600 -560 t11.5 28.5 T640 -520 t28.5 -11.5 M480 -600 q17 0 28.5 -11.5 T520 -640 t-11.5 -28.5 T480 -680 H360 q-17 0 -28.5 11.5 T320 -640 t11.5 28.5 T360 -600z m0 102
            path(
                fill = SolidColor(Color(0xFF1F1F1F)),
            ) {
                // M 237 840
                moveTo(x = 237.0f, y = 840.0f)
                // q -23 0 -44.5 -16
                quadToRelative(
                    dx1 = -23.0f,
                    dy1 = 0.0f,
                    dx2 = -44.5f,
                    dy2 = -16.0f,
                )
                // T 164 785
                reflectiveQuadTo(
                    x1 = 164.0f,
                    y1 = 785.0f,
                )
                // q -25 -84 -41 -145.5
                quadToRelative(
                    dx1 = -25.0f,
                    dy1 = -84.0f,
                    dx2 = -41.0f,
                    dy2 = -145.5f,
                )
                // t -25.5 -108
                reflectiveQuadToRelative(
                    dx1 = -25.5f,
                    dy1 = -108.0f,
                )
                // T 84 449
                reflectiveQuadTo(
                    x1 = 84.0f,
                    y1 = 449.0f,
                )
                // t -4 -69
                reflectiveQuadToRelative(
                    dx1 = -4.0f,
                    dy1 = -69.0f,
                )
                // q 0 -92 64 -156
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -92.0f,
                    dx2 = 64.0f,
                    dy2 = -156.0f,
                )
                // t 156 -64
                reflectiveQuadToRelative(
                    dx1 = 156.0f,
                    dy1 = -64.0f,
                )
                // l 200 0
                lineToRelative(dx = 200.0f, dy = 0.0f)
                // q 27 -36 68.5 -58
                quadToRelative(
                    dx1 = 27.0f,
                    dy1 = -36.0f,
                    dx2 = 68.5f,
                    dy2 = -58.0f,
                )
                // t 91.5 -22
                reflectiveQuadToRelative(
                    dx1 = 91.5f,
                    dy1 = -22.0f,
                )
                // q 25 0 42.5 17.5
                quadToRelative(
                    dx1 = 25.0f,
                    dy1 = 0.0f,
                    dx2 = 42.5f,
                    dy2 = 17.5f,
                )
                // T 720 140
                reflectiveQuadTo(
                    x1 = 720.0f,
                    y1 = 140.0f,
                )
                // q 0 6 -1.5 12
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 6.0f,
                    dx2 = -1.5f,
                    dy2 = 12.0f,
                )
                // t -3.5 11
                reflectiveQuadToRelative(
                    dx1 = -3.5f,
                    dy1 = 11.0f,
                )
                // q -4 11 -7.5 22
                quadToRelative(
                    dx1 = -4.0f,
                    dy1 = 11.0f,
                    dx2 = -7.5f,
                    dy2 = 22.0f,
                )
                // t -5.5 24
                reflectiveQuadToRelative(
                    dx1 = -5.5f,
                    dy1 = 24.0f,
                )
                // l 91 91
                lineToRelative(dx = 91.0f, dy = 91.0f)
                // l 47 0
                lineToRelative(dx = 47.0f, dy = 0.0f)
                // q 17 0 28.5 11.5
                quadToRelative(
                    dx1 = 17.0f,
                    dy1 = 0.0f,
                    dx2 = 28.5f,
                    dy2 = 11.5f,
                )
                // T 880 340
                reflectiveQuadTo(
                    x1 = 880.0f,
                    y1 = 340.0f,
                )
                // l 0 210
                lineToRelative(dx = 0.0f, dy = 210.0f)
                // q 0 13 -7.5 23
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 13.0f,
                    dx2 = -7.5f,
                    dy2 = 23.0f,
                )
                // T 852 588
                reflectiveQuadTo(
                    x1 = 852.0f,
                    y1 = 588.0f,
                )
                // l -85 28
                lineToRelative(dx = -85.0f, dy = 28.0f)
                // l -50 167
                lineToRelative(dx = -50.0f, dy = 167.0f)
                // q -8 26 -29 41.5
                quadToRelative(
                    dx1 = -8.0f,
                    dy1 = 26.0f,
                    dx2 = -29.0f,
                    dy2 = 41.5f,
                )
                // T 640 840
                reflectiveQuadTo(
                    x1 = 640.0f,
                    y1 = 840.0f,
                )
                // l -80 0
                lineToRelative(dx = -80.0f, dy = 0.0f)
                // q -33 0 -56.5 -23.5
                quadToRelative(
                    dx1 = -33.0f,
                    dy1 = 0.0f,
                    dx2 = -56.5f,
                    dy2 = -23.5f,
                )
                // T 480 760
                reflectiveQuadTo(
                    x1 = 480.0f,
                    y1 = 760.0f,
                )
                // l -80 0
                lineToRelative(dx = -80.0f, dy = 0.0f)
                // q 0 33 -23.5 56.5
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 33.0f,
                    dx2 = -23.5f,
                    dy2 = 56.5f,
                )
                // T 320 840z
                reflectiveQuadTo(
                    x1 = 320.0f,
                    y1 = 840.0f,
                )
                close()
                // m 3 -80
                moveToRelative(dx = 3.0f, dy = -80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 0 -80
                lineToRelative(dx = 0.0f, dy = -80.0f)
                // l 240 0
                lineToRelative(dx = 240.0f, dy = 0.0f)
                // l 0 80
                lineToRelative(dx = 0.0f, dy = 80.0f)
                // l 80 0
                lineToRelative(dx = 80.0f, dy = 0.0f)
                // l 62 -206
                lineToRelative(dx = 62.0f, dy = -206.0f)
                // l 98 -33
                lineToRelative(dx = 98.0f, dy = -33.0f)
                // l 0 -141
                lineToRelative(dx = 0.0f, dy = -141.0f)
                // l -40 0
                lineToRelative(dx = -40.0f, dy = 0.0f)
                // L 620 240
                lineTo(x = 620.0f, y = 240.0f)
                // q 0 -20 2.5 -39
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = -20.0f,
                    dx2 = 2.5f,
                    dy2 = -39.0f,
                )
                // t 7.5 -37
                reflectiveQuadToRelative(
                    dx1 = 7.5f,
                    dy1 = -37.0f,
                )
                // q -29 8 -51 27.5
                quadToRelative(
                    dx1 = -29.0f,
                    dy1 = 8.0f,
                    dx2 = -51.0f,
                    dy2 = 27.5f,
                )
                // T 547 240
                reflectiveQuadTo(
                    x1 = 547.0f,
                    y1 = 240.0f,
                )
                // L 300 240
                lineTo(x = 300.0f, y = 240.0f)
                // q -58 0 -99 41
                quadToRelative(
                    dx1 = -58.0f,
                    dy1 = 0.0f,
                    dx2 = -99.0f,
                    dy2 = 41.0f,
                )
                // t -41 99
                reflectiveQuadToRelative(
                    dx1 = -41.0f,
                    dy1 = 99.0f,
                )
                // q 0 41 21 140.5
                quadToRelative(
                    dx1 = 0.0f,
                    dy1 = 41.0f,
                    dx2 = 21.0f,
                    dy2 = 140.5f,
                )
                // T 240 760
                reflectiveQuadTo(
                    x1 = 240.0f,
                    y1 = 760.0f,
                )
                // m 428.5 -331.5
                moveToRelative(dx = 428.5f, dy = -331.5f)
                // Q 680 417 680 400
                quadTo(
                    x1 = 680.0f,
                    y1 = 417.0f,
                    x2 = 680.0f,
                    y2 = 400.0f,
                )
                // t -11.5 -28.5
                reflectiveQuadToRelative(
                    dx1 = -11.5f,
                    dy1 = -28.5f,
                )
                // T 640 360
                reflectiveQuadTo(
                    x1 = 640.0f,
                    y1 = 360.0f,
                )
                // t -28.5 11.5
                reflectiveQuadToRelative(
                    dx1 = -28.5f,
                    dy1 = 11.5f,
                )
                // T 600 400
                reflectiveQuadTo(
                    x1 = 600.0f,
                    y1 = 400.0f,
                )
                // t 11.5 28.5
                reflectiveQuadToRelative(
                    dx1 = 11.5f,
                    dy1 = 28.5f,
                )
                // T 640 440
                reflectiveQuadTo(
                    x1 = 640.0f,
                    y1 = 440.0f,
                )
                // t 28.5 -11.5
                reflectiveQuadToRelative(
                    dx1 = 28.5f,
                    dy1 = -11.5f,
                )
                // M 480 360
                moveTo(x = 480.0f, y = 360.0f)
                // q 17 0 28.5 -11.5
                quadToRelative(
                    dx1 = 17.0f,
                    dy1 = 0.0f,
                    dx2 = 28.5f,
                    dy2 = -11.5f,
                )
                // T 520 320
                reflectiveQuadTo(
                    x1 = 520.0f,
                    y1 = 320.0f,
                )
                // t -11.5 -28.5
                reflectiveQuadToRelative(
                    dx1 = -11.5f,
                    dy1 = -28.5f,
                )
                // T 480 280
                reflectiveQuadTo(
                    x1 = 480.0f,
                    y1 = 280.0f,
                )
                // L 360 280
                lineTo(x = 360.0f, y = 280.0f)
                // q -17 0 -28.5 11.5
                quadToRelative(
                    dx1 = -17.0f,
                    dy1 = 0.0f,
                    dx2 = -28.5f,
                    dy2 = 11.5f,
                )
                // T 320 320
                reflectiveQuadTo(
                    x1 = 320.0f,
                    y1 = 320.0f,
                )
                // t 11.5 28.5
                reflectiveQuadToRelative(
                    dx1 = 11.5f,
                    dy1 = 28.5f,
                )
                // T 360 360z
                reflectiveQuadTo(
                    x1 = 360.0f,
                    y1 = 360.0f,
                )
                close()
                // m 0 102
                moveToRelative(dx = 0.0f, dy = 102.0f)
            }
        }.build().also { _credit = it }
    }


@Suppress("ObjectPropertyName")
private var _credit: ImageVector? = null
