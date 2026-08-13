package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Credit: ImageVector
    get() {
        if (_Credit != null) {
            return _Credit!!
        }
        _Credit = ImageVector.Builder(
            name = "Credit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(237f, 840f)
                quadToRelative(-23f, 0f, -44.5f, -16f)
                reflectiveQuadTo(164f, 785f)
                quadToRelative(-25f, -84f, -41f, -145.5f)
                reflectiveQuadToRelative(-25.5f, -108f)
                quadTo(88f, 485f, 84f, 449f)
                reflectiveQuadToRelative(-4f, -69f)
                quadToRelative(0f, -92f, 64f, -156f)
                reflectiveQuadToRelative(156f, -64f)
                horizontalLineToRelative(200f)
                quadToRelative(27f, -36f, 68.5f, -58f)
                reflectiveQuadToRelative(91.5f, -22f)
                quadToRelative(25f, 0f, 42.5f, 17.5f)
                reflectiveQuadTo(720f, 140f)
                quadToRelative(0f, 6f, -1.5f, 12f)
                reflectiveQuadToRelative(-3.5f, 11f)
                quadToRelative(-4f, 11f, -7.5f, 22f)
                reflectiveQuadToRelative(-5.5f, 24f)
                lineToRelative(91f, 91f)
                horizontalLineToRelative(47f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(880f, 340f)
                verticalLineToRelative(210f)
                quadToRelative(0f, 13f, -7.5f, 23f)
                reflectiveQuadTo(852f, 588f)
                lineToRelative(-85f, 28f)
                lineToRelative(-50f, 167f)
                quadToRelative(-8f, 26f, -29f, 41.5f)
                reflectiveQuadTo(640f, 840f)
                horizontalLineToRelative(-80f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(480f, 760f)
                horizontalLineToRelative(-80f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(320f, 840f)
                horizontalLineToRelative(-83f)
                close()
                moveTo(668.5f, 428.5f)
                quadTo(680f, 417f, 680f, 400f)
                reflectiveQuadToRelative(-11.5f, -28.5f)
                quadTo(657f, 360f, 640f, 360f)
                reflectiveQuadToRelative(-28.5f, 11.5f)
                quadTo(600f, 383f, 600f, 400f)
                reflectiveQuadToRelative(11.5f, 28.5f)
                quadTo(623f, 440f, 640f, 440f)
                reflectiveQuadToRelative(28.5f, -11.5f)
                close()
                moveTo(480f, 360f)
                quadToRelative(17f, 0f, 28.5f, -11.5f)
                reflectiveQuadTo(520f, 320f)
                quadToRelative(0f, -17f, -11.5f, -28.5f)
                reflectiveQuadTo(480f, 280f)
                lineTo(360f, 280f)
                quadToRelative(-17f, 0f, -28.5f, 11.5f)
                reflectiveQuadTo(320f, 320f)
                quadToRelative(0f, 17f, 11.5f, 28.5f)
                reflectiveQuadTo(360f, 360f)
                horizontalLineToRelative(120f)
                close()
            }
        }.build()

        return _Credit!!
    }

@Suppress("ObjectPropertyName")
private var _Credit: ImageVector? = null
