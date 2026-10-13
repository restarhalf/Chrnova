package restarhalf.stellar.schedule.ui.impl

import platform.UIKit.UIApplication
import platform.UIKit.UIScreen
import restarhalf.stellar.schedule.ui.port.ScreenTunerPort

class ScreenTunerPortImpl : ScreenTunerPort {

    private var previousBrightness: Double? = null

    override fun enterScanPresentation(): Boolean {
        val screen = UIScreen.mainScreen
        if (previousBrightness == null) {
            previousBrightness = screen.brightness
        }
        screen.brightness = 1.0
        UIApplication.sharedApplication.idleTimerDisabled = true
        return true
    }

    override fun exitScanPresentation() {
        previousBrightness?.let { UIScreen.mainScreen.brightness = it }
        previousBrightness = null
        UIApplication.sharedApplication.idleTimerDisabled = false
    }
}
