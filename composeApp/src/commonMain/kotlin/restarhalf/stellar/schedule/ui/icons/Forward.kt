package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Forward: ImageVector
    get() {
        if (_Forward != null) {
            return _Forward!!
        }
        _Forward = ImageVector.Builder(
            name = "Forward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(579f, 480f)
                lineTo(285f, 186f)
                quadToRelative(-15f, -15f, -14.5f, -35.5f)
                reflectiveQuadTo(286f, 115f)
                quadToRelative(15f, -15f, 35.5f, -15f)
                reflectiveQuadToRelative(35.5f, 15f)
                lineToRelative(307f, 308f)
                quadToRelative(12f, 12f, 18f, 27f)
                reflectiveQuadToRelative(6f, 30f)
                quadToRelative(0f, 15f, -6f, 30f)
                reflectiveQuadToRelative(-18f, 27f)
                lineTo(356f, 845f)
                quadToRelative(-15f, 15f, -35f, 14.5f)
                reflectiveQuadTo(286f, 844f)
                quadToRelative(-15f, -15f, -15f, -35.5f)
                reflectiveQuadToRelative(15f, -35.5f)
                lineToRelative(293f, -293f)
                close()
            }
        }.build()

        return _Forward!!
    }

@Suppress("ObjectPropertyName")
private var _Forward: ImageVector? = null
