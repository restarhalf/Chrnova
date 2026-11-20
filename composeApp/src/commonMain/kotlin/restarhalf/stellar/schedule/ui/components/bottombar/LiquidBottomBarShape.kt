package restarhalf.stellar.schedule.ui.components.bottombar

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.haooz.softglass.shape.Continuity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal val MaterialBleedX = 16.dp
internal val MaterialBleedY = 28.dp

internal val BottomBarSelectorInset = 0.5.dp

internal enum class LeftMorphMode {
    CollapseBurst,
    ExpandStream,
}

internal const val LEFT_BURST_DROPLET_COUNT = 8
internal val ExpandDropEasing = CubicBezierEasing(0.16f, 0.88f, 0.30f, 1.0f)
internal val ExpandBodyEasing = CubicBezierEasing(0.32f, 0.0f, 0.20f, 1.0f)

internal fun computeExpandFrontT(p: Float): Float =
    ExpandBodyEasing.transform(((p - 0.14f) / 0.78f).coerceIn(0f, 1f))

internal class BarNeckGeometry(
    val hasNeck: Boolean,
    val pu: Offset,
    val c1u: Offset,
    val c2u: Offset,
    val cu: Offset,
    val pl: Offset,
    val c1l: Offset,
    val c2l: Offset,
    val cl: Offset,
    val bulgeH: Float,
    val bulgeAngle: Float,
    val phiPill: Float,
    val phiCircle: Float,
    val gaussEdge: Float,
)

private fun fusionWrapAngle(delta: Float): Float =
    delta - 6.2831853f * floor(delta / 6.2831853f + 0.5f)

internal fun fusionBumpAt(
    angle: Float,
    center: Float,
    phi: Float,
    sigma: Float,
    gaussEdge: Float,
): Float {
    val t = (fusionWrapAngle(angle - center) + phi) / (2f * phi)
    if (t <= 0f || t >= 1f) return 0f
    val dx = t - 0.5f
    val rawG = exp(-(dx * dx) / (2f * sigma * sigma))
    val bell = ((rawG - gaussEdge) / (1f - gaussEdge)).coerceIn(0f, 1f)
    val u = 2f * t - 1f
    return bell * (1f - u * u * u * u)
}

private fun tangentAt(a: Float, toward: Offset): Offset {
    val tx = -sin(a)
    val ty = cos(a)
    val dot = tx * toward.x + ty * toward.y
    return if (dot >= 0f) Offset(tx, ty) else Offset(-tx, -ty)
}

internal fun barNeckGeometryPx(
    pillCenterX: Float,
    pillCenterY: Float,
    pillHalfWidth: Float,
    pillHalfHeight: Float,
    circleCenterX: Float,
    circleCenterY: Float,
    circleRadiusX: Float,
    circleRadiusY: Float,
    maxNeckHalfPx: Float,
    fusion: LiquidFusion,
    density: Density,
    pillRadiusX: Float? = null,
    pillRadiusY: Float? = null,
    ellipseTangents: Boolean = false,
): BarNeckGeometry? {
    val circlePillRadius = minOf(pillHalfWidth, pillHalfHeight)
    if (circlePillRadius <= 0f) return null
    val prx = pillRadiusX ?: circlePillRadius
    val pry = pillRadiusY ?: circlePillRadius
    val pillRadius = minOf(prx, pry)

    fun pillRAt(a: Float): Float =
        if (prx == pry) prx else ellipseRadiusAt(prx, pry, a)

    fun tangentOf(a: Float, rx: Float, ry: Float, toward: Offset): Offset {
        if (!ellipseTangents || rx == ry) return tangentAt(a, toward)
        val e = 0.002f
        val r0 = ellipseRadiusAt(rx, ry, a - e)
        val r1 = ellipseRadiusAt(rx, ry, a + e)
        val dx = r1 * cos(a + e) - r0 * cos(a - e)
        val dy = r1 * sin(a + e) - r0 * sin(a - e)
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-6f)
        val tx = dx / len
        val ty = dy / len
        val dot = tx * toward.x + ty * toward.y
        return Offset(if (dot >= 0f) tx else -tx, if (dot >= 0f) ty else -ty)
    }

    val axisLeft = pillCenterX - pillHalfWidth + prx
    val axisRight = pillCenterX + pillHalfWidth - prx
    val axisX = circleCenterX.coerceIn(minOf(axisLeft, axisRight), maxOf(axisLeft, axisRight))
    val vx = circleCenterX - axisX
    val vy = circleCenterY - pillCenterY
    val dist = sqrt(vx * vx + vy * vy)
    if (dist < 0.5f) return null
    val ang = atan2(vy, vx)
    val circleDirR = ellipseRadiusAt(circleRadiusX, circleRadiusY, ang)
    val surfaceGap = dist - pillRAt(ang) - circleDirR

    val neckThresholdPx = with(density) { (fusion.bulgeMaxDp * 2f).dp.toPx() }
    val bulgeRangePx = with(density) { fusion.bulgeRangeDp.dp.toPx() }
    val bulgeT = if (surfaceGap >= bulgeRangePx) {
        0f
    } else {
        ((bulgeRangePx - surfaceGap) / (bulgeRangePx - neckThresholdPx)).coerceIn(0f, 1f)
    }
    val bulgeH = with(density) { fusion.bulgeMaxDp.dp.toPx() } * (bulgeT * bulgeT)
    val gaussEdge = exp(-0.25f / (2f * fusion.bulgeSigma * fusion.bulgeSigma))
    val circleAvgR = (circleRadiusX + circleRadiusY) / 2f
    val phiPill = ((bulgeH * fusion.bulgeWidthRatio) / pry)
        .coerceIn(fusion.bulgeMinAngle, fusion.bulgeMaxAngle)
    val phiCircle = ((bulgeH * fusion.bulgeWidthRatio) / circleAvgR)
        .coerceIn(fusion.bulgeMinAngle, fusion.bulgeMaxAngle)
    val pi = PI.toFloat()

    fun bulgeOnly() = BarNeckGeometry(
        hasNeck = false,
        pu = Offset.Zero,
        c1u = Offset.Zero,
        c2u = Offset.Zero,
        cu = Offset.Zero,
        pl = Offset.Zero,
        c1l = Offset.Zero,
        c2l = Offset.Zero,
        cl = Offset.Zero,
        bulgeH = bulgeH,
        bulgeAngle = ang,
        phiPill = phiPill,
        phiCircle = phiCircle,
        gaussEdge = gaussEdge,
    )

    if (surfaceGap >= neckThresholdPx) return bulgeOnly()
    val k = ((neckThresholdPx - surfaceGap) / neckThresholdPx).coerceIn(0f, 1f).pow(fusion.rampPower)
    if (k <= 0.001f) return bulgeOnly()

    val overlapLeadPx = with(density) { fusion.overlapLeadDp.dp.toPx() }
    val effDist = (dist - overlapLeadPx).coerceAtLeast(1f)
    val intersectHh = if (effDist < prx + circleDirR) {
        val proj =
            (effDist * effDist + prx * prx - circleDirR * circleDirR) / (2f * effDist)
        sqrt((pry * pry - proj * proj).coerceAtLeast(0f))
    } else {
        0f
    }
    val neck = (maxNeckHalfPx * k + intersectHh * fusion.neckOverlapGrow).coerceAtMost(pillRadius)

    fun bumpAt(angle: Float, center: Float, phi: Float): Float {
        val t = (angle - (center - phi)) / (2f * phi)
        if (t <= 0f || t >= 1f) return 0f
        val dx = t - 0.5f
        val rawG = exp(-(dx * dx) / (2f * fusion.bulgeSigma * fusion.bulgeSigma))
        return ((rawG - gaussEdge) / (1f - gaussEdge)).coerceIn(0f, 1f)
    }

    fun onPill(a: Float): Offset {
        val r = pillRAt(a) + bulgeH * bumpAt(a, ang, phiPill)
        return Offset(axisX + r * cos(a), pillCenterY + r * sin(a))
    }

    fun onCircle(a: Float): Offset {
        val r = ellipseRadiusAt(circleRadiusX, circleRadiusY, a) +
                bulgeH * bumpAt(a, ang + pi, phiCircle)
        return Offset(circleCenterX + r * cos(a), circleCenterY + r * sin(a))
    }

    val phi1 = asin((neck / (pry + bulgeH)).coerceIn(-1f, 1f))
    val circleNearR = ellipseRadiusAt(circleRadiusX, circleRadiusY, ang + pi)
    val phi2 = asin((neck / (circleNearR + bulgeH).coerceAtLeast(0.5f)).coerceIn(-1f, 1f))

    val a1u = ang + phi1
    val a1l = ang - phi1
    val a2u = ang + pi - phi2
    val a2l = ang + pi + phi2
    val pu = onPill(a1u)
    val pl = onPill(a1l)
    val cu = onCircle(a2u)
    val cl = onCircle(a2l)

    val chordU = cu - pu
    val chordL = cl - pl
    val lenU = sqrt(chordU.x * chordU.x + chordU.y * chordU.y).coerceAtLeast(0.001f)
    val lenL = sqrt(chordL.x * chordL.x + chordL.y * chordL.y).coerceAtLeast(0.001f)
    val tu = tangentOf(a1u, prx, pry, chordU)
    val tcu = tangentOf(a2u, circleRadiusX, circleRadiusY, Offset(-chordU.x, -chordU.y))
    val tl = tangentOf(a1l, prx, pry, chordL)
    val tcl = tangentOf(a2l, circleRadiusX, circleRadiusY, Offset(-chordL.x, -chordL.y))

    return BarNeckGeometry(
        hasNeck = true,
        pu = pu,
        c1u = Offset(pu.x + tu.x * lenU / 3f, pu.y + tu.y * lenU / 3f),
        c2u = Offset(cu.x + tcu.x * lenU / 3f, cu.y + tcu.y * lenU / 3f),
        cu = cu,
        pl = pl,
        c1l = Offset(pl.x + tl.x * lenL / 3f, pl.y + tl.y * lenL / 3f),
        c2l = Offset(cl.x + tcl.x * lenL / 3f, cl.y + tcl.y * lenL / 3f),
        cl = cl,
        bulgeH = bulgeH,
        bulgeAngle = ang,
        phiPill = phiPill,
        phiCircle = phiCircle,
        gaussEdge = gaussEdge,
    )
}

