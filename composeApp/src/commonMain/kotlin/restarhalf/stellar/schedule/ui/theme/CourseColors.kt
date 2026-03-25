package restarhalf.stellar.schedule.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

typealias CourseColor = Triple<Color, Color, Color>

val courseColorPaletteLight: List<CourseColor> =
    listOf(
        Triple(Color(0xFFF0F4FF), Color(0xFF5B89D3), Color(0xFF8BAAE0)),

        Triple(Color(0xFFEAF7E8), Color(0xFF52A55C), Color(0xFF88C38F)),

        Triple(Color(0xFFFFF8E1), Color(0xFFE69A3A), Color(0xFFEBB56E)),

        Triple(Color(0xFFFCE4EC), Color(0xFFD85988), Color(0xFFE28BAA)),

        Triple(Color(0xFFF3E5F5), Color(0xFF9C56B8), Color(0xFFB986CE)),

        Triple(Color(0xFFE0F7FA), Color(0xFF0097A7), Color(0xFF4DD0E1)),

        Triple(Color(0xFFFFF3E0), Color(0xFFF57C00), Color(0xFFFFB74D)),

        Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Color(0xFFE57373)),

        Triple(Color(0xFFE8EAF6), Color(0xFF3F51B5), Color(0xFF7986CB)),

        Triple(Color(0xFFE0F2F1), Color(0xFF009688), Color(0xFF4DB6AC))
    )

val courseColorPaletteDark: List<CourseColor> =
    listOf(

        Triple(Color(0xFF2A3A5A), Color(0xFFA9C7FF), Color(0xFF88A8D9)),

        Triple(Color(0xFF294538), Color(0xFF9BE3B0), Color(0xFF79C693)),

        Triple(Color(0xFF4A3F23), Color(0xFFFFD08A), Color(0xFFE5B86B)),

        Triple(Color(0xFF4A2D3A), Color(0xFFFFA7C7), Color(0xFFE08DAE)),

        Triple(Color(0xFF3F3154), Color(0xFFD0B2FF), Color(0xFFB898E2)),

        Triple(Color(0xFF24444A), Color(0xFF93E3EE), Color(0xFF72C9D5)),

        Triple(Color(0xFF4A3628), Color(0xFFFFC59A), Color(0xFFE4A878)),

        Triple(Color(0xFF4D2D2D), Color(0xFFFFA8A8), Color(0xFFE08787)),

        Triple(Color(0xFF2F3551), Color(0xFFAEC0FF), Color(0xFF8FA3E0)),

        Triple(Color(0xFF214242), Color(0xFF8DE0D5), Color(0xFF73C4BA))
    )


fun getCoursePaletteIndex(courseName: String): Int {
    return courseName.hashCode().absoluteValue % courseColorPaletteLight.size
}

fun pickCourseColor(courseName: String, darkMode: Boolean): Color {
    val palette = if (darkMode) courseColorPaletteDark else courseColorPaletteLight
    return palette[getCoursePaletteIndex(courseName)].first
}

fun pickCourseTitleColor(courseName: String, darkMode: Boolean): Color {
    val palette = if (darkMode) courseColorPaletteDark else courseColorPaletteLight
    return palette[getCoursePaletteIndex(courseName)].second
}

fun pickCourseSubColor(courseName: String, darkMode: Boolean): Color {
    val palette = if (darkMode) courseColorPaletteDark else courseColorPaletteLight
    return palette[getCoursePaletteIndex(courseName)].third
}
