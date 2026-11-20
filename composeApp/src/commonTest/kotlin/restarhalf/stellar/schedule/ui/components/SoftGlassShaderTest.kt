package restarhalf.stellar.schedule.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SoftGlassShaderTest {

    @Test
    fun edgeSafeShaderReplacesAllContentSamplingPaths() {
        val shader = EDGE_SAFE_SOFT_GLASS_LIVE_SHADER
        assertTrue(shader.contains("float3 sampleContentOr(float2 p, float3 fallback)"))
        assertTrue(shader.contains("sum += sampleContentOr(p + float2(cos(a), sin(a)) * r, center);"))
        assertTrue(
            shader.contains(
                "refractionCol = sampleContentOr(fragCoord + refractDir.xy * refractThickness, centerSample.rgb);",
            ),
        )
        assertTrue(
            shader.contains(
                "        float3 reflectionCol = sampleContentOr(\n" +
                    "            fragCoord + refractXY * (uReflectOffsetPx * (1.0 - w)),\n" +
                    "            centerSample.rgb\n" +
                    "        );",
            ),
        )
        assertFalse(
            shader.contains("refractionCol = sampleContent(fragCoord + refractDir.xy * refractThickness);"),
        )
    }
}
