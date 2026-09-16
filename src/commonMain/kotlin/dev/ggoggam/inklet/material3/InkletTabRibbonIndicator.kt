package dev.ggoggam.inklet.material3

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainWidth
import dev.ggoggam.inklet.LocalInkletReduceMotion
import dev.ggoggam.inklet.LocalInkletStyle
import dev.ggoggam.inklet.LocalSketchFrame
import dev.ggoggam.inklet.Mulberry32
import kotlin.random.Random

/**
 * A continuous pen ribbon that unwinds from one tab and wraps around the next.
 * Use directly in a Material tab row's indicator slot, with an empty divider slot.
 * This owns its full-row layout; do not apply `tabIndicatorOffset`.
 * Compact scrollable tabs with generous label padding best suit the looping gesture.
 *
 * The theme supplies color, pen width, roughness and boil. Reduced motion snaps selection
 * and freezes the pen; other selection motion honors the platform duration scale.
 * For a swipeable pager, use the progress overload instead of animating the index twice.
 */
@Composable
fun TabIndicatorScope.InkletTabRibbonIndicator(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    seed: Int? = null,
    animationSpec: FiniteAnimationSpec<Float> = tween(450),
) {
    val progress =
        animateFloatAsState(
            targetValue = selectedTabIndex.toFloat(),
            animationSpec = if (LocalInkletReduceMotion.current) snap() else animationSpec,
            label = "Inklet tab ribbon",
        )
    InkletTabRibbonIndicator(
        progress = { progress.value },
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        color = color,
        seed = seed,
    )
}

/**
 * Draw the ribbon at a fractional tab index, e.g.
 * `{ pagerState.currentPage + pagerState.currentPageOffsetFraction }`.
 * [selectedTabIndex] must match the native row's selected index, including during a drag.
 * Progress is read only during drawing and clamped to the available tabs (NaN becomes zero).
 * The caller owns progress motion, including snapping pager progress for reduced motion.
 * Material retains tab input, semantics and scroll-to-selection behavior.
 *
 * Inspired by Sina Samaki's custom TabRow indicator: one connected path, four quadratic
 * curves per tab, and a moving window interpolated between measured section lengths.
 */
@Composable
fun TabIndicatorScope.InkletTabRibbonIndicator(
    progress: () -> Float,
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    seed: Int? = null,
) {
    val style = LocalInkletStyle.current
    val frame = LocalSketchFrame.current
    val mountSeed = remember(seed) { seed ?: Random.nextInt() }
    val positions = remember { mutableStateOf(emptyList<TabPosition>()) }
    Box(
        modifier
            .tabIndicatorLayout { measurable, constraints, tabs ->
                positions.value = tabs
                // Include both edge paddings so mirroring also aligns scrollable RTL rows.
                val width = tabs.lastOrNull()?.let { (it.right + tabs.first().left).roundToPx() } ?: 0
                val height = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
                val placeable = measurable.measure(Constraints.fixed(width, height))
                val slotWidth = constraints.constrainWidth(width)
                // Scrollable Material rows center the slot within the selected tab. Undo that
                // offset while reporting a legal slot size, avoiding Compose's implicit centering
                // of an oversized indicator. The child draws in full-row coordinates.
                val centering =
                    if (constraints.minWidth == 0) {
                        maxOf(0, ((tabs.getOrNull(selectedTabIndex)?.width?.roundToPx() ?: 0) - slotWidth) / 2)
                    } else {
                        0
                    }
                layout(slotWidth, height) { placeable.placeRelative(-centering, 0) }
            }.fillMaxSize()
            .drawWithCache {
                val tabs = positions.value
                val penWidth = style.strokeWidth.toPx()
                val boil = if (style.animate) style.boil else 0.0
                val guard = (penWidth / 2 + (style.roughness + boil).toFloat() * density)
                val insetY = maxOf(6 * density, guard + 2 * density).coerceAtMost(size.height / 2)
                val top = insetY
                val bottom = size.height - insetY
                // Perturb the curve's anchors, not densely sampled points: retain a fluid gesture.
                val ribbons =
                    List(if (boil > 0) 3 else 1) { frameIndex ->
                        val random = Mulberry32(mountSeed)
                        val movement = Mulberry32(mountSeed + frameIndex * 7919)

                        fun jitter(): Float =
                            (
                                (
                                    (random.nextDouble() * 2 - 1) * style.roughness +
                                        (movement.nextDouble() * 2 - 1) * boil
                                ) * density
                            ).toFloat()
                        val path = Path()
                        val measure = PathMeasure()
                        val starts = FloatArray(tabs.size)
                        val ends = FloatArray(tabs.size)
                        tabs.forEachIndexed { index, tab ->
                            val insetX = maxOf(4 * density, guard + density).coerceAtMost(tab.width.toPx() / 2)
                            val left = tab.left.toPx() + insetX
                            val right = tab.right.toPx() - insetX
                            val middle = (left + right) / 2
                            val centerY = size.height / 2
                            if (index == 0) path.moveTo(left, top) else path.lineTo(left, top)
                            measure.setPath(path, false)
                            starts[index] = measure.length
                            path.quadraticTo(right + jitter(), top + jitter(), right, centerY)
                            path.quadraticTo(right + jitter(), bottom + jitter(), middle, bottom)
                            path.quadraticTo(left + jitter(), bottom + jitter(), left, centerY)
                            path.quadraticTo(left + jitter(), top + jitter(), right, top)
                            measure.setPath(path, false)
                            ends[index] = measure.length
                        }
                        Ribbon(measure, starts, ends)
                    }
                val segment = Path()
                val pen = Stroke(penWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    if (tabs.isEmpty() || bottom <= top) return@onDrawBehind
                    val ribbon = ribbons[frame.value % ribbons.size]
                    val value = progress().let { if (it.isNaN()) 0f else it.coerceIn(0f, tabs.lastIndex.toFloat()) }
                    val from = value.toInt()
                    val to = minOf(from + 1, tabs.lastIndex)
                    val fraction = value - from

                    fun FloatArray.interpolate(): Float = this[from] + (this[to] - this[from]) * fraction
                    segment.reset()
                    ribbon.measure.getSegment(ribbon.starts.interpolate(), ribbon.ends.interpolate(), segment)
                    // Flip the gesture vertically so the overlapping stroke sits below the label.
                    scale(if (layoutDirection == LayoutDirection.Rtl) -1f else 1f, -1f) {
                        drawPath(segment, color, style = pen)
                    }
                }
            },
    )
}

private class Ribbon(
    val measure: PathMeasure,
    val starts: FloatArray,
    val ends: FloatArray,
)
