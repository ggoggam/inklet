package dev.ggoggam.inklet.material3

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.InkletStyle
import dev.ggoggam.inklet.InkletTheme
import dev.ggoggam.inklet.LocalInkletReduceMotion
import dev.ggoggam.inklet.LocalInkletStyle
import dev.ggoggam.inklet.LocalSketchFrame
import dev.ggoggam.inklet.Rough
import dev.ggoggam.inklet.RoughOptions
import dev.ggoggam.inklet.toPenPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

/**
 * Pen-drawn progress from the layout's start edge. [progress] is clamped to 0..1 (NaN is zero).
 * Inherits the theme's pen width; defaults to 240 x 12dp. Label the operation at the call site.
 * Progress updates are immediate; callers can supply an animated value if desired.
 */
@Composable
fun InkletLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    seed: Int? = null,
) = InkletProgressIndicator(progress, modifier, color, trackColor, seed, circular = false)

/**
 * A travelling pen stroke. Loading motion is independent of pen boil and [InkletStyle.animate].
 * [InkletTheme]'s reduceMotion flag freezes a visible segment, retaining indeterminate semantics.
 */
@Composable
fun InkletLinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    seed: Int? = null,
) = InkletProgressIndicator(null, modifier, color, trackColor, seed, circular = false)

/**
 * Pen-drawn clockwise progress starting at the top. [progress] is clamped to 0..1 (NaN is zero).
 * Inherits the theme's pen width; defaults to 40dp. Label the operation at the call site.
 * A non-square size keeps the circle centered in the available bounds.
 */
@Composable
fun InkletCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    trackColor: Color = ProgressIndicatorDefaults.circularDeterminateTrackColor,
    seed: Int? = null,
) = InkletProgressIndicator(progress, modifier, color, trackColor, seed, circular = true)

/** A rotating, growing pen arc, with the same motion policy as [InkletLinearProgressIndicator]. */
@Composable
fun InkletCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    trackColor: Color = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
    seed: Int? = null,
) = InkletProgressIndicator(null, modifier, color, trackColor, seed, circular = true)

@Composable
private fun loadingPhase(circular: Boolean): State<Float> {
    if (LocalInkletReduceMotion.current) return rememberUpdatedState(0f)
    val transition = rememberInfiniteTransition(label = "Inklet loading")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (circular) 6000 else 1800, easing = LinearEasing)),
        label = "Loading phase",
    )
}

private fun Float.progressFraction() = if (isNaN()) 0f else coerceIn(0f, 1f)

@Composable
private fun InkletProgressIndicator(
    progress: (() -> Float)?,
    modifier: Modifier,
    color: Color,
    trackColor: Color,
    seed: Int?,
    circular: Boolean,
) {
    val style = LocalInkletStyle.current
    val frame = LocalSketchFrame.current
    val phase = if (progress == null) loadingPhase(circular) else rememberUpdatedState(0f)
    val mountSeed = remember(seed) { seed ?: Random.nextInt() }
    Box(
        modifier
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo =
                    if (progress == null) {
                        ProgressBarRangeInfo.Indeterminate
                    } else {
                        ProgressBarRangeInfo(progress().progressFraction(), 0f..1f)
                    }
            }.size(if (circular) 40.dp else 240.dp, if (circular) 40.dp else 12.dp)
            .drawWithCache {
                val w = size.width / density.toDouble()
                val h = size.height / density.toDouble()
                val attenuation = if (circular) (minOf(w, h) / 48.0).coerceAtMost(1.0) else 1.0
                val options =
                    RoughOptions(
                        mountSeed,
                        style.roughness * attenuation,
                        if (style.animate) style.boil * attenuation else 0.0,
                    )
                val inset =
                    (2.1 * options.roughness + options.boil + style.strokeWidth.value / 2)
                        .coerceAtMost(minOf(w, h) / 2)
                val frames =
                    Rough.variants(options) { o ->
                        if (circular) {
                            Rough.circle(w / 2, h / 2, minOf(w, h) / 2 - inset, o)
                        } else if (w > inset * 2 && h > 0) {
                            Rough.line(inset, h / 2, w - inset, h / 2, o)
                        } else {
                            emptyList()
                        }
                    }
                // Measure each pen pass separately: PathMeasure only measures one contour.
                // Progress and animation are read in drawing so the full paths stay cached.
                val paths = frames.map { strokes -> strokes.map { listOf(it).toPenPath(density) } }
                val measures = paths.map { passes -> passes.map { PathMeasure().apply { setPath(it, circular) } } }
                val segment = Path()
                val pen = Stroke(style.strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                // Closed smoothing starts halfway between the last and first sample.
                val sampleCount =
                    frames
                        .first()
                        .firstOrNull()
                        ?.points
                        ?.size ?: 1
                val rotation = -90f + 180f / sampleCount
                onDrawBehind {
                    val index = frame.value % paths.size
                    val fraction = progress?.invoke()?.progressFraction()
                    val time = phase.value
                    val start =
                        when {
                            fraction != null -> 0f

                            // Three turns per growth cycle keep both arc ends moving clockwise.
                            // Modulo also handles the endpoint when system animations are disabled.
                            circular -> (time * 3f) % 1f

                            else -> ((time + 0.5f) % 1f) * 1.4f - 0.4f
                        }
                    val end =
                        when {
                            fraction != null -> fraction
                            circular -> start + 0.1f + 0.77f * ((1 - cos(time * 2 * PI)) / 2).toFloat()
                            else -> start + 0.4f
                        }

                    fun drawIndicator() {
                        paths[index].forEach { path -> drawPath(path, trackColor, style = pen) }
                        paths[index].forEachIndexed { pass, path ->
                            if (fraction == 1f) {
                                drawPath(path, color, style = pen)
                            } else {
                                val measure = measures[index][pass]
                                segment.reset()
                                val from = start.coerceIn(0f, 1f)
                                val to = end.coerceIn(0f, 1f)
                                if (to > from) {
                                    measure.getSegment(from * measure.length, to * measure.length, segment)
                                    drawPath(segment, color, style = pen)
                                }
                                if (circular && end > 1f) {
                                    // Extract and draw each side of the seam separately. Some path
                                    // backends replace the destination instead of appending to it.
                                    segment.reset()
                                    measure.getSegment(0f, (end - 1f) * measure.length, segment, startWithMoveTo = true)
                                    drawPath(segment, color, style = pen)
                                }
                            }
                        }
                    }
                    if (circular) {
                        rotate(rotation) { drawIndicator() }
                    } else {
                        scale(if (layoutDirection == LayoutDirection.Rtl) -1f else 1f, 1f) { drawIndicator() }
                    }
                }
            },
    )
}
