package dev.ggoggam.inklet

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Align the top pen stroke with Material's label cutout, even at large roughness. */
@Composable
internal fun InkletTextFieldContainer(
    ink: Color,
    fill: Color,
    cornerRadius: Dp,
    seed: Int,
    modifier: Modifier = Modifier,
    labelBounds: () -> Rect? = { null },
) {
    val style = LocalInkletStyle.current
    val frame = LocalSketchFrame.current
    Box(
        modifier.drawWithCache {
            val w = size.width / density.toDouble()
            val h = size.height / density.toDouble()
            val options = RoughOptions(seed, style.roughness, if (style.animate) style.boil else 0.0)
            val inset = (2.1 * options.roughness + options.boil + style.strokeWidth.value / 2).coerceAtMost(minOf(w, h) / 2)
            // Unlike a generic border, this top edge sits at y=0. The field reserves room above
            // it; Material clips the animated label gap around y=0, independently of pen width.
            val frames =
                Rough.variants(options) {
                    Rough.roundedRect(inset, 0.0, w - 2 * inset, h - inset, cornerRadius.value.toDouble(), it)
                }
            val paths = frames.map { it.toPenPath(density) }
            val fills = frames.map { it.take(1).toPenPath(density) }
            val pen = Stroke(style.strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                val index = frame.value % paths.size
                drawPath(fills[index], fill)
                val label = labelBounds()?.takeUnless { it.isEmpty }
                if (label == null) {
                    drawPath(paths[index], ink, style = pen)
                } else {
                    // The host's animated cutout handles the settled outline gap. A rough pen
                    // can extend past that cutout during motion, so also clear the moving label.
                    clipRect(
                        label.left - 4.dp.toPx(),
                        label.top - 1.dp.toPx(),
                        label.right + 4.dp.toPx(),
                        label.bottom + 1.dp.toPx(),
                        ClipOp.Difference,
                    ) {
                        drawPath(paths[index], ink, style = pen)
                    }
                }
            }
        },
    )
}
