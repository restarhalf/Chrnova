package restarhalf.stellar.schedule.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

/**
 * 课程颜色三元组类型
 * 
 * 第一个颜色：卡片背景色
 * 第二个颜色：标题颜色
 * 第三个颜色：副标题颜色
 */
typealias CourseColor = Triple<Color, Color, Color>

/** 浅色模式课程颜色调色板 */
val courseColorPaletteLight: List<CourseColor> =
    listOf(
        Triple(Color(0xFFF0F4FF), Color(0xFF5B89D3), Color(0xFF8BAAE0)),  // 蓝色
        Triple(Color(0xFFEAF7E8), Color(0xFF52A55C), Color(0xFF88C38F)),  // 绿色
        Triple(Color(0xFFFFF8E1), Color(0xFFE69A3A), Color(0xFFEBB56E)),  // 橙色
        Triple(Color(0xFFFCE4EC), Color(0xFFD85988), Color(0xFFE28BAA)),  // 粉色
        Triple(Color(0xFFF3E5F5), Color(0xFF9C56B8), Color(0xFFB986CE)),  // 紫色
        Triple(Color(0xFFE0F7FA), Color(0xFF0097A7), Color(0xFF4DD0E1)),  // 青色
        Triple(Color(0xFFFFF3E0), Color(0xFFF57C00), Color(0xFFFFB74D)),  // 深橙
        Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Color(0xFFE57373)),  // 红色
        Triple(Color(0xFFE8EAF6), Color(0xFF3F51B5), Color(0xFF7986CB)),  // 靛蓝
        Triple(Color(0xFFE0F2F1), Color(0xFF009688), Color(0xFF4DB6AC)),  // 青绿
        Triple(Color(0xFFF1F8E9), Color(0xFF689F38), Color(0xFF9CCC65)),  // 黄绿
        Triple(Color(0xFFFFF0E0), Color(0xFFBF5B00), Color(0xFFD4944A)),  // 赭石
        Triple(Color(0xFFE8EAF6), Color(0xFF5C6BC0), Color(0xFF8E99D0)),  // 钴蓝
        Triple(Color(0xFFFFFDE7), Color(0xFFE65100), Color(0xFFEF6C00)),  // 琥珀
        Triple(Color(0xFFE0F7FA), Color(0xFF00838F), Color(0xFF4DD0E1)),  // 水鸭绿
        Triple(Color(0xFFEDE7F6), Color(0xFF7E57C2), Color(0xFFB39DDB)),  // 深紫
        Triple(Color(0xFFE1F5FE), Color(0xFF039BE5), Color(0xFF4FC3F7)),  // 浅蓝
        Triple(Color(0xFFFFF9C4), Color(0xFF8D6E00), Color(0xFFA68B00)),  // 黄色
        Triple(Color(0xFFFFF0F0), Color(0xFFEF5350), Color(0xFFEF9A9A)),  // 珊瑚
        Triple(Color(0xFFE0F2F1), Color(0xFF26A69A), Color(0xFF80CBC4)),  // 薄荷
        Triple(Color(0xFFF3E5F5), Color(0xFFAB47BC), Color(0xFFCE93D8)),  // 薰衣草
        Triple(Color(0xFFFFF3E0), Color(0xFFFF7043), Color(0xFFFFAB91)),  // 蜜桃
        Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFF66BB6A)),  // 翡翠绿
        Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), Color(0xFF42A5F5)),  // 宝石蓝
        Triple(Color(0xFFFCE4EC), Color(0xFFC62828), Color(0xFFEF9A9A)),  // 樱红
        Triple(Color(0xFFFFF8E1), Color(0xFF996515), Color(0xFFC68B20)),  // 金色
        Triple(Color(0xFFFCE4EC), Color(0xFFEC407A), Color(0xFFF48FB1)),  // 珊瑚粉
        Triple(Color(0xFFF1F8E9), Color(0xFF8BC34A), Color(0xFFC5E1A5)),  // 橄榄
        Triple(Color(0xFFE1F5FE), Color(0xFF0288D1), Color(0xFF4DD0E1)),  // 天蓝
        Triple(Color(0xFFEFEBE9), Color(0xFFAD1457), Color(0xFFC2185B)),  // 酒红
    )