internal fun ellipseRadiusAt(rx: Float, ry: Float, a: Float): Float {
    val c = cos(a)
    val s = sin(a)
    val d = (ry * c) * (ry * c) + (rx * s) * (rx * s)
    return if (d <= 1e-6f) 0f else rx * ry / sqrt(d)
}

internal const val BottomBarRefractionShader = """
uniform shader content;

uniform float2 offset;
uniform float2 pillCenter;
uniform float2 pillHalf;
uniform float pillRadius;
uniform float2 circleCenter;
uniform float2 circleRadii;
uniform float2 pu;
uniform float2 c1u;
uniform float2 c2u;
uniform float2 cu;
uniform float2 pl;
uniform float2 c1l;
uniform float2 c2l;
uniform float2 cl;
uniform float neckRadius;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float bulgeH;
uniform float bulgeAngle;
uniform float bulgePhiPill;
uniform float bulgePhiCircle;
uniform float bulgeGaussEdge;
uniform float bulgeSigma;
uniform float morphMode;
uniform float4 drop0;
uniform float4 drop1;
uniform float4 drop2;
uniform float4 drop3;
uniform float4 drop4;
uniform float4 drop5;
uniform float4 drop6;
uniform float4 drop7;
uniform float dropSmoothK;

float sdRoundRect(float2 p, float2 c, float2 h, float r) {
    float2 q = abs(p - c) - (h - r);
    return length(max(q, float2(0.0, 0.0))) + min(max(q.x, q.y), 0.0) - r;
}

float bumpAt(float angle, float center, float phi) {
    float da = angle - center;
    da = da - 6.2831853 * floor(da / 6.2831853 + 0.5);
    float t = (da + phi) / (2.0 * phi);
    if (t <= 0.0 || t >= 1.0) {
        return 0.0;
    }
    float dx = t - 0.5;
    float rawG = exp(-(dx * dx) / (2.0 * bulgeSigma * bulgeSigma));
    return clamp((rawG - bulgeGaussEdge) / (1.0 - bulgeGaussEdge), 0.0, 1.0);
}

float sdEllipse(float2 p, float2 c, float2 ab) {
    float2 d = p - c;
    float t = atan(d.y / max(ab.y, 1e-4), d.x / max(ab.x, 1e-4));
    for (int i = 0; i < 4; i++) {
        float ct = cos(t);
        float st = sin(t);
        float2 q = float2(ab.x * ct, ab.y * st);
        float2 dq = float2(-ab.x * st, ab.y * ct);
        float2 d2q = float2(-ab.x * ct, -ab.y * st);
        float f = dot(q - d, dq);
        float df = dot(dq, dq) + dot(q - d, d2q);
        t = t - f / (abs(df) > 1e-4 ? df : 1e-4);
    }
    float2 q = float2(ab.x * cos(t), ab.y * sin(t));
    float dist = length(d - q);
    return (length(d / ab) < 1.0) ? -dist : dist;
}

float2 cBez(float2 a, float2 b, float2 c, float2 d, float t) {
    float mt = 1.0 - t;
    float mt2 = mt * mt;
    float t2 = t * t;
    return mt2 * mt * a + (3.0 * mt2 * t) * b + (3.0 * mt * t2) * c + t2 * t * d;
}

float2 dBez(float2 a, float2 b, float2 c, float2 d, float t) {
    float mt = 1.0 - t;
    return 3.0 * mt * mt * (b - a) + 6.0 * mt * t * (c - b) + 3.0 * t * t * (d - c);
}

float2 d2Bez(float2 a, float2 b, float2 c, float2 d, float t) {
    float mt = 1.0 - t;
    return 6.0 * mt * (c - 2.0 * b + a) + 6.0 * t * (d - 2.0 * c + b);
}

float nearestT(float2 p, float2 a, float2 b, float2 c, float2 d) {
    float bestD = 1e9;
    float bestT = 0.0;
    for (int k = 0; k < 2; k++) {
        float t = (k == 0) ? 0.25 : 0.75;
        for (int i = 0; i < 5; i++) {
            float2 q = cBez(a, b, c, d, t);
            float2 dq = dBez(a, b, c, d, t);
            float2 d2q = d2Bez(a, b, c, d, t);
            float f = dot(q - p, dq);
            float df = dot(dq, dq) + dot(q - p, d2q);
            t = clamp(t - f / (abs(df) > 1e-4 ? df : 1e-4), 0.0, 1.0);
        }
        float dd = length(cBez(a, b, c, d, t) - p);
        if (dd < bestD) {
            bestD = dd;
            bestT = t;
        }
    }
    return bestT;
}

float sdNeck(float2 p) {
    float tu = nearestT(p, pu, c1u, c2u, cu);
    float tl = nearestT(p, pl, c1l, c2l, cl);
    float2 qu = cBez(pu, c1u, c2u, cu, tu);
    float2 ql = cBez(pl, c1l, c2l, cl, tl);

    float2 du = dBez(pu, c1u, c2u, cu, tu);
    float2 dl = dBez(pl, c1l, c2l, cl, tl);
    if (tu < 0.02 || tu > 0.98) {
        du = cu - pu;
    }
    if (tl < 0.02 || tl > 0.98) {
        dl = cl - pl;
    }
    float sU = (du.x * (p.y - qu.y) - du.y * (p.x - qu.x)) / max(length(du), 1e-4);
    float sL = (dl.x * (p.y - ql.y) - dl.y * (p.x - ql.x)) / max(length(dl), 1e-4);

    float2 ax = cu - pu;
    ax = ax / max(length(ax), 1e-4);
    float dLong = max(-dot(p - pu, ax), dot(p - cu, ax));
    return max(sU, -sL) + max(0.0, dLong);
}

float sminSoft(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float accumulateDrop(float d, float4 drop, float2 p, float k) {
    if (drop.z > 0.5) {
        float di = length(p - drop.xy) - drop.z;
        return (d > 1e8) ? di : (k > 0.01 ? sminSoft(d, di, k) : min(d, di));
    }
    return d;
}

float sdDrops(float2 p) {
    float d = 1e9;
    d = accumulateDrop(d, drop0, p, dropSmoothK);
    d = accumulateDrop(d, drop1, p, dropSmoothK);
    d = accumulateDrop(d, drop2, p, dropSmoothK);
    d = accumulateDrop(d, drop3, p, dropSmoothK);
    d = accumulateDrop(d, drop4, p, dropSmoothK);
    d = accumulateDrop(d, drop5, p, dropSmoothK);
    d = accumulateDrop(d, drop6, p, dropSmoothK);
    d = accumulateDrop(d, drop7, p, dropSmoothK);
    return d;
}

float sdExpandMetaball(float2 p) {
    float2 q = p - pillCenter;
    float axisHalf = max(pillHalf.x - pillRadius, 0.0);
    float2 nearest = float2(clamp(q.x, -axisHalf, axisHalf), 0.0);
    float d = length(q - nearest) - pillRadius;
    d = accumulateDrop(d, drop0, p, dropSmoothK);
    d = accumulateDrop(d, drop1, p, dropSmoothK);
    d = accumulateDrop(d, drop2, p, dropSmoothK);
    return d;
}

float bodySdf(float2 p) {
    float dPill;
    if (morphMode > 1.5) {
        dPill = sdExpandMetaball(p);
    } else if (morphMode > 0.5) {
        dPill = sdDrops(p);
    } else {
        float2 q = p - pillCenter;
        float axisHalf = max(pillHalf.x - pillRadius, 0.0);
        float2 nearest = float2(clamp(q.x, -axisHalf, axisHalf), 0.0);
        float2 d = q - nearest;
        float distP = length(d);
        float rEff = pillRadius;
        if (bulgeH > 0.01 && distP > 1e-4) {
            rEff = pillRadius + bulgeH * bumpAt(atan(d.y, d.x), bulgeAngle, bulgePhiPill);
        }
        dPill = distP - rEff;
    }

    float dCirc = sdEllipse(p, circleCenter, circleRadii);
    if (bulgeH > 0.01) {
        float2 dc = p - circleCenter;
        dCirc = dCirc - bulgeH * bumpAt(atan(dc.y, dc.x), bulgeAngle + 3.14159265, bulgePhiCircle);
    }
    return min(dPill, dCirc);
}

float sceneSdf(float2 p) {
    float d = bodySdf(p);
    if (neckRadius > 0.001) {
        d = min(d, sdNeck(p));
    }
    return d;
}

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 p = coord + offset;
    float sd = sceneSdf(p);
    float rh = refractionHeight;

    if (-sd >= rh) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    float d = circleMap(1.0 - -sd / rh) * refractionAmount;

    float eps = 0.75;
    float2 g = float2(
        sceneSdf(p + float2(eps, 0.0)) - sceneSdf(p - float2(eps, 0.0)),
        sceneSdf(p + float2(0.0, eps)) - sceneSdf(p - float2(0.0, eps))
    );
    float gl = length(g);
    float2 grad = gl > 0.0001 ? g / gl : float2(0.0, 0.0);
    return content.eval(coord + d * grad);
}
"""

