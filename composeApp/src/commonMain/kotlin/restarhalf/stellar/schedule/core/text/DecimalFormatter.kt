package restarhalf.stellar.schedule.core.text

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

object DecimalFormatter {
    fun format(value: Float, decimals: Int): String = format(value.toDouble(), decimals)

    fun format(value: Double, decimals: Int): String {
        val safeDecimals = decimals.coerceAtLeast(0)
        if (safeDecimals == 0) return value.roundToInt().toString()

        var scale = 1
        repeat(safeDecimals) { scale *= 10 }

        val scaled = (value * scale).roundToInt()
        val sign = if (scaled < 0) "-" else ""
        val absScaled = scaled.absoluteValue
        val integerPart = absScaled / scale
        val fractionPart = absScaled % scale

        return buildString {
            append(sign)
            append(integerPart)
            append('.')
            append(fractionPart.toString().padStart(safeDecimals, '0'))
        }
    }
}