/** 深色模式课程颜色调色板 */
val courseColorPaletteDark: List<CourseColor> =
    listOf(
        Triple(Color(0xFF2A3A5A), Color(0xFFA9C7FF), Color(0xFF88A8D9)),  // 蓝色
        Triple(Color(0xFF294538), Color(0xFF9BE3B0), Color(0xFF79C693)),  // 绿色
        Triple(Color(0xFF4A3F23), Color(0xFFFFD08A), Color(0xFFE5B86B)),  // 橙色
        Triple(Color(0xFF4A2D3A), Color(0xFFFFA7C7), Color(0xFFE08DAE)),  // 粉色
        Triple(Color(0xFF3F3154), Color(0xFFD0B2FF), Color(0xFFB898E2)),  // 紫色
        Triple(Color(0xFF24444A), Color(0xFF93E3EE), Color(0xFF72C9D5)),  // 青色
        Triple(Color(0xFF4A3628), Color(0xFFFFC59A), Color(0xFFE4A878)),  // 深橙
        Triple(Color(0xFF4D2D2D), Color(0xFFFFA8A8), Color(0xFFE08787)),  // 红色
        Triple(Color(0xFF2F3551), Color(0xFFAEC0FF), Color(0xFF8FA3E0)),  // 靛蓝
        Triple(Color(0xFF214242), Color(0xFF8DE0D5), Color(0xFF73C4BA)),  // 青绿
        Triple(Color(0xFF2A3D1F), Color(0xFFA5D6A7), Color(0xFF81C784)),  // 黄绿
        Triple(Color(0xFF3D2A10), Color(0xFFD4944A), Color(0xFFBF7B30)),  // 赭石
        Triple(Color(0xFF252D50), Color(0xFF8E99D0), Color(0xFF6B78B8)),  // 钴蓝
        Triple(Color(0xFF3D3518), Color(0xFFFFAB40), Color(0xFFFF9100)),  // 琥珀
        Triple(Color(0xFF1A3838), Color(0xFF4DD0E1), Color(0xFF26C6DA)),  // 水鸭绿
        Triple(Color(0xFF352650), Color(0xFFCE93D8), Color(0xFFBA68C8)),  // 深紫
        Triple(Color(0xFF1A3A50), Color(0xFF4FC3F7), Color(0xFF29B6F6)),  // 浅蓝
        Triple(Color(0xFF3D3518), Color(0xFFFFD740), Color(0xFFFFC400)),  // 黄色
        Triple(Color(0xFF4A2020), Color(0xFFFF8A80), Color(0xFFEF9A9A)),  // 珊瑚
        Triple(Color(0xFF1E3A33), Color(0xFF80CBC4), Color(0xFF4DB6AC)),  // 薄荷
        Triple(Color(0xFF3B2050), Color(0xFFCE93D8), Color(0xFFBA68C8)),  // 薰衣草
        Triple(Color(0xFF4A2E1A), Color(0xFFFFAB91), Color(0xFFFF8A65)),  // 蜜桃
        Triple(Color(0xFF1A3D20), Color(0xFF66BB6A), Color(0xFF4CAF50)),  // 翡翠绿
        Triple(Color(0xFF142D50), Color(0xFF42A5F5), Color(0xFF2196F3)),  // 宝石蓝
        Triple(Color(0xFF4A1A1A), Color(0xFFEF9A9A), Color(0xFFE57373)),  // 樱红
        Triple(Color(0xFF4A3D1A), Color(0xFFFFCA28), Color(0xFFFFB300)),  // 金色
        Triple(Color(0xFF4A2030), Color(0xFFF48FB1), Color(0xFFEC407A)),  // 珊瑚粉
        Triple(Color(0xFF2A3D1F), Color(0xFFC5E1A5), Color(0xFFAED581)),  // 橄榄
        Triple(Color(0xFF1A3050), Color(0xFF4DD0E1), Color(0xFF26C6DA)),  // 天蓝
        Triple(Color(0xFF3D1528), Color(0xFFF06292), Color(0xFFEC407A)),  // 酒红
    )


fun getCoursePaletteIndex(courseName: String): Int {
    return courseName.hashCode().absoluteValue % courseColorPaletteLight.size
}

private val colorCache = mutableMapOf<Pair<String, Boolean>, CourseColor>()
private const val COLOR_CACHE_MAX = 128

private fun getCachedColors(courseName: String, darkMode: Boolean): CourseColor {
    val key = courseName to darkMode
    colorCache[key]?.let { return it }
    if (colorCache.size >= COLOR_CACHE_MAX) colorCache.clear()
    val palette = if (darkMode) courseColorPaletteDark else courseColorPaletteLight
    return palette[getCoursePaletteIndex(courseName)].also { colorCache[key] = it }
}

fun pickCourseColor(courseName: String, darkMode: Boolean): Color {
    return getCachedColors(courseName, darkMode).first
}

fun pickCourseTitleColor(courseName: String, darkMode: Boolean): Color {
    return getCachedColors(courseName, darkMode).second
}

fun pickCourseSubColor(courseName: String, darkMode: Boolean): Color {
    return getCachedColors(courseName, darkMode).third
}