internal class BurstDropletState(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val vx: Float,
    val vy: Float,
    val stretch: Float,
    val active: Boolean,
)

internal class LeftMorphGeometry(
    val mode: LeftMorphMode,
    val leftEdge: Float,
    val rightEdge: Float,
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val orbCx: Float,
    val orbCy: Float,
    val orbRadius: Float,
    val isFullyExpanded: Boolean,
    val isFullyCollapsed: Boolean,
    val isBreakupPhase: Boolean,
    val breakupProgress: Float,
    val seedSettled: Boolean,
    val dropSmoothKPx: Float,
    val droplets: Array<BurstDropletState>,
    val streamLeftX: Float,
    val streamFrontX: Float,
    val streamProgress: Float = 0f,
)

private val EMPTY_DROPLETS = Array(LEFT_BURST_DROPLET_COUNT) {
    BurstDropletState(0f, 0f, 0f, 0f, 0f, 0f, false)
}

private val CORE_U_SPAWN = floatArrayOf(0.00f, 0.25f, 0.50f, 0.75f, 1.00f)
private val CORE_R_RATIO = floatArrayOf(0.48f, 0.38f, 0.44f, 0.36f, 0.42f)
private val CORE_BURST_DX_R = floatArrayOf(-0.14f, -0.05f, 0.06f, 0.14f, 0.24f)
private val CORE_BURST_DY_R = floatArrayOf(-0.28f, 0.46f, -0.50f, 0.42f, -0.34f)
private val CORE_T_PEAK = floatArrayOf(0.22f, 0.25f, 0.27f, 0.28f, 0.30f)
private val CORE_T_ARRIVE = floatArrayOf(0.46f, 0.62f, 0.74f, 0.87f, 0.94f)

private val SAT_U_SPAWN = floatArrayOf(0.125f, 0.625f, 0.875f)
private val SAT_R_RATIO = floatArrayOf(0.22f, 0.19f, 0.21f)
private val SAT_BURST_DX_R = floatArrayOf(-0.10f, 0.10f, 0.30f)
private val SAT_BURST_DY_R = floatArrayOf(0.68f, -0.72f, 0.62f)
private val SAT_T_PEAK = floatArrayOf(0.26f, 0.29f, 0.31f)
private val SAT_T_ARRIVE = floatArrayOf(0.56f, 0.82f, 0.98f)
private val SAT_LEFT_CORE = intArrayOf(0, 2, 3)

// 展开态 Metaball 三水珠（由大到小）：目标落位、半径比、起飞与吸合时间窗、轻微上下有机起伏
private const val EXPAND_DROPLET_COUNT = 3
private val EXPAND_DROP_TARGET_U = floatArrayOf(0.44f, 0.74f, 0.98f)
private val EXPAND_DROP_R_RATIO = floatArrayOf(0.86f, 0.70f, 0.54f)
private val EXPAND_DROP_LAUNCH_START = floatArrayOf(0.09f, 0.15f, 0.21f)
private val EXPAND_DROP_LAUNCH_END = floatArrayOf(0.52f, 0.63f, 0.73f)
private val EXPAND_DROP_ABSORB_START = floatArrayOf(0.44f, 0.58f, 0.72f)
private val EXPAND_DROP_ABSORB_END = floatArrayOf(0.68f, 0.80f, 0.92f)
private val EXPAND_DROP_BOB_SIGN = floatArrayOf(-1f, 1f, -0.75f)

private const val DROP_NORM_TOTAL_SQ = 1.0030f
private const val BREAKUP_END_T = 0.14f
private const val SEED_SETTLE_T = 0.46f

