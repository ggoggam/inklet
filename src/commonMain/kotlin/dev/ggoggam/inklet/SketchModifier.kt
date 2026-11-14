package dev.ggoggam.inklet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.random.Random

/** Midpoint quadratic smoothing, matching upstream toPath without an SVG parse at runtime. */
internal fun List<PenStroke>.toPenPath(density: Float): Path =
    Path().apply {
        fun Double.px() = (this * density).toFloat()
        for (stroke in this@toPenPath) {
            val points = stroke.points
            if (points.isEmpty()) continue
            if (stroke.closed && points.size > 2) {
                // Smooth through the closing seam as well as the rest of the loop.
                val last = points.last()
                val first = points.first()
                moveTo(((last.x + first.x) / 2).px(), ((last.y + first.y) / 2).px())
                for (i in points.indices) {
                    val p = points[i]
                    val next = points[(i + 1) % points.size]
                    quadraticTo(p.x.px(), p.y.px(), ((p.x + next.x) / 2).px(), ((p.y + next.y) / 2).px())
                }
                close()
                continue
            }
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

/** [Field] is a text field outline whose top edge sits at y=0, under Material's label cutout. */
internal enum class PenShape { Rectangle, Ellipse, Line, Check, Dot, Field }

/** Draw over existing native controls without replacing their input, focus, or semantics. */
@Composable
fun Modifier.inkletBorder(
    color: Color,
    cornerRadius: Dp = 12.dp,
    seed: Int? = null,
): Modifier = sketch(PenShape.Rectangle, color, cornerRadius = cornerRadius, seed = seed)

/**
 * Sketch a container behind its content. Set the host component's own container and border
 * colors to transparent. This changes drawing only; the host owns input, layout and semantics.
 */
@Composable
fun Modifier.inkletSurface(
    containerColor: Color,
    ink: Color,
    cornerRadius: Dp = 12.dp,
    scribble: Boolean = false,
    seed: Int? = null,
): Modifier = sketch(PenShape.Rectangle, ink, containerColor, cornerRadius, scribble, seed)

/** A decoration for a single label/block. For wrapped text, decorate individual Text spans. */
@Composable
fun Modifier.inkletDecoration(
    decoration: InkletDecoration,
    color: Color,
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

/**
 * Sketch a pen shape behind ([fill], scribble) and over (ink) the node's content.
 * [strokeProgress] draws the ink partially, for a pen that is still drawing. [labelBounds] clears
 * the ink under a moving label, in this node's coordinates.
 */
@Composable
internal fun Modifier.sketch(
    shape: PenShape,
    ink: Color,
    fill: Color = Color.Transparent,
    cornerRadius: Dp = 12.dp,
    scribble: Boolean = false,
    seed: Int? = null,
    underline: Boolean = false,
    scribbleAlpha: Float = 1f,
    strokeProgress: (() -> Float)? = null,
    labelBounds: (() -> Rect?)? = null,
): Modifier {
    val style = LocalInkletStyle.current
    val frame = LocalSketchFrame.current
    val mountSeed = remember(seed) { seed ?: Random.nextInt() }
    return this then
        SketchElement(
            PenSpec(shape, cornerRadius, scribble, mountSeed, underline, style),
            ink,
            fill,
            scribbleAlpha,
            strokeProgress,
            labelBounds,
            frame,
        )
}

/** Everything that decides the pen's geometry, apart from the node's size. */
private data class PenSpec(
    val shape: PenShape,
    val cornerRadius: Dp,
    val scribble: Boolean,
    val seed: Int,
    val underline: Boolean,
    val style: InkletStyle,
)

private data class SketchElement(
    val spec: PenSpec,
    val ink: Color,
    val fill: Color,
    val scribbleAlpha: Float,
    val strokeProgress: (() -> Float)?,
    val labelBounds: (() -> Rect?)?,
    val frame: State<Int>,
) : ModifierNodeElement<SketchNode>() {
    override fun create() = SketchNode(this)

    override fun update(node: SketchNode) = node.update(this)

    override fun InspectorInfo.inspectableProperties() {
        name = "sketch"
        properties["shape"] = spec.shape
        properties["ink"] = ink
        properties["fill"] = fill
    }
}

/**
 * The pen's drawings for one size: one per boil frame, each in its own [GraphicsLayer], recorded
 * once. On Android this node draws every frame's layer and shows one of them through the layers'
 * alpha. A boil tick only changes those alphas, so it records nothing: neither the paths nor the
 * content around this node. Only a change of size, style or color records the layers again.
 */
private class SketchNode(
    private var element: SketchElement,
) : Modifier.Node(),
    DrawModifierNode,
    ObserverModifierNode,
    CompositionLocalConsumerModifierNode {
    private var pen: Pen? = null
    private var recorded = false
    private var frame = 0

    // Fill and scribble, drawn behind the content; ink, drawn over it. Index = boil frame.
    private var backs: List<GraphicsLayer> = emptyList()
    private var fronts: List<GraphicsLayer> = emptyList()
    private var redraw: (() -> Unit)? = null

    fun update(next: SketchElement) {
        val previous = element
        element = next
        if (next.spec != previous.spec) {
            pen = null
            recorded = false
        } else if (next.ink != previous.ink || next.fill != previous.fill || next.scribbleAlpha != previous.scribbleAlpha) {
            recorded = false
        }
        if (next.frame !== previous.frame) observeFrame()
        invalidateDraw()
    }

    override fun onAttach() {
        redraw = windowRedraw()
        observeFrame()
    }

    override fun onDetach() {
        val graphics = requireGraphicsContext()
        (backs + fronts).forEach(graphics::releaseGraphicsLayer)
        backs = emptyList()
        fronts = emptyList()
        pen = null
        recorded = false
        redraw = null
    }

    /**
     * Reads the boil clock outside drawing, so a tick never invalidates the parent's drawing.
     * The observation outlives each change, so a tick only reads the new value.
     */
    private fun observeFrame() {
        var next = 0
        observeReads { next = element.frame.value }
        showFrame(next)
    }

    override fun onObservedReadsChanged() = showFrame(element.frame.value)

    private fun showFrame(next: Int) {
        if (next == frame) return
        val previous = frame
        frame = next
        if (!recorded || (backs.size < 2 && fronts.size < 2)) return
        for (layers in listOf(backs, fronts)) {
            if (layers.isEmpty()) continue
            layers[previous % layers.size].alpha = 0f
            layers[next % layers.size].alpha = 1f
        }
        // A partially drawn stroke is drawn directly, so it needs a new recording to move.
        val drawing = (element.strokeProgress?.invoke() ?: 1f) < 1f
        val redraw = redraw
        if (redraw != null && !drawing) redraw() else invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val current =
            pen?.takeIf { it.size == size && it.density == density } ?: buildPen().also {
                pen = it
                recorded = false
            }
        if (!recorded) record(current)
        drawFrames(backs)
        drawContent()
        val progress = element.strokeProgress?.invoke()?.coerceIn(0f, 1f) ?: 1f
        if (fronts.isEmpty() || progress == 0f) return
        val label = element.labelBounds?.invoke()?.takeUnless { it.isEmpty }
        if (label == null) {
            drawInk(current, progress)
        } else {
            // The host's animated cutout handles the settled outline gap. A rough pen
            // can extend past that cutout during motion, so also clear the moving label.
            clipRect(
                label.left - 4.dp.toPx(),
                label.top - 1.dp.toPx(),
                label.right + 4.dp.toPx(),
                label.bottom + 1.dp.toPx(),
                ClipOp.Difference,
            ) { drawInk(current, progress) }
        }
    }

    /**
     * Where the window can show a layer's new alpha, draw every frame's layer, so a tick needs no
     * new recording. Elsewhere a tick records this drawing again anyway, so draw only the frame shown.
     */
    private fun DrawScope.drawFrames(layers: List<GraphicsLayer>) {
        if (redraw != null) {
            layers.forEach { drawLayer(it) }
        } else if (layers.isNotEmpty()) {
            drawLayer(layers[frame % layers.size])
        }
    }

    private fun DrawScope.drawInk(
        pen: Pen,
        progress: Float,
    ) {
        if (progress == 1f) {
            drawFrames(fronts)
            return
        }
        val segment = pen.segment
        pen.measures(frame % pen.count).forEach { measure ->
            segment.reset()
            measure.getSegment(0f, measure.length * progress, segment, startWithMoveTo = true)
            drawPath(segment, element.ink, style = pen.stroke)
        }
    }

    private fun DrawScope.record(pen: Pen) {
        val element = element
        val hasBack = element.fill != Color.Transparent || pen.scribbles.isNotEmpty()
        backs = layers(backs, if (hasBack) pen.count else 0)
        fronts = layers(fronts, if (element.ink.alpha > 0f) pen.count else 0)
        val size = IntSize(size.width.roundToInt(), size.height.roundToInt())
        backs.forEachIndexed { i, layer ->
            layer.record(this, layoutDirection, size) {
                if (element.fill != Color.Transparent) drawPath(pen.fills[i], element.fill)
                if (pen.scribbles.isNotEmpty()) {
                    val ink = element.ink.copy(alpha = element.ink.alpha * 0.16f * element.scribbleAlpha)
                    clipPath(pen.fills[i]) { drawPath(pen.scribbles[i], ink, style = pen.stroke) }
                }
            }
        }
        fronts.forEachIndexed { i, layer ->
            layer.record(this, layoutDirection, size) { drawPath(pen.paths[i], element.ink, style = pen.stroke) }
        }
        for (layers in listOf(backs, fronts)) {
            layers.forEachIndexed { i, layer -> layer.alpha = if (i == frame % layers.size) 1f else 0f }
        }
        recorded = true
    }

    private fun layers(
        current: List<GraphicsLayer>,
        count: Int,
    ): List<GraphicsLayer> {
        if (current.size == count) return current
        val graphics = requireGraphicsContext()
        current.drop(count).forEach(graphics::releaseGraphicsLayer)
        return current.take(count) + List(maxOf(0, count - current.size)) { graphics.createGraphicsLayer() }
    }

    private fun DrawScope.buildPen(): Pen {
        val spec = element.spec
        val style = spec.style
        val w = size.width / density.toDouble()
        val h = size.height / density.toDouble()
        // Small indicators need a lighter hand than card outlines. Lines retain their amplitude.
        val scale =
            when (spec.shape) {
                PenShape.Line, PenShape.Field -> 1.0
                else -> (minOf(w, h) / 48.0).coerceAtMost(1.0)
            }
        val options = RoughOptions(spec.seed, style.roughness * scale, if (style.animate) style.boil * scale else 0.0)
        val inset = (2.1 * options.roughness + options.boil + style.strokeWidth.value / 2).coerceAtMost(minOf(w, h) / 2)
        val frames =
            Rough.variants(options) { o ->
                when (spec.shape) {
                    PenShape.Rectangle -> {
                        Rough.roundedRect(inset, inset, w - inset * 2, h - inset * 2, spec.cornerRadius.value.toDouble(), o)
                    }

                    PenShape.Ellipse -> {
                        Rough.ellipse(w / 2, h / 2, w / 2 - inset, h / 2 - inset, o)
                    }

                    PenShape.Line -> {
                        val y = if (spec.underline) h - inset else h / 2
                        Rough.line(inset, y, w - inset, y, o)
                    }

                    PenShape.Check -> {
                        Rough.checkmark(inset, inset, w - inset * 2, h - inset * 2, o)
                    }

                    PenShape.Dot -> {
                        Rough.dot(w / 2, h / 2, (minOf(w, h) / 2 - inset) / 1.3, o)
                    }

                    // Unlike a generic border, this top edge sits at y=0. The field reserves room
                    // above it; Material clips the label gap around y=0, independently of pen width.
                    PenShape.Field -> {
                        Rough.roundedRect(inset, 0.0, w - 2 * inset, h - inset, spec.cornerRadius.value.toDouble(), o)
                    }
                }
            }
        val scribbles =
            if (spec.scribble) {
                Rough
                    .variants(options) { Rough.scribble(inset, inset, w - inset * 2, h - inset * 2, it) }
                    .map { it.toPenPath(density) }
            } else {
                emptyList()
            }
        return Pen(
            size = size,
            density = density,
            frames = frames,
            paths = frames.map { it.toPenPath(density) },
            // Fill only the first closed pen pass, so overlapping outlines don't create fill seams.
            fills = frames.map { it.take(1).toPenPath(density) },
            scribbles = scribbles,
            stroke = Stroke(style.strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private class Pen(
    val size: Size,
    val density: Float,
    private val frames: List<List<PenStroke>>,
    val paths: List<Path>,
    val fills: List<Path>,
    val scribbles: List<Path>,
    val stroke: Stroke,
) {
    val count get() = paths.size
    val segment = Path()
    private var measures: List<List<PathMeasure>>? = null

    /** Each pen pass measured separately, since PathMeasure reads one contour at a time. */
    fun measures(index: Int): List<PathMeasure> =
        (
            measures ?: frames
                .map { strokes ->
                    strokes.map { stroke -> PathMeasure().apply { setPath(listOf(stroke).toPenPath(density), stroke.closed) } }
                }.also { measures = it }
        )[index]
}
