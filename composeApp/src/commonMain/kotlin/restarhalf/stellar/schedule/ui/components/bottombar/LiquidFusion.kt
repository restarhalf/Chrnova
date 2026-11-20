package restarhalf.stellar.schedule.ui.components.bottombar

data class LiquidFusion(
    val neckStrength: Float = 0.36f,
    val bulgeRangeDp: Float = 10f,
    val bulgeMaxDp: Float = 2.6f,
    val bulgeWidthRatio: Float = 10f,
    val bulgeMinAngle: Float = 0.35f,
    val bulgeMaxAngle: Float = 1.2f,
    val bulgeSigma: Float = 0.2f,
    val neckOverlapGrow: Float = 0.35f,
    val overlapLeadDp: Float = 4f,
    val rampPower: Float = 0.22f,
    val weldRadiusDp: Float = 2.5f,
) {
    companion object {
        val Default = LiquidFusion()
    }
}
