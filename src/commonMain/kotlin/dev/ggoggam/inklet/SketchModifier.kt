package dev.ggoggam.inklet

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** Midpoint quadratic smoothing, matching upstream toPath without an SVG parse at runtime. */
internal fun List<PenStroke>.toPenPath(density: Float): Path =
    Path().apply {
        fun Double.px() = (this * density).toFloat()
        for (stroke in this@toPenPath) {
            val points = stroke.points
            if (points.isEmpty()) continue
            moveTo(points.first().x.px(), points.first().y.px())
            for (i in 1 until points.lastIndex) {
                val p = points[i]
                val next = points[i + 1]
                quadraticTo(p.x.px(), p.y.px(), ((p.x + next.x) / 2).px(), ((p.y + next.y) / 2).px())
            }
            lineTo(points.last().x.px(), points.last().y.px())
            if (stroke.closed) close()
        }
    }

enum class InkletDecoration { Underline, Highlight, Circle }

internal enum class PenShape { Rectangle, Ellipse, Line, Check }

/** Draw over existing native controls without replacing their input, focus, or semantics. */
@Composable
fun Modifier.inkletBorder(
    color: Color = MaterialTheme.colorScheme.outline,
    cornerRadius: Dp = 12.dp,
    seed: Int? = null,
): Modifier = sketch(PenShape.Rectangle, color, cornerRadius = cornerRadius, seed = seed)

/** A decoration for a single label/block. For wrapped text, decorate individual Text spans. */
@Composable
fun Modifier.inkletDecoration(
    decoration: InkletDecoration,
    color: Color = MaterialTheme.colorScheme.primary,
    seed: Int? = null,
): Modifier =
    when (decoration) {
        InkletDecoration.Underline -> {
            sketch(PenShape.Line, color, seed = seed, underline = true)
        }

        InkletDecoration.Circle -> {
            sketch(PenShape.Ellipse, color, seed = seed)
        }

        InkletDecoration.Highlight -> {
            sketch(
                PenShape.Rectangle,
                Color.Transparent,
                fill = color.copy(alpha = 0.22f),
                cornerRadius = 2.dp,
                seed = seed,
            )
        }
    }

@Composable
internal fun Modifier.sketch(
    shape: PenShape,
    ink: Color,
    fill: Color = Color.Transparent,
    cornerRadius: Dp = 12.dp,
    scribble: Boolean = false,
    seed: Int? = null,
    underline: Boolean = false,
): Modifier {
    val style = LocalInkletStyle.current
    val frame = LocalSketchFrame.current
    val mountSeed = remember(seed) { seed ?: Random.nextInt() }
    return drawWithCache {
        val w = size.width / density.toDouble()
        val h = size.height / density.toDouble()
        // Leave enough room for both pen passes and their rounded stroke caps inside the bounds.
        val inset = (2.1 * style.roughness + style.boil + style.strokeWidth.value / 2).coerceAtMost(minOf(w, h) / 2)
        val options = RoughOptions(mountSeed, style.roughness, if (style.animate) style.boil else 0.0)
        val frames =
            Rough.variants(options) { o ->
                when (shape) {
                    PenShape.Rectangle -> {
                        Rough.roundedRect(inset, inset, w - inset * 2, h - inset * 2, cornerRadius.value.toDouble(), o)
                    }

                    PenShape.Ellipse -> {
                        Rough.ellipse(w / 2, h / 2, w / 2 - inset, h / 2 - inset, o)
                    }

                    PenShape.Line -> {
                        val y = if (underline) h - inset else h / 2
                        Rough.line(inset, y, w - inset, y, o)
                    }

                    PenShape.Check -> {
                        Rough.checkmark(inset, inset, w - inset * 2, h - inset * 2, o)
                    }
                }
            }
        val paths = frames.map { it.toPenPath(density) }
        // Fill only the first closed pen pass, so overlapping outlines don't create fill seams.
        val fills = frames.map { it.take(1).toPenPath(density) }
        val scribbles =
            if (scribble) {
                Rough
                    .variants(options) {
                        Rough.scribble(inset, inset, w - inset * 2, h - inset * 2, it)
                    }.map { it.toPenPath(density) }
            } else {
                emptyList()
            }
        val pen = Stroke(style.strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        onDrawWithContent {
            val index = frame.value % paths.size
            if (fill != Color.Transparent) drawPath(fills[index], fill)
            if (scribbles.isNotEmpty()) {
                clipPath(fills[index]) { drawPath(scribbles[index], ink.copy(alpha = ink.alpha * 0.16f), style = pen) }
            }
            drawContent()
            drawPath(paths[index], ink, style = pen)
        }
    }
}
