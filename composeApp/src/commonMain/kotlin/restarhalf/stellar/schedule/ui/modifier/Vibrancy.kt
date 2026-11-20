package restarhalf.stellar.schedule.ui.modifier

import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.effects.colorControls

fun GlassEffectScope.vibrancy() {
    colorControls(
        brightness = 0f,
        contrast = 1f,
        saturation = 1.5f,
    )
}
