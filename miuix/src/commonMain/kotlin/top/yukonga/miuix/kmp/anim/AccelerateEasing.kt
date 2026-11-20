// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.anim

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import kotlin.math.pow

/**
 * Equivalent to the Android [AccelerateInterpolator](https://cs.android.com/search?q=file:androidx/core/animation/AccelerateInterpolator.java+class:androidx.core.animation.AccelerateInterpolator)
 *
 * @param factor Easing degree. 1.0f yields a y=x^2 parabola; higher values exaggerate
 *   the ease-in effect (starts slower, ends faster).
 */
@Immutable
class AccelerateEasing(
    private val factor: Float = 1.0f,
) : Easing {
    private val doubleFactor: Float = 2 * factor

    override fun transform(fraction: Float): Float = if (factor == 1.0f) {
        fraction * fraction
    } else {
        fraction.pow(doubleFactor)
    }
}
