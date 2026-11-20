package restarhalf.stellar.schedule.domain.model

enum class CardRefractionLevel(
    val scale: Float,
    val label: String,
) {
    OFF(0f, "关闭"),
    WEAK(0.75f, "较弱"),
    DEFAULT(1f, "默认"),
    STRONG(1.25f, "较强"),
    EXTREME(1.5f, "最强");

    companion object {
        fun fromOrdinal(ordinal: Int): CardRefractionLevel =
            entries.getOrElse(ordinal) { DEFAULT }
    }
}

enum class CardEdgeThickness(
    val scale: Float,
    val label: String,
) {
    SMALLER(0.8f, "较小"),
    DEFAULT(1.0f, "默认"),
    LARGER(1.2f, "较大"),
    LARGEST(1.5f, "最大");

    companion object {
        fun fromOrdinal(ordinal: Int): CardEdgeThickness =
            entries.getOrElse(ordinal) { DEFAULT }
    }
}