internal fun morphSmoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val span = (edge1 - edge0).coerceAtLeast(1e-5f)
    val t = ((x - edge0) / span).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun computeLeftMorphGeometry(
    leftX: Float,
    centerY: Float,
    pillWidth: Float,
    height: Float,
    pressScale: Float,
    panelOffset: Float,
    collapseProgress: Float,
    jellyRadiusDeltaPx: Float,
    morphMode: LeftMorphMode,
    density: Density,
): LeftMorphGeometry {
    val rawP = collapseProgress
    val p = rawP.coerceIn(0f, 1f)
    val baseRadius = (height * 0.5f).coerceAtLeast(1f)

    if (p >= 0.998f) {
        val extraStretchX = if (rawP > 1f) 1f + (rawP - 1f) * 0.45f else 1f
        val jellyStretchX = jellyRadiusDeltaPx * 1.35f
        val jellySquashY = -jellyRadiusDeltaPx * 0.35f
        val halfW = (pillWidth * 0.5f * pressScale * extraStretchX + jellyStretchX).coerceAtLeast(baseRadius)
        val halfH = (height * 0.5f * pressScale + jellySquashY).coerceAtLeast(baseRadius * 0.85f)
        val cx = leftX + pillWidth * 0.5f + panelOffset + jellyStretchX * 0.45f
        val leftEdge = cx - halfW
        val rightEdge = cx + halfW
        return LeftMorphGeometry(
            mode = morphMode,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            centerX = cx,
            centerY = centerY,
            halfWidth = halfW,
            halfHeight = halfH,
            orbCx = cx - (halfW - halfH).coerceAtLeast(0f),
            orbCy = centerY,
            orbRadius = halfH,
            isFullyExpanded = true,
            isFullyCollapsed = false,
            isBreakupPhase = false,
            breakupProgress = 0f,
            seedSettled = false,
            dropSmoothKPx = 0f,
            droplets = EMPTY_DROPLETS,
            streamLeftX = leftEdge,
            streamFrontX = rightEdge,
            streamProgress = 1f,
        )
    }

    if (p <= 0.002f) {
        val orbCx = leftX + baseRadius
        val orbR = (baseRadius + jellyRadiusDeltaPx).coerceAtLeast(1f)
        return LeftMorphGeometry(
            mode = morphMode,
            leftEdge = orbCx - orbR,
            rightEdge = orbCx + orbR,
            centerX = orbCx,
            centerY = centerY,
            halfWidth = orbR,
            halfHeight = orbR,
            orbCx = orbCx,
            orbCy = centerY,
            orbRadius = orbR,
            isFullyExpanded = false,
            isFullyCollapsed = true,
            isBreakupPhase = false,
            breakupProgress = 0f,
            seedSettled = true,
            dropSmoothKPx = 0f,
            droplets = EMPTY_DROPLETS,
            streamLeftX = leftX,
            streamFrontX = leftX + orbR * 2f,
            streamProgress = 0f,
        )
    }

    val s = lerp(1f, pressScale, p)
    val po = panelOffset * p
    val effLeftX = leftX + po + pillWidth * 0.5f * (1f - s)
    val effPillW = (pillWidth * s).coerceAtLeast(height * s)
    val effR = baseRadius * s

    if (morphMode == LeftMorphMode.ExpandStream) {
        val orbX = effLeftX + effR
        val axisSpan = (effPillW - 2f * effR).coerceAtLeast(0f)

        // 1. 左球微微蓄力（p: 0.00 -> 0.14）：横向微收、纵向微鼓
        val charge = morphSmoothstep(0.00f, 0.08f, p) * (1f - morphSmoothstep(0.08f, 0.18f, p))
        val chargeSquashX = -with(density) { 2.4.dp.toPx() } * charge
        val chargeStretchY = with(density) { 1.5.dp.toPx() } * charge

        // 2. 主胶囊由左向右延展并依次吞并 3 颗水珠（p: 0.14 -> 0.94）
        val bodyT = ExpandBodyEasing.transform(((p - 0.14f) / 0.80f).coerceIn(0f, 1f))
        val jellyStretchX = jellyRadiusDeltaPx * 1.35f
        val jellySquashY = -jellyRadiusDeltaPx * 0.35f
        val halfW = (lerp(effR, effPillW * 0.5f, bodyT) + chargeSquashX + jellyStretchX)
            .coerceAtLeast(effR * 0.88f)
        val halfH = (effR + chargeStretchY + jellySquashY)
            .coerceAtLeast(effR * 0.85f)
        val bodyCenterX = effLeftX + halfW
        val bodyRightX = effLeftX + halfW * 2f

        // 3. 向右「甩」出 3 颗由大到小的液态玻璃水珠，滚动到位的同时被胶囊依次吸合
        val maxBobPx = with(density) { 2.0.dp.toPx() }
        val pi = PI.toFloat()
        var maxRightX = bodyRightX
        val drops = Array(LEFT_BURST_DROPLET_COUNT) { i ->
            if (i < EXPAND_DROPLET_COUNT && axisSpan >= effR * 0.4f) {
                val launchStart = EXPAND_DROP_LAUNCH_START[i]
                val launchEnd = EXPAND_DROP_LAUNCH_END[i]
                val absorbStart = EXPAND_DROP_ABSORB_START[i]
                val absorbEnd = EXPAND_DROP_ABSORB_END[i]

                val rawFly = ((p - launchStart) / (launchEnd - launchStart)).coerceIn(0f, 1f)
                val flyT = ExpandDropEasing.transform(rawFly)
                val absorbT = morphSmoothstep(absorbStart, absorbEnd, p)

                val startX = orbX + effR * (0.14f + i * 0.10f)
                val endX = orbX + axisSpan * EXPAND_DROP_TARGET_U[i]
                val cx = lerp(startX, endX, flyT)

                val bobEnv = sin(pi * flyT) * (1f - absorbT)
                val cy = centerY + EXPAND_DROP_BOB_SIGN[i] * maxBobPx * bobEnv

                val spawnScale = morphSmoothstep(launchStart - 0.02f, launchStart + 0.07f, p)
                val mergeFade = 1f - absorbT
                val radius = effR * EXPAND_DROP_R_RATIO[i] * spawnScale * mergeFade

                val vx = (endX - startX) * (1f - rawFly).coerceAtLeast(0.05f)
                val vy = EXPAND_DROP_BOB_SIGN[i] * maxBobPx * cos(pi * flyT) * pi
                val stretch = (sin(pi * flyT) * 0.16f * (1f - absorbT)).coerceIn(0f, 0.18f)
                val active = radius > 0.5f && p >= launchStart - 0.02f && absorbT < 0.995f
                if (active) {
                    maxRightX = maxOf(maxRightX, cx + radius)
                }
                BurstDropletState(cx, cy, if (active) radius else 0f, vx, vy, stretch, active)
            } else {
                BurstDropletState(0f, 0f, 0f, 0f, 0f, 0f, false)
            }
        }

        // 4. 表面张力黏连桥强度：甩出拉丝与吸合阶段最强，融合成完整胶囊后平滑归零
        val bridgeEnv = morphSmoothstep(0.08f, 0.22f, p) * (1f - morphSmoothstep(0.72f, 0.94f, p))
        val dropSmoothKPx = with(density) { 15.dp.toPx() } * bridgeEnv

        return LeftMorphGeometry(
            mode = LeftMorphMode.ExpandStream,
            leftEdge = effLeftX,
            rightEdge = maxRightX,
            centerX = bodyCenterX,
            centerY = centerY,
            halfWidth = halfW,
            halfHeight = halfH,
            orbCx = orbX,
            orbCy = centerY,
            orbRadius = halfH,
            isFullyExpanded = false,
            isFullyCollapsed = false,
            isBreakupPhase = false,
            breakupProgress = 0f,
            seedSettled = false,
            dropSmoothKPx = dropSmoothKPx,
            droplets = drops,
            streamLeftX = effLeftX,
            streamFrontX = maxRightX,
            streamProgress = p,
        )
    }

    // CollapseBurst
    val t = (1f - p).coerceIn(0f, 1f)
    val orbX = effLeftX + effR
    val axisSpan = (effPillW - 2f * effR).coerceAtLeast(0f)

    if (axisSpan < effR * 0.4f) {
        val singleR = (effR + jellyRadiusDeltaPx).coerceAtLeast(1f)
        val drops = Array(LEFT_BURST_DROPLET_COUNT) { idx ->
            if (idx == 0) {
                BurstDropletState(orbX, centerY, singleR, 0f, 0f, 0f, true)
            } else {
                BurstDropletState(0f, 0f, 0f, 0f, 0f, 0f, false)
            }
        }
        return LeftMorphGeometry(
            mode = LeftMorphMode.CollapseBurst,
            leftEdge = orbX - singleR,
            rightEdge = orbX + singleR,
            centerX = orbX,
            centerY = centerY,
            halfWidth = singleR,
            halfHeight = singleR,
            orbCx = orbX,
            orbCy = centerY,
            orbRadius = singleR,
            isFullyExpanded = false,
            isFullyCollapsed = false,
            isBreakupPhase = false,
            breakupProgress = 0f,
            seedSettled = true,
            dropSmoothKPx = 0f,
            droplets = drops,
            streamLeftX = effLeftX,
            streamFrontX = orbX + singleR,
            streamProgress = p,
        )
    }

    val isBreakup = t < BREAKUP_END_T
    val pBreak = if (isBreakup) (t / BREAKUP_END_T).coerceIn(0f, 1f) else 1f
    val sBreak = pBreak * pBreak * (3f - 2f * pBreak)
    val pi = PI.toFloat()

    if (isBreakup) {
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        val drops = Array(LEFT_BURST_DROPLET_COUNT) { i ->
            if (i < 5) {
                val xStart = orbX + CORE_U_SPAWN[i] * axisSpan
                val dx = CORE_BURST_DX_R[i] * effR
                val dy = CORE_BURST_DY_R[i] * effR
                val cx = xStart + 0.32f * dx * sBreak
                val cy = centerY + 0.32f * dy * sBreak
                val r = lerp(effR, CORE_R_RATIO[i] * effR, sBreak)
                minX = minOf(minX, cx - r)
                maxX = maxOf(maxX, cx + r)
                minY = minOf(minY, cy - r)
                maxY = maxOf(maxY, cy + r)
                BurstDropletState(cx, cy, r, dx, dy, 0f, true)
            } else {
                BurstDropletState(0f, 0f, 0f, 0f, 0f, 0f, false)
            }
        }
        val smoothK = lerp(effR * 0.78f, effR * 0.22f, pBreak)
        return LeftMorphGeometry(
            mode = LeftMorphMode.CollapseBurst,
            leftEdge = minX,
            rightEdge = maxX,
            centerX = (minX + maxX) * 0.5f,
            centerY = centerY,
            halfWidth = ((maxX - minX) * 0.5f).coerceAtLeast(effR),
            halfHeight = ((maxY - minY) * 0.5f).coerceAtLeast(effR),
            orbCx = orbX,
            orbCy = centerY,
            orbRadius = drops[0].radius,
            isFullyExpanded = false,
            isFullyCollapsed = false,
            isBreakupPhase = true,
            breakupProgress = pBreak,
            seedSettled = false,
            dropSmoothKPx = smoothK,
            droplets = drops,
            streamLeftX = effLeftX,
            streamFrontX = maxX,
            streamProgress = p,
        )
    }

    val seedSettled = t >= SEED_SETTLE_T
    var orbRadius = effR * CORE_R_RATIO[0]
    if (seedSettled) {
        var totalRatioSq = CORE_R_RATIO[0] * CORE_R_RATIO[0]
        var swellPulse = 0f
        for (i in 1..4) {
            val absorb = morphSmoothstep(CORE_T_ARRIVE[i] - 0.08f, CORE_T_ARRIVE[i], t)
            totalRatioSq += CORE_R_RATIO[i] * CORE_R_RATIO[i] * absorb
            swellPulse += effR * 0.025f * sin(pi * absorb)
        }
        for (si in 0..2) {
            val absorb = morphSmoothstep(SAT_T_ARRIVE[si] - 0.08f, SAT_T_ARRIVE[si], t)
            totalRatioSq += SAT_R_RATIO[si] * SAT_R_RATIO[si] * absorb
            swellPulse += effR * 0.025f * sin(pi * absorb)
        }
        orbRadius =
            effR * sqrt((totalRatioSq / DROP_NORM_TOTAL_SQ).coerceIn(0.18f, 1f)) +
                    swellPulse +
                    jellyRadiusDeltaPx
    }

    var minX = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    val drops = Array(LEFT_BURST_DROPLET_COUNT) { i ->
        val isCore = i < 5
        val rBurst: Float
        val spawnX: Float
        val spawnY: Float
        val peakX: Float
        val peakY: Float
        val tPeak: Float
        val tArrive: Float
        val ratio: Float

        if (isCore) {
            ratio = CORE_R_RATIO[i]
            rBurst = ratio * effR
            val xStart = orbX + CORE_U_SPAWN[i] * axisSpan
            val bdx = CORE_BURST_DX_R[i] * effR
            val bdy = CORE_BURST_DY_R[i] * effR
            spawnX = xStart + 0.32f * bdx
            spawnY = centerY + 0.32f * bdy
            peakX = xStart + bdx
            peakY = centerY + bdy
            tPeak = CORE_T_PEAK[i]
            tArrive = CORE_T_ARRIVE[i]
        } else {
            val si = i - 5
            val leftCore = SAT_LEFT_CORE[si]
            val rightCore = leftCore + 1
            val xA = orbX + CORE_U_SPAWN[leftCore] * axisSpan + 0.32f * CORE_BURST_DX_R[leftCore] * effR
            val xB = orbX + CORE_U_SPAWN[rightCore] * axisSpan + 0.32f * CORE_BURST_DX_R[rightCore] * effR
            val yA = 0.32f * CORE_BURST_DY_R[leftCore] * effR
            val yB = 0.32f * CORE_BURST_DY_R[rightCore] * effR
            ratio = SAT_R_RATIO[si]
            rBurst = ratio * effR
            spawnX = (xA + xB) * 0.5f
            spawnY = centerY + (yA + yB) * 0.5f
            val xTarget = orbX + SAT_U_SPAWN[si] * axisSpan
            peakX = xTarget + SAT_BURST_DX_R[si] * effR
            peakY = centerY + SAT_BURST_DY_R[si] * effR
            tPeak = SAT_T_PEAK[si]
            tArrive = SAT_T_ARRIVE[si]
        }

        val droplet = if (i == 0 && seedSettled) {
            BurstDropletState(orbX, centerY, orbRadius, 0f, 0f, 0f, true)
        } else if (t >= tArrive) {
            BurstDropletState(orbX, centerY, 0f, 0f, 0f, 0f, false)
        } else if (t <= tPeak) {
            val u = ((t - BREAKUP_END_T) / (tPeak - BREAKUP_END_T)).coerceIn(0f, 1f)
            val f = 1f - (1f - u) * (1f - u)
            val cx = lerp(spawnX, peakX, f)
            val cy = lerp(spawnY, peakY, f)
            val vx = peakX - spawnX
            val vy = peakY - spawnY
            val r = if (isCore) {
                rBurst
            } else {
                lerp(effR * 0.09f, rBurst, morphSmoothstep(0f, 0.35f, u))
            }
            val stretch = sin(pi * u) * 0.16f
            BurstDropletState(cx, cy, r, vx, vy, stretch, true)
        } else {
            val u = ((t - tPeak) / (tArrive - tPeak)).coerceIn(0f, 1f)
            val uEased = (u * u * (2.2f - 1.2f * u)).coerceIn(0f, 1f)
            val omt = 1f - uEased
            val p1x = lerp(peakX, orbX, 0.42f)
            val p1y = peakY * 1.18f - centerY * 0.18f
            val cx = omt * omt * peakX + 2f * omt * uEased * p1x + uEased * uEased * orbX
            val cy = omt * omt * peakY + 2f * omt * uEased * p1y + uEased * uEased * centerY
            val vx = 2f * omt * (p1x - peakX) + 2f * uEased * (orbX - p1x)
            val vy = 2f * omt * (p1y - peakY) + 2f * uEased * (centerY - p1y)
            val startDist = hypot(peakX - orbX, peakY - centerY)
            val spanWeight = (startDist / (effPillW * 0.7f)).coerceIn(0.25f, 1f)
            val taperOut = 1f - morphSmoothstep(0.70f, 0.94f, u)
            val stretch = (sin(pi * uEased) * 0.32f * spanWeight * taperOut).coerceIn(0f, 0.34f)
            val distToOrb = hypot(cx - orbX, cy - centerY)
            val swallowed = seedSettled && distToOrb <= (orbRadius - rBurst * 0.4f).coerceAtLeast(0f)
            BurstDropletState(cx, cy, if (swallowed) 0f else rBurst, vx, vy, stretch, !swallowed)
        }

        if (droplet.active && droplet.radius > 0f) {
            minX = minOf(minX, droplet.cx - droplet.radius)
            maxX = maxOf(maxX, droplet.cx + droplet.radius)
            minY = minOf(minY, droplet.cy - droplet.radius)
            maxY = maxOf(maxY, droplet.cy + droplet.radius)
        }
        droplet
    }

    if (minX.isInfinite()) {
        minX = orbX - orbRadius
        maxX = orbX + orbRadius
        minY = centerY - orbRadius
        maxY = centerY + orbRadius
    }

    return LeftMorphGeometry(
        mode = LeftMorphMode.CollapseBurst,
        leftEdge = minX,
        rightEdge = maxX,
        centerX = (minX + maxX) * 0.5f,
        centerY = centerY,
        halfWidth = ((maxX - minX) * 0.5f).coerceAtLeast(effR * 0.4f),
        halfHeight = ((maxY - minY) * 0.5f).coerceAtLeast(effR * 0.4f),
        orbCx = orbX,
        orbCy = centerY,
        orbRadius = orbRadius,
        isFullyExpanded = false,
        isFullyCollapsed = false,
        isBreakupPhase = false,
        breakupProgress = 1f,
        seedSettled = seedSettled,
        dropSmoothKPx = with(density) { 9.dp.toPx() },
        droplets = drops,
        streamLeftX = effLeftX,
        streamFrontX = maxX,
        streamProgress = p,
    )
}

