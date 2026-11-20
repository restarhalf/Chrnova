// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.anim

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import kotlin.math.pow

/**
 * Equivalent to the Android [DecelerateInterpolator](https://cs.android.com/search?q=file:androidx/core/animation/DecelerateInterpolator.java+class:androidx.core.animation.DecelerateInterpolator)
 *
 * @param factor Easing degree. 1.0f yields an upside-down y=x^2 parabola; higher
 *   values exaggerate the ease-out effect (starts faster, ends slower).
 */
@Immutable
class DecelerateEasing(
    private val factor: Float = 1.0f,
) : Easing {
    override fun transform(fraction: Float): Float = if (factor == 1.0f) {
        1.0f - (1.0f - fraction) * (1.0f - fraction)
    } else {
        1.0f - (1.0f - fraction).pow(2 * factor)
    }
}
