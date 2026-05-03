package restarhalf.stellar.schedule.ui.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Bot: ImageVector
    get() {
        val current = _bot
        if (current != null) return current

        return ImageVector.Builder(
            name = "Rounded.Bot",
            defaultWidth = 1.0.dp,
            defaultHeight = 1.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            // M20.62 10.84 a14 14 0 0 1 -4.45 -3 14 14 0 0 1 -3.68 -6.46 .5 .5 0 0 0 -.98 0 14 14 0 0 1 -3.68 6.45 14 14 0 0 1 -4.45 3 13 13 0 0 1 -2 .68 .5 .5 0 0 0 0 .98 13 13 0 0 1 2 .68 14 14 0 0 1 4.45 3 14 14 0 0 1 3.68 6.45 .5 .5 0 0 0 .98 0 13 13 0 0 1 .67 -2 14 14 0 0 1 3 -4.45 14 14 0 0 1 6.46 -3.68 .5 .5 0 0 0 0 -.98 13 13 0 0 1 -2 -.67
            path(
                fill = SolidColor(Color(0xFF3186FF)),
            ) {
                // M 20.62 10.84
                moveTo(x = 20.62f, y = 10.84f)
                // a 14 14 0 0 1 -4.45 -3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.45f,
                    dy1 = -3.0f,
                )
                // a 14 14 0 0 1 -3.68 -6.46
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.68f,
                    dy1 = -6.46f,
                )
                // a 0.5 0.5 0 0 0 -0.98 0
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.98f,
                    dy1 = 0.0f,
                )
                // a 14 14 0 0 1 -3.68 6.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.68f,
                    dy1 = 6.45f,
                )
                // a 14 14 0 0 1 -4.45 3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.45f,
                    dy1 = 3.0f,
                )
                // a 13 13 0 0 1 -2 0.68
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.0f,
                    dy1 = 0.68f,
                )
                // a 0.5 0.5 0 0 0 0 0.98
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = 0.98f,
                )
                // a 13 13 0 0 1 2 0.68
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 2.0f,
                    dy1 = 0.68f,
                )
                // a 14 14 0 0 1 4.45 3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 4.45f,
                    dy1 = 3.0f,
                )
                // a 14 14 0 0 1 3.68 6.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.68f,
                    dy1 = 6.45f,
                )
                // a 0.5 0.5 0 0 0 0.98 0
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.98f,
                    dy1 = 0.0f,
                )
                // a 13 13 0 0 1 0.67 -2
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.67f,
                    dy1 = -2.0f,
                )
                // a 14 14 0 0 1 3 -4.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.0f,
                    dy1 = -4.45f,
                )
                // a 14 14 0 0 1 6.46 -3.68
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 6.46f,
                    dy1 = -3.68f,
                )
                // a 0.5 0.5 0 0 0 0 -0.98
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = -0.98f,
                )
                // a 13 13 0 0 1 -2 -0.67
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.0f,
                    dy1 = -0.67f,
                )
            }
            // M20.62 10.84 a14 14 0 0 1 -4.45 -3 14 14 0 0 1 -3.68 -6.46 .5 .5 0 0 0 -.98 0 14 14 0 0 1 -3.68 6.45 14 14 0 0 1 -4.45 3 13 13 0 0 1 -2 .68 .5 .5 0 0 0 0 .98 13 13 0 0 1 2 .68 14 14 0 0 1 4.45 3 14 14 0 0 1 3.68 6.45 .5 .5 0 0 0 .98 0 13 13 0 0 1 .67 -2 14 14 0 0 1 3 -4.45 14 14 0 0 1 6.46 -3.68 .5 .5 0 0 0 0 -.98 13 13 0 0 1 -2 -.67
            path(
                fill = Brush.linearGradient(
                    0.0f to Color(0xFF08B962),
                    1.0f to Color(0x0008B962),
                    start = Offset(x = 7.0f, y = 15.5f),
                    end = Offset(x = 11.0f, y = 12.0f),
                ),
            ) {
                // M 20.62 10.84
                moveTo(x = 20.62f, y = 10.84f)
                // a 14 14 0 0 1 -4.45 -3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.45f,
                    dy1 = -3.0f,
                )
                // a 14 14 0 0 1 -3.68 -6.46
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.68f,
                    dy1 = -6.46f,
                )
                // a 0.5 0.5 0 0 0 -0.98 0
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.98f,
                    dy1 = 0.0f,
                )
                // a 14 14 0 0 1 -3.68 6.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.68f,
                    dy1 = 6.45f,
                )
                // a 14 14 0 0 1 -4.45 3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.45f,
                    dy1 = 3.0f,
                )
                // a 13 13 0 0 1 -2 0.68
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.0f,
                    dy1 = 0.68f,
                )
                // a 0.5 0.5 0 0 0 0 0.98
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = 0.98f,
                )
                // a 13 13 0 0 1 2 0.68
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 2.0f,
                    dy1 = 0.68f,
                )
                // a 14 14 0 0 1 4.45 3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 4.45f,
                    dy1 = 3.0f,
                )
                // a 14 14 0 0 1 3.68 6.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.68f,
                    dy1 = 6.45f,
                )
                // a 0.5 0.5 0 0 0 0.98 0
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.98f,
                    dy1 = 0.0f,
                )
                // a 13 13 0 0 1 0.67 -2
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.67f,
                    dy1 = -2.0f,
                )
                // a 14 14 0 0 1 3 -4.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.0f,
                    dy1 = -4.45f,
                )
                // a 14 14 0 0 1 6.46 -3.68
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 6.46f,
                    dy1 = -3.68f,
                )
                // a 0.5 0.5 0 0 0 0 -0.98
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = -0.98f,
                )
                // a 13 13 0 0 1 -2 -0.67
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.0f,
                    dy1 = -0.67f,
                )
            }
            // M20.62 10.84 a14 14 0 0 1 -4.45 -3 14 14 0 0 1 -3.68 -6.46 .5 .5 0 0 0 -.98 0 14 14 0 0 1 -3.68 6.45 14 14 0 0 1 -4.45 3 13 13 0 0 1 -2 .68 .5 .5 0 0 0 0 .98 13 13 0 0 1 2 .68 14 14 0 0 1 4.45 3 14 14 0 0 1 3.68 6.45 .5 .5 0 0 0 .98 0 13 13 0 0 1 .67 -2 14 14 0 0 1 3 -4.45 14 14 0 0 1 6.46 -3.68 .5 .5 0 0 0 0 -.98 13 13 0 0 1 -2 -.67
            path(
                fill = Brush.linearGradient(
                    0.0f to Color(0xFFF94543),
                    1.0f to Color(0x00F94543),
                    start = Offset(x = 8.0f, y = 5.5f),
                    end = Offset(x = 11.5f, y = 11.0f),
                ),
            ) {
                // M 20.62 10.84
                moveTo(x = 20.62f, y = 10.84f)
                // a 14 14 0 0 1 -4.45 -3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.45f,
                    dy1 = -3.0f,
                )
                // a 14 14 0 0 1 -3.68 -6.46
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.68f,
                    dy1 = -6.46f,
                )
                // a 0.5 0.5 0 0 0 -0.98 0
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.98f,
                    dy1 = 0.0f,
                )
                // a 14 14 0 0 1 -3.68 6.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.68f,
                    dy1 = 6.45f,
                )
                // a 14 14 0 0 1 -4.45 3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.45f,
                    dy1 = 3.0f,
                )
                // a 13 13 0 0 1 -2 0.68
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.0f,
                    dy1 = 0.68f,
                )
                // a 0.5 0.5 0 0 0 0 0.98
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = 0.98f,
                )
                // a 13 13 0 0 1 2 0.68
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 2.0f,
                    dy1 = 0.68f,
                )
                // a 14 14 0 0 1 4.45 3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 4.45f,
                    dy1 = 3.0f,
                )
                // a 14 14 0 0 1 3.68 6.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.68f,
                    dy1 = 6.45f,
                )
                // a 0.5 0.5 0 0 0 0.98 0
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.98f,
                    dy1 = 0.0f,
                )
                // a 13 13 0 0 1 0.67 -2
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.67f,
                    dy1 = -2.0f,
                )
                // a 14 14 0 0 1 3 -4.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.0f,
                    dy1 = -4.45f,
                )
                // a 14 14 0 0 1 6.46 -3.68
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 6.46f,
                    dy1 = -3.68f,
                )
                // a 0.5 0.5 0 0 0 0 -0.98
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = -0.98f,
                )
                // a 13 13 0 0 1 -2 -0.67
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.0f,
                    dy1 = -0.67f,
                )
            }
            // M20.62 10.84 a14 14 0 0 1 -4.45 -3 14 14 0 0 1 -3.68 -6.46 .5 .5 0 0 0 -.98 0 14 14 0 0 1 -3.68 6.45 14 14 0 0 1 -4.45 3 13 13 0 0 1 -2 .68 .5 .5 0 0 0 0 .98 13 13 0 0 1 2 .68 14 14 0 0 1 4.45 3 14 14 0 0 1 3.68 6.45 .5 .5 0 0 0 .98 0 13 13 0 0 1 .67 -2 14 14 0 0 1 3 -4.45 14 14 0 0 1 6.46 -3.68 .5 .5 0 0 0 0 -.98 13 13 0 0 1 -2 -.67
            path(
                fill = Brush.linearGradient(
                    0.0f to Color(0xFFFABC12),
                    0.46f to Color(0x00FABC12),
                    start = Offset(x = 3.5f, y = 13.5f),
                    end = Offset(x = 17.5f, y = 12.0f),
                ),
            ) {
                // M 20.62 10.84
                moveTo(x = 20.62f, y = 10.84f)
                // a 14 14 0 0 1 -4.45 -3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.45f,
                    dy1 = -3.0f,
                )
                // a 14 14 0 0 1 -3.68 -6.46
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.68f,
                    dy1 = -6.46f,
                )
                // a 0.5 0.5 0 0 0 -0.98 0
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.98f,
                    dy1 = 0.0f,
                )
                // a 14 14 0 0 1 -3.68 6.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.68f,
                    dy1 = 6.45f,
                )
                // a 14 14 0 0 1 -4.45 3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -4.45f,
                    dy1 = 3.0f,
                )
                // a 13 13 0 0 1 -2 0.68
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.0f,
                    dy1 = 0.68f,
                )
                // a 0.5 0.5 0 0 0 0 0.98
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = 0.98f,
                )
                // a 13 13 0 0 1 2 0.68
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 2.0f,
                    dy1 = 0.68f,
                )
                // a 14 14 0 0 1 4.45 3
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 4.45f,
                    dy1 = 3.0f,
                )
                // a 14 14 0 0 1 3.68 6.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.68f,
                    dy1 = 6.45f,
                )
                // a 0.5 0.5 0 0 0 0.98 0
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.98f,
                    dy1 = 0.0f,
                )
                // a 13 13 0 0 1 0.67 -2
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.67f,
                    dy1 = -2.0f,
                )
                // a 14 14 0 0 1 3 -4.45
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 3.0f,
                    dy1 = -4.45f,
                )
                // a 14 14 0 0 1 6.46 -3.68
                arcToRelative(
                    a = 14.0f,
                    b = 14.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 6.46f,
                    dy1 = -3.68f,
                )
                // a 0.5 0.5 0 0 0 0 -0.98
                arcToRelative(
                    a = 0.5f,
                    b = 0.5f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = -0.98f,
                )
                // a 13 13 0 0 1 -2 -0.67
                arcToRelative(
                    a = 13.0f,
                    b = 13.0f,
                    theta = 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -2.0f,
                    dy1 = -0.67f,
                )
            }
        }.build().also { _bot = it }
    }


@Suppress("ObjectPropertyName")
private var _bot: ImageVector? = null