private fun Path.addTeardropOrOval(
    cx: Float,
    cy: Float,
    r: Float,
    vx: Float,
    vy: Float,
    stretch: Float,
) {
    if (r <= 0.1f) return
    val speed = hypot(vx, vy)
    if (stretch < 0.015f || speed < 1e-3f) {
        addOval(Rect(cx - r, cy - r, cx + r, cy + r))
        return
    }
    val tx = vx / speed
    val ty = vy / speed
    val nx = -ty
    val ny = tx
    val rxFront = r * (1f + stretch * 0.85f)
    val rxBack = r * (1f + stretch * 1.15f)
    val ry = r / sqrt(1f + stretch)
    val kappa = 0.5522848f

    fun px(u: Float, v: Float) = cx + tx * u + nx * v
    fun py(u: Float, v: Float) = cy + ty * u + ny * v

    moveTo(px(rxFront, 0f), py(rxFront, 0f))
    cubicTo(
        px(rxFront, ry * kappa), py(rxFront, ry * kappa),
        px(rxFront * kappa, ry), py(rxFront * kappa, ry),
        px(0f, ry), py(0f, ry),
    )
    cubicTo(
        px(-rxBack * kappa, ry), py(-rxBack * kappa, ry),
        px(-rxBack, ry * kappa), py(-rxBack, ry * kappa),
        px(-rxBack, 0f), py(-rxBack, 0f),
    )
    cubicTo(
        px(-rxBack, -ry * kappa), py(-rxBack, -ry * kappa),
        px(-rxBack * kappa, -ry), py(-rxBack * kappa, -ry),
        px(0f, -ry), py(0f, -ry),
    )
    cubicTo(
        px(rxFront * kappa, -ry), py(rxFront * kappa, -ry),
        px(rxFront, -ry * kappa), py(rxFront, -ry * kappa),
        px(rxFront, 0f), py(rxFront, 0f),
    )
    close()
}

private fun buildBurstBreakupPath(geom: LeftMorphGeometry): Path {
    val drops = geom.droplets
    val pinch = geom.breakupProgress.coerceIn(0f, 1f)
    val pinchWaistT = pinch.pow(1.15f)
    val maxSinBeta = sin(0.88f)
    val radToDeg = 180f / PI.toFloat()

    val betaL = FloatArray(5)
    val betaR = FloatArray(5)
    for (j in 0 until 4) {
        val dA = drops[j]
        val dB = drops[j + 1]
        val dx = (dB.cx - dA.cx).coerceAtLeast(1f)
        val sinR = ((dx * 0.36f) / dA.radius.coerceAtLeast(1f)).coerceIn(0f, maxSinBeta) * pinch
        val sinL = ((dx * 0.36f) / dB.radius.coerceAtLeast(1f)).coerceIn(0f, maxSinBeta) * pinch
        betaR[j] = asin(sinR)
        betaL[j + 1] = asin(sinL)
    }

    return Path().apply {
        val d0 = drops[0]
        val b0Deg = betaR[0] * radToDeg
        arcTo(
            rect = Rect(d0.cx - d0.radius, d0.cy - d0.radius, d0.cx + d0.radius, d0.cy + d0.radius),
            startAngleDegrees = 90f - b0Deg,
            sweepAngleDegrees = 180f + 2f * b0Deg,
            forceMoveTo = true,
        )

        for (j in 0 until 4) {
            val dA = drops[j]
            val dB = drops[j + 1]
            val dx = dB.cx - dA.cx
            val dy = dB.cy - dA.cy
            val axLen = hypot(dx, dy).coerceAtLeast(1e-4f)
            val ux = dx / axLen
            val uy = dy / axLen

            val bR = betaR[j]
            val bL = betaL[j + 1]
            val p0x = dA.cx + dA.radius * sin(bR)
            val p0y = dA.cy - dA.radius * cos(bR)
            val p3x = dB.cx - dB.radius * sin(bL)
            val p3y = dB.cy - dB.radius * cos(bL)

            val minR = minOf(dA.radius, dB.radius)
            val waist = lerp(minR, minR * 0.14f, pinchWaistT)
            val vtx = (dA.cx + dB.cx) * 0.5f
            val vty = (dA.cy + dB.cy) * 0.5f - waist

            val len1 = hypot(vtx - p0x, vty - p0y) * 0.36f
            val t0x = cos(bR)
            val t0y = sin(bR)
            cubicTo(
                p0x + t0x * len1, p0y + t0y * len1,
                vtx - ux * len1, vty - uy * len1,
                vtx, vty,
            )

            val len2 = hypot(p3x - vtx, p3y - vty) * 0.36f
            val t3x = cos(bL)
            val t3y = -sin(bL)
            cubicTo(
                vtx + ux * len2, vty + uy * len2,
                p3x - t3x * len2, p3y - t3y * len2,
                p3x, p3y,
            )

            if (j < 3) {
                val sweepTop = (betaL[j + 1] + betaR[j + 1]) * radToDeg
                if (sweepTop > 0.05f) {
                    arcTo(
                        rect = Rect(dB.cx - dB.radius, dB.cy - dB.radius, dB.cx + dB.radius, dB.cy + dB.radius),
                        startAngleDegrees = 270f - betaL[j + 1] * radToDeg,
                        sweepAngleDegrees = sweepTop,
                        forceMoveTo = false,
                    )
                }
            }
        }

        val d4 = drops[4]
        val b4Deg = betaL[4] * radToDeg
        arcTo(
            rect = Rect(d4.cx - d4.radius, d4.cy - d4.radius, d4.cx + d4.radius, d4.cy + d4.radius),
            startAngleDegrees = 270f - b4Deg,
            sweepAngleDegrees = 180f + 2f * b4Deg,
            forceMoveTo = false,
        )

        for (j in 3 downTo 0) {
            val dA = drops[j]
            val dB = drops[j + 1]
            val dx = dB.cx - dA.cx
            val dy = dB.cy - dA.cy
            val axLen = hypot(dx, dy).coerceAtLeast(1e-4f)
            val ux = dx / axLen
            val uy = dy / axLen

            val bL = betaL[j + 1]
            val bR = betaR[j]
            val q0x = dB.cx - dB.radius * sin(bL)
            val q0y = dB.cy + dB.radius * cos(bL)
            val q3x = dA.cx + dA.radius * sin(bR)
            val q3y = dA.cy + dA.radius * cos(bR)

            val minR = minOf(dA.radius, dB.radius)
            val waist = lerp(minR, minR * 0.14f, pinchWaistT)
            val vbx = (dA.cx + dB.cx) * 0.5f
            val vby = (dA.cy + dB.cy) * 0.5f + waist

            val len1 = hypot(vbx - q0x, vby - q0y) * 0.36f
            val s0x = -cos(bL)
            val s0y = -sin(bL)
            cubicTo(
                q0x + s0x * len1, q0y + s0y * len1,
                vbx + ux * len1, vby + uy * len1,
                vbx, vby,
            )

            val len2 = hypot(q3x - vbx, q3y - vby) * 0.36f
            val s3x = -cos(bR)
            val s3y = sin(bR)
            cubicTo(
                vbx - ux * len2, vby - uy * len2,
                q3x - s3x * len2, q3y - s3y * len2,
                q3x, q3y,
            )

            if (j > 0) {
                val sweepBot = (betaR[j] + betaL[j]) * radToDeg
                if (sweepBot > 0.05f) {
                    arcTo(
                        rect = Rect(dA.cx - dA.radius, dA.cy - dA.radius, dA.cx + dA.radius, dA.cy + dA.radius),
                        startAngleDegrees = 90f - betaR[j] * radToDeg,
                        sweepAngleDegrees = sweepBot,
                        forceMoveTo = false,
                    )
                }
            }
        }

        close()
    }
}

