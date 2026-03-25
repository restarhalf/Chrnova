package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 首页图标
 *
 * 房屋图标，用于首页导航。
 */
val Home: ImageVector
    get() =
        ImageVector.Builder(
            name = "Rounded.Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .apply {
                path(fill = SolidColor(Color(0xFF1F1F1F)), pathFillType = PathFillType.NonZero) {
                    moveTo(10.0f, 19.0f)
                    verticalLineToRelative(-5.0f)
                    horizontalLineToRelative(4.0f)
                    verticalLineToRelative(5.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(3.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    verticalLineToRelative(-7.0f)
                    horizontalLineToRelative(1.7f)
                    curveToRelative(0.46f, 0.0f, 0.68f, -0.57f, 0.33f, -0.87f)
                    lineTo(12.67f, 3.6f)
                    curveToRelative(-0.38f, -0.34f, -0.96f, -0.34f, -1.34f, 0.0f)
                    lineToRelative(-8.36f, 7.53f)
                    curveToRelative(-0.34f, 0.3f, -0.13f, 0.87f, 0.33f, 0.87f)
                    horizontalLineTo(5.0f)
                    verticalLineToRelative(7.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(3.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    close()
                }
            }
            .build()
