package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Tune: ImageVector
    get() {
        if (_Tune != null) {
            return _Tune!!
        }
        _Tune = ImageVector.Builder(
            name = "Tune",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 240f)
                lineTo(800f, 240f)
                lineTo(800f, 288f)
                lineTo(160f, 288f)
                close()
                moveTo(160f, 456f)
                lineTo(800f, 456f)
                lineTo(800f, 504f)
                lineTo(160f, 504f)
                close()
                moveTo(160f, 672f)
                lineTo(800f, 672f)
                lineTo(800f, 720f)
                lineTo(160f, 720f)
                close()

                moveTo(560f, 264f)
                arcToRelative(100f, 100f, 0f, true, true, 200f, 0f)
                arcToRelative(100f, 100f, 0f, true, true, -200f, 0f)
                close()

                moveTo(280f, 480f)
                arcToRelative(100f, 100f, 0f, true, true, 200f, 0f)
                arcToRelative(100f, 100f, 0f, true, true, -200f, 0f)
                close()

                moveTo(460f, 696f)
                arcToRelative(100f, 100f, 0f, true, true, 200f, 0f)
                arcToRelative(100f, 100f, 0f, true, true, -200f, 0f)
                close()
            }
        }.build()

        return _Tune!!
    }

@Suppress("ObjectPropertyName")
private var _Tune: ImageVector? = null