private fun buildArbitraryCircleBridgePath(
    x1: Float,
    y1: Float,
    r1: Float,
    x2: Float,
    y2: Float,
    r2: Float,
    capillaryRange: Float,
    density: Density,
): Path? {
    val dx = x2 - x1
    val dy = y2 - y1
    val d = hypot(dx, dy)
    if (d < 1e-3f || d <= abs(r1 - r2) + 0.5f) return null

    val surfaceGap = d - r1 - r2
    if (surfaceGap >= capillaryRange) return null

    val pinchT = ((capillaryRange - surfaceGap) / capillaryRange.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val smoothPinch = pinchT * pinchT * (3f - 2f * pinchT)

    val pi = PI.toFloat()
    val maxSpread = acos(((r1 - r2) / d).coerceIn(-0.9999f, 0.9999f))
    val u1: Float
    val u2: Float
    if (d < r1 + r2) {
        u1 = acos(((r1 * r1 + d * d - r2 * r2) / (2f * r1 * d)).coerceIn(-1f, 1f))
        u2 = acos(((r2 * r2 + d * d - r1 * r1) / (2f * r2 * d)).coerceIn(-1f, 1f))
    } else {
        u1 = 0f
        u2 = 0f
    }

    val v = 0.52f * smoothPinch
    val a1 = u1 + (maxSpread - u1) * v
    val a2 = u2 + (pi - maxSpread - u2) * v

    val cos1 = cos(a1)
    val sin1 = sin(a1)
    val cos2 = cos(a2)
    val sin2 = sin(a2)

    val ux = dx / d
    val uy = dy / d
    val nx = -uy
    val ny = ux

    val p1ux = x1 + ux * (r1 * cos1) + nx * (r1 * sin1)
    val p1uy = y1 + uy * (r1 * cos1) + ny * (r1 * sin1)
    val p1lx = x1 + ux * (r1 * cos1) - nx * (r1 * sin1)
    val p1ly = y1 + uy * (r1 * cos1) - ny * (r1 * sin1)

    val p2ux = x2 - ux * (r2 * cos2) + nx * (r2 * sin2)
    val p2uy = y2 - uy * (r2 * cos2) + ny * (r2 * sin2)
    val p2lx = x2 - ux * (r2 * cos2) - nx * (r2 * sin2)
    val p2ly = y2 - uy * (r2 * cos2) - ny * (r2 * sin2)

    val chordLen = hypot(p2ux - p1ux, p2uy - p1uy)
    if (chordLen < 0.5f) return null

    var hSep = (chordLen * 0.36f).coerceAtMost((r1 + r2) * 0.62f)
    if (cos1 > 0f && cos2 > 0f) {
        val avgAnchorOffset = 0.5f * (r1 * sin1 + r2 * sin2)
        val minWaistHalf = (minOf(r1, r2) * 0.28f)
            .coerceAtLeast(with(density) { 2.dp.toPx() })
            .coerceAtMost(avgAnchorOffset * 0.72f)
        val maxDrop = (avgAnchorOffset - minWaistHalf).coerceAtLeast(0f)
        val midDrop = 0.375f * (cos1 + cos2) * hSep
        if (midDrop > maxDrop && midDrop > 1e-4f) {
            hSep *= maxDrop / midDrop
        }
    }
    val overlapT = ((r1 + r2 - d) / (r2 * 0.45f).coerceAtLeast(1f)).coerceIn(0f, 1f)
    val handleLen = lerp(hSep, chordLen * 0.34f, overlapT).coerceAtMost(chordLen * 0.45f)

    // Upper tangent leaves 1 along (+sin1*u - cos1*n) and enters 2 along (-sin2*u - cos2*n) backward
    val c1ux = p1ux + (ux * sin1 - nx * cos1) * handleLen
    val c1uy = p1uy + (uy * sin1 - ny * cos1) * handleLen
    val c2ux = p2ux + (-ux * sin2 - nx * cos2) * handleLen
    val c2uy = p2uy + (-uy * sin2 - ny * cos2) * handleLen

    // Lower tangent leaves 2 toward 1 along (-sin2*u + cos2*n) and arrives at 1 along (+sin1*u + cos1*n)
    val c1lx = p2lx + (-ux * sin2 + nx * cos2) * handleLen
    val c1ly = p2ly + (-uy * sin2 + ny * cos2) * handleLen
    val c2lx = p1lx + (ux * sin1 + nx * cos1) * handleLen
    val c2ly = p1ly + (uy * sin1 + ny * cos1) * handleLen

    return Path().apply {
        moveTo(p1ux, p1uy)
        cubicTo(c1ux, c1uy, c2ux, c2uy, p2ux, p2uy)
        lineTo(x2, y2)
        lineTo(p2lx, p2ly)
        cubicTo(c1lx, c1ly, c2lx, c2ly, p1lx, p1ly)
        lineTo(x1, y1)
        close()
    }
}

private fun buildLeftStreamPath(
    geom: LeftMorphGeometry,
    density: Density,
): Path {
    val capR = minOf(geom.halfWidth, geom.halfHeight).coerceAtLeast(1f)
    var mergedPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = geom.centerX - geom.halfWidth,
                top = geom.centerY - geom.halfHeight,
                right = geom.centerX + geom.halfWidth,
                bottom = geom.centerY + geom.halfHeight,
                radiusX = capR,
                radiusY = capR,
            )
        )
    }

    val axisLeft = geom.centerX - geom.halfWidth + capR
    val axisRight = geom.centerX + geom.halfWidth - capR
    val bridgeBoostPx = geom.dropSmoothKPx * 1.15f

    // 1. 合并 3 颗液态水珠，并与主胶囊最近端生成表面张力黏连桥
    for (i in 0 until EXPAND_DROPLET_COUNT) {
        val d = geom.droplets[i]
        if (!d.active || d.radius <= 0.5f) continue

        val nearestCapX = d.cx.coerceIn(minOf(axisLeft, axisRight), maxOf(axisLeft, axisRight))
        val distToCap = hypot(d.cx - nearestCapX, d.cy - geom.centerY)
        if (distToCap <= (capR - d.radius).coerceAtLeast(0f) + 0.5f) continue

        val dropPath = Path().apply {
            addOval(Rect(d.cx - d.radius, d.cy - d.radius, d.cx + d.radius, d.cy + d.radius))
        }
        val withDrop = Path()
        if (withDrop.op(mergedPath, dropPath, PathOperation.Union)) {
            mergedPath = withDrop
        }

        if (bridgeBoostPx > 0.5f) {
            val capillaryRange = minOf(capR, d.radius) * 0.95f + bridgeBoostPx
            val bridge = buildArbitraryCircleBridgePath(
                x1 = nearestCapX,
                y1 = geom.centerY,
                r1 = capR,
                x2 = d.cx,
                y2 = d.cy,
                r2 = d.radius,
                capillaryRange = capillaryRange,
                density = density,
            )
            if (bridge != null) {
                val withBridge = Path()
                if (withBridge.op(mergedPath, bridge, PathOperation.Union)) {
                    mergedPath = withBridge
                }
            }
        }
    }

    // 2. 相邻水珠之间拉出液态表面张力黏连桥（drop0 <-> drop1 <-> drop2）
    if (bridgeBoostPx > 0.5f) {
        for (i in 0 until EXPAND_DROPLET_COUNT - 1) {
            val dA = geom.droplets[i]
            val dB = geom.droplets[i + 1]
            if (!dA.active || dA.radius <= 0.5f || !dB.active || dB.radius <= 0.5f) continue
            val pairRange = minOf(dA.radius, dB.radius) * 1.15f + bridgeBoostPx
            val bridge = buildArbitraryCircleBridgePath(
                x1 = dA.cx,
                y1 = dA.cy,
                r1 = dA.radius,
                x2 = dB.cx,
                y2 = dB.cy,
                r2 = dB.radius,
                capillaryRange = pairRange,
                density = density,
            )
            if (bridge != null) {
                val withBridge = Path()
                if (withBridge.op(mergedPath, bridge, PathOperation.Union)) {
                    mergedPath = withBridge
                }
            }
        }
    }

    return mergedPath
}

private fun createExpandedCapsulePath(
    cx: Float,
    centerY: Float,
    halfW: Float,
    halfH: Float,
): Path {
    val pillRadius = minOf(halfW, halfH)
    return Path().apply {
        val outline = Continuity.Default.createCapsuleOutline(Size(halfW * 2f, halfH * 2f))
        val continuous = (outline as? Outline.Generic)?.path
        if (continuous != null) {
            addPath(continuous, Offset(cx - halfW, centerY - halfH))
            close()
        } else {
            addRoundRect(
                RoundRect(
                    left = cx - halfW,
                    top = centerY - halfH,
                    right = cx + halfW,
                    bottom = centerY + halfH,
                    radiusX = pillRadius,
                    radiusY = pillRadius,
                )
            )
        }
    }
}

