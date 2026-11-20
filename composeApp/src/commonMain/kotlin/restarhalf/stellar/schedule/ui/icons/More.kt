package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val More: ImageVector
    get() {
        if (_More != null) {
            return _More!!
        }
        _More = ImageVector.Builder(
            name = "More",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(480f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(400f, 720f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(480f, 640f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(560f, 720f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(480f, 800f)
                close()
                moveTo(480f, 560f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(400f, 480f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(480f, 400f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(560f, 480f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(480f, 560f)
                close()
                moveTo(480f, 320f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(400f, 240f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(480f, 160f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(560f, 240f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(480f, 320f)
                close()
            }
        }.build()

        return _More!!
    }

@Suppress("ObjectPropertyName")
private var _More: ImageVector? = null
