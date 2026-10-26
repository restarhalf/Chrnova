package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

val TransparentStackTransition: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    scrim = { 0f },
) { scope ->
    val width = scope.layoutSize.width.toFloat()
    val d = scope.relativeDepth
    val rtl = scope.layoutDirection == LayoutDirection.Rtl
    if (d <= 0f) {

        translationX = ((if (rtl) -1f else 1f) * (-d).coerceIn(0f, 1f) * width).fastRoundToInt().toFloat()
    } else {

        val p = d.coerceIn(0f, 1f)
        translationX = (if (rtl) 1f else -1f) * p * width * 0.25f
        alpha = 1f - p
    }
}