internal fun buildLeftMorphPath(
    geom: LeftMorphGeometry,
    density: Density,
): Path {
    if (geom.isFullyExpanded) {
        return createExpandedCapsulePath(
            cx = geom.centerX,
            centerY = geom.centerY,
            halfW = geom.halfWidth,
            halfH = geom.halfHeight,
        )
    }

    if (geom.isFullyCollapsed) {
        return Path().apply {
            addOval(
                Rect(
                    geom.orbCx - geom.orbRadius,
                    geom.orbCy - geom.orbRadius,
                    geom.orbCx + geom.orbRadius,
                    geom.orbCy + geom.orbRadius,
                )
            )
        }
    }

    if (geom.mode == LeftMorphMode.ExpandStream) {
        return buildLeftStreamPath(geom, density)
    }

    if (geom.isBreakupPhase) {
        return buildBurstBreakupPath(geom)
    }

    if (!geom.seedSettled) {
        return Path().apply {
            for (d in geom.droplets) {
                if (d.active && d.radius > 0f) {
                    addTeardropOrOval(d.cx, d.cy, d.radius, d.vx, d.vy, d.stretch)
                }
            }
        }
    }

    val orb = geom.droplets[0]
    var orbPath = Path().apply {
        addOval(
            Rect(
                orb.cx - orb.radius,
                orb.cy - orb.radius,
                orb.cx + orb.radius,
                orb.cy + orb.radius,
            )
        )
    }
    var freeDropsPath: Path? = null
    val extraCapillaryPx = with(density) { 4.dp.toPx() }

    for (i in 1 until LEFT_BURST_DROPLET_COUNT) {
        val d = geom.droplets[i]
        if (!d.active || d.radius <= 0f) continue
        val dist = hypot(d.cx - orb.cx, d.cy - orb.cy)
        if (dist <= (orb.radius - d.radius).coerceAtLeast(0f) + 0.5f) continue

        val capillaryRange = minOf(orb.radius, d.radius) * 1.15f + extraCapillaryPx
        val surfaceGap = dist - orb.radius - d.radius
        if (surfaceGap < capillaryRange) {
            val dropOval = Path().apply {
                addOval(Rect(d.cx - d.radius, d.cy - d.radius, d.cx + d.radius, d.cy + d.radius))
            }
            val mergedDrop = Path()
            if (mergedDrop.op(orbPath, dropOval, PathOperation.Union)) {
                orbPath = mergedDrop
            }
            val bridge = buildArbitraryCircleBridgePath(
                x1 = orb.cx,
                y1 = orb.cy,
                r1 = orb.radius,
                x2 = d.cx,
                y2 = d.cy,
                r2 = d.radius,
                capillaryRange = capillaryRange,
                density = density,
            )
            if (bridge != null) {
                val withBridge = Path()
                if (withBridge.op(orbPath, bridge, PathOperation.Union)) {
                    orbPath = withBridge
                }
            }
        } else {
            val free = freeDropsPath ?: Path().also { freeDropsPath = it }
            free.addTeardropOrOval(d.cx, d.cy, d.radius, d.vx, d.vy, d.stretch)
        }
    }

    freeDropsPath?.let { orbPath.addPath(it) }
    return orbPath
}

private fun buildLeftClipPath(
    geom: LeftMorphGeometry,
    leftX: Float,
    centerY: Float,
    pillWidth: Float,
    height: Float,
    pressScale: Float,
    panelOffset: Float,
    collapseProgress: Float,
    density: Density,
): Path {
    if (geom.isFullyExpanded || geom.isFullyCollapsed) {
        return buildLeftMorphPath(geom, density)
    }

    if (geom.mode == LeftMorphMode.ExpandStream) {
        // 展开拉丝期用随液态前沿平滑延展的胶囊剪裁内容，避免水珠颈部将图标/文字切碎
        val p = collapseProgress.coerceIn(0f, 1f)
        val effW = (geom.streamFrontX - geom.leftEdge).coerceIn(height, pillWidth * pressScale)
        val r = (geom.halfHeight).coerceAtLeast(height * 0.42f)
        val left = leftX + panelOffset * p
        return Path().apply {
            addRoundRect(
                RoundRect(
                    left = left,
                    top = centerY - r,
                    right = left + effW,
                    bottom = centerY + r,
                    radiusX = r,
                    radiusY = r,
                )
            )
        }
    }

    // CollapseBurst 期间为正在左移的选中态与淡出态提供平滑收敛的胶囊剪裁，避免碎滴分割图标
    val p = collapseProgress.coerceIn(0f, 1f)
    val c = 1f - p
    val contractT = morphSmoothstep(0f, 0.92f, c)
    val effW = lerp(pillWidth * pressScale, height, contractT).coerceAtLeast(height)
    val r = height * 0.5f * lerp(pressScale, 1f, contractT)
    val left = leftX + panelOffset * p
    return Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = centerY - r,
                right = left + effW,
                bottom = centerY + r,
                radiusX = r,
                radiusY = r,
            )
        )
    }
}

internal class LeftMorphClipShape(
    private val panelOffset: () -> Float,
    private val pressScale: () -> Float,
    private val collapseProgress: () -> Float,
    private val jellyRadiusDeltaPx: () -> Float,
    private val morphMode: () -> LeftMorphMode = { LeftMorphMode.ExpandStream },
) : Shape {
    private var cachedOutline: Outline? = null
    private var lastWidth = Float.NaN
    private var lastHeight = Float.NaN
    private var lastDensity = Float.NaN
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastPo = Float.NaN
    private var lastScale = Float.NaN
    private var lastCollapse = Float.NaN
    private var lastJelly = Float.NaN
    private var lastMode: LeftMorphMode? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val po = panelOffset()
        val s = pressScale()
        val collapse = collapseProgress()
        val jelly = jellyRadiusDeltaPx()
        val mode = morphMode()
        val d = density.density
        val existing = cachedOutline
        if (existing != null &&
            size.width == lastWidth &&
            size.height == lastHeight &&
            d == lastDensity &&
            layoutDirection == lastLayoutDirection &&
            po == lastPo &&
            s == lastScale &&
            collapse == lastCollapse &&
            jelly == lastJelly &&
            mode == lastMode
        ) {
            return existing
        }

        val geom = computeLeftMorphGeometry(
            leftX = 0f,
            centerY = size.height * 0.5f,
            pillWidth = size.width,
            height = size.height,
            pressScale = s,
            panelOffset = po,
            collapseProgress = collapse,
            jellyRadiusDeltaPx = jelly,
            morphMode = mode,
            density = density,
        )
        val path = buildLeftClipPath(
            geom = geom,
            leftX = 0f,
            centerY = size.height * 0.5f,
            pillWidth = size.width,
            height = size.height,
            pressScale = s,
            panelOffset = po,
            collapseProgress = collapse,
            density = density,
        )
        return Outline.Generic(path).also {
            cachedOutline = it
            lastWidth = size.width
            lastHeight = size.height
            lastDensity = d
            lastLayoutDirection = layoutDirection
            lastPo = po
            lastScale = s
            lastCollapse = collapse
            lastJelly = jelly
            lastMode = mode
        }
    }
}

internal class BarShapeParams(
    val panelOffset: () -> Float,
    val pressScale: () -> Float,
    val trailingTranslation: () -> Offset,
    val trailingScale: () -> Offset,
    val extraPx: Float,
    val gapPx: Float,
    val maxNeckHalfPx: Float,
    val fusion: LiquidFusion,
    val bleedX: Float,
    val bleedY: Float,
    val collapseProgress: () -> Float = { 1f },
    val jellyRadiusDeltaPx: () -> Float = { 0f },
    val morphMode: () -> LeftMorphMode = { LeftMorphMode.ExpandStream },
)

internal class BarShape(private val p: BarShapeParams) : Shape {
    private var cachedOutline: Outline? = null
    private var lastWidth = Float.NaN
    private var lastHeight = Float.NaN
    private var lastDensity = Float.NaN
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastS = Float.NaN
    private var lastPo = Float.NaN
    private var lastRawCollapse = Float.NaN
    private var lastJellyDelta = Float.NaN
    private var lastMode: LeftMorphMode? = null
    private var lastTx = Float.NaN
    private var lastTy = Float.NaN
    private var lastTsx = Float.NaN
    private var lastTsy = Float.NaN

    private var snapshotVersion = 0
    private var snapS = Float.NaN
    private var snapPo = Float.NaN
    private var snapCollapse = Float.NaN
    private var snapJelly = Float.NaN
    private var snapMode: LeftMorphMode? = null
    private var snapTx = Float.NaN
    private var snapTy = Float.NaN
    private var snapTsx = Float.NaN
    private var snapTsy = Float.NaN
    private var cachedSnapshotShape: Shape? = null

