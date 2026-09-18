package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Appointment: ImageVector
    get() {
        if (_Appointment != null) {
            return _Appointment!!
        }
        _Appointment = ImageVector.Builder(
            name = "Appointment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF1F1F1F))) {
                moveTo(200f, 320f)
                horizontalLineToRelative(560f)
                verticalLineToRelative(-80f)
                lineTo(200f, 240f)
                verticalLineToRelative(80f)
                close()
                moveTo(200f, 320f)
                verticalLineToRelative(-80f)
                verticalLineToRelative(80f)
                close()
                moveTo(200f, 880f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(120f, 800f)
                verticalLineToRelative(-560f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(200f, 160f)
                horizontalLineToRelative(40f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(80f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(80f)
                horizontalLineToRelative(40f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(840f, 240f)
                verticalLineToRelative(227f)
                quadToRelative(-19f, -9f, -39f, -15f)
                reflectiveQuadToRelative(-41f, -9f)
                verticalLineToRelative(-43f)
                lineTo(200f, 400f)
                verticalLineToRelative(400f)
                horizontalLineToRelative(252f)
                quadToRelative(7f, 22f, 16.5f, 42f)
                reflectiveQuadTo(491f, 880f)
                lineTo(200f, 880f)
                close()
                moveTo(578.5f, 861.5f)
                quadTo(520f, 803f, 520f, 720f)
                reflectiveQuadToRelative(58.5f, -141.5f)
                quadTo(637f, 520f, 720f, 520f)
                reflectiveQuadToRelative(141.5f, 58.5f)
                quadTo(920f, 637f, 920f, 720f)
                reflectiveQuadTo(861.5f, 861.5f)
                quadTo(803f, 920f, 720f, 920f)
                reflectiveQuadTo(578.5f, 861.5f)
                close()
                moveTo(787f, 815f)
                lineToRelative(28f, -28f)
                lineToRelative(-75f, -75f)
                verticalLineToRelative(-112f)
                horizontalLineToRelative(-40f)
                verticalLineToRelative(128f)
                lineToRelative(87f, 87f)
                close()
            }
        }.build()

        return _Appointment!!
    }

@Suppress("ObjectPropertyName")
private var _Appointment: ImageVector? = null