    /**
     * 读取所有驱动外轮廓的实时 State；仅当动态参数发生变化时生成新版本号的代理 [Shape]，
     * 让 `drawBackdrop` 在收起/展开/拖曳期间立即更新裁剪轮廓，静止滚动时复用同引用跳过重传。
     */
    fun snapshotShape(): Shape {
        val s = p.pressScale()
        val po = p.panelOffset()
        val collapse = p.collapseProgress()
        val jelly = p.jellyRadiusDeltaPx()
        val mode = p.morphMode()
        val t = p.trailingTranslation()
        val ts = p.trailingScale()
        val existing = cachedSnapshotShape
        if (existing != null &&
            s == snapS &&
            po == snapPo &&
            collapse == snapCollapse &&
            jelly == snapJelly &&
            mode == snapMode &&
            t.x == snapTx &&
            t.y == snapTy &&
            ts.x == snapTsx &&
            ts.y == snapTsy
        ) {
            return existing
        }
        snapS = s
        snapPo = po
        snapCollapse = collapse
        snapJelly = jelly
        snapMode = mode
        snapTx = t.x
        snapTy = t.y
        snapTsx = ts.x
        snapTsy = ts.y
        snapshotVersion++
        return DelegatingBarShape(this, snapshotVersion).also {
            cachedSnapshotShape = it
        }
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val s = p.pressScale()
        val po = p.panelOffset()
        val rawCollapse = p.collapseProgress()
        val jellyDelta = p.jellyRadiusDeltaPx()
        val mode = p.morphMode()
        val t = p.trailingTranslation()
        val ts = p.trailingScale()
        val d = density.density
        val existing = cachedOutline
        if (existing != null &&
            size.width == lastWidth &&
            size.height == lastHeight &&
            d == lastDensity &&
            layoutDirection == lastLayoutDirection &&
            s == lastS &&
            po == lastPo &&
            rawCollapse == lastRawCollapse &&
            jellyDelta == lastJellyDelta &&
            mode == lastMode &&
            t.x == lastTx &&
            t.y == lastTy &&
            ts.x == lastTsx &&
            ts.y == lastTsy
        ) {
            return existing
        }

        val computed = computeOutline(
            size = size,
            density = density,
            s = s,
            po = po,
            collapse = rawCollapse,
            jellyDelta = jellyDelta,
            mode = mode,
            t = t,
            ts = ts,
        )
        cachedOutline = computed
        lastWidth = size.width
        lastHeight = size.height
        lastDensity = d
        lastLayoutDirection = layoutDirection
        lastS = s
        lastPo = po
        lastRawCollapse = rawCollapse
        lastJellyDelta = jellyDelta
        lastMode = mode
        lastTx = t.x
        lastTy = t.y
        lastTsx = ts.x
        lastTsy = ts.y
        return computed
    }

    private fun computeOutline(
        size: Size,
        density: Density,
        s: Float,
        po: Float,
        collapse: Float,
        jellyDelta: Float,
        mode: LeftMorphMode,
        t: Offset,
        ts: Offset,
    ): Outline {
        val h = size.height - p.bleedY * 2f
        val bx = p.bleedX
        val cy = p.bleedY + h / 2f
        val radius = h / 2f
        val pillPx = size.width - p.extraPx - bx * 2f

        val geom = computeLeftMorphGeometry(
            leftX = bx,
            centerY = cy,
            pillWidth = pillPx,
            height = h,
            pressScale = s,
            panelOffset = po,
            collapseProgress = collapse,
            jellyRadiusDeltaPx = jellyDelta,
            morphMode = mode,
            density = density,
        )
        val capsule = buildLeftMorphPath(geom, density)
        if (p.extraPx <= 0f) return Outline.Generic(capsule)

        val circleRx = radius * ts.x
        val circleRy = radius * ts.y
        val circleCx = bx + pillPx + p.gapPx + radius + t.x
        val circleCy = cy + t.y

        if (!geom.isFullyExpanded && !geom.isFullyCollapsed) {
            capsule.addOval(
                Rect(
                    circleCx - circleRx,
                    circleCy - circleRy,
                    circleCx + circleRx,
                    circleCy + circleRy,
                )
            )
            return Outline.Generic(capsule)
        }

        val halfW = geom.halfWidth
        val halfH = geom.halfHeight
        val cx = geom.centerX
        val pillRadius = minOf(halfW, halfH)

        val circle = Path().apply {
            addOval(Rect(circleCx - circleRx, circleCy - circleRy, circleCx + circleRx, circleCy + circleRy))
        }

        val merged = Path()
        merged.op(capsule, circle, PathOperation.Union)

        val axisLeft = cx - halfW + pillRadius
        val axisRight = cx + halfW - pillRadius
        val axisX = circleCx.coerceIn(minOf(axisLeft, axisRight), maxOf(axisLeft, axisRight))
        val vx = circleCx - axisX
        val vy = circleCy - cy
        val dist = sqrt(vx * vx + vy * vy)
        val ang = atan2(vy, vx)

        val fusion = p.fusion
        val circleAvgR = (circleRx + circleRy) / 2f
        val circleDirR = ellipseRadiusAt(circleRx, circleRy, ang)
        val surfaceGap = dist - pillRadius - circleDirR
        val neckThresholdPx = with(density) { (fusion.bulgeMaxDp * 2f).dp.toPx() }
        val k = if (surfaceGap >= neckThresholdPx) {
            0f
        } else {
            ((neckThresholdPx - surfaceGap) / neckThresholdPx).coerceIn(0f, 1f).pow(fusion.rampPower)
        }
        if (dist < 0.5f) return Outline.Generic(merged)
        val pi = PI.toFloat()

        val bulgeRangePx = with(density) { fusion.bulgeRangeDp.dp.toPx() }
        val bulgeT = if (surfaceGap >= bulgeRangePx) {
            0f
        } else {
            ((bulgeRangePx - surfaceGap) / (bulgeRangePx - neckThresholdPx)).coerceIn(0f, 1f)
        }

        val bulgeH = with(density) { fusion.bulgeMaxDp.dp.toPx() } * (bulgeT * bulgeT)
        val gaussEdge = exp(-0.25f / (2f * fusion.bulgeSigma * fusion.bulgeSigma))
        val phiPill = ((bulgeH * fusion.bulgeWidthRatio) / pillRadius)
            .coerceIn(fusion.bulgeMinAngle, fusion.bulgeMaxAngle)
        val phiCircle = ((bulgeH * fusion.bulgeWidthRatio) / circleAvgR)
            .coerceIn(fusion.bulgeMinAngle, fusion.bulgeMaxAngle)
        val bulgeCenterCircle = ang + pi

        fun bumpAt(angle: Float, center: Float, phi: Float): Float {
            val tt = (angle - (center - phi)) / (2f * phi)
            if (tt <= 0f || tt >= 1f) return 0f
            val dx = tt - 0.5f
            val rawG = exp(-(dx * dx) / (2f * fusion.bulgeSigma * fusion.bulgeSigma))
            return ((rawG - gaussEdge) / (1f - gaussEdge)).coerceIn(0f, 1f)
        }

        fun hillPath(
            ccx: Float,
            ccy: Float,
            rx: Float,
            ry: Float,
            center: Float,
            phi: Float,
        ): Path? {
            if (bulgeH < 0.3f) return null
            return Path().apply {
                val steps = 56
                for (i in 0..steps) {
                    val tt = i.toFloat() / steps
                    val a = (center - phi) + 2f * phi * tt
                    val r = ellipseRadiusAt(rx, ry, a) + bulgeH * bumpAt(a, center, phi)
                    val x = ccx + r * cos(a)
                    val y = ccy + r * sin(a)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }

        fun bulgeAt(): Path? {
            if (bulgeH < 0.3f) return null
            val a = hillPath(axisX, cy, pillRadius, pillRadius, ang, phiPill)
            val b = hillPath(circleCx, circleCy, circleRx, circleRy, bulgeCenterCircle, phiCircle)
            if (a == null || b == null) return null
            return Path().apply { op(a, b, PathOperation.Union) }
        }

        fun unionWith(extra: Path?): Outline.Generic? {
            if (extra == null) return null
            val out = Path()
            return if (out.op(merged, extra, PathOperation.Union)) Outline.Generic(out) else null
        }

        val bulge = bulgeAt()

        if (k <= 0.001f) return unionWith(bulge) ?: Outline.Generic(merged)

        val neckGeom = barNeckGeometryPx(
            pillCenterX = cx,
            pillCenterY = cy,
            pillHalfWidth = halfW,
            pillHalfHeight = halfH,
            circleCenterX = circleCx,
            circleCenterY = circleCy,
            circleRadiusX = circleRx,
            circleRadiusY = circleRy,
            maxNeckHalfPx = p.maxNeckHalfPx,
            fusion = fusion,
            density = density,
        )
        if (neckGeom == null || !neckGeom.hasNeck) {
            return unionWith(bulge) ?: Outline.Generic(merged)
        }

        val neckPath = Path().apply {
            moveTo(neckGeom.pu.x, neckGeom.pu.y)
            cubicTo(
                neckGeom.c1u.x, neckGeom.c1u.y,
                neckGeom.c2u.x, neckGeom.c2u.y,
                neckGeom.cu.x, neckGeom.cu.y,
            )
            lineTo(neckGeom.cl.x, neckGeom.cl.y)
            cubicTo(
                neckGeom.c2l.x, neckGeom.c2l.y,
                neckGeom.c1l.x, neckGeom.c1l.y,
                neckGeom.pl.x, neckGeom.pl.y,
            )
            close()
        }

        val withNeck = Path()
        if (!withNeck.op(merged, neckPath, PathOperation.Union)) return Outline.Generic(merged)
        if (bulge != null) {
            val combined = Path()
            if (combined.op(withNeck, bulge, PathOperation.Union)) return Outline.Generic(combined)
        }
        return Outline.Generic(withNeck)
    }
}

private class DelegatingBarShape(
    private val delegate: BarShape,
    private val version: Int,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = delegate.createOutline(size, layoutDirection, density)

    override fun equals(other: Any?): Boolean =
        other is DelegatingBarShape && other.delegate === delegate && other.version == version

    override fun hashCode(): Int = 31 * delegate.hashCode() + version
}