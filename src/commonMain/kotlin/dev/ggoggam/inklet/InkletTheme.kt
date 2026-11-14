package dev.ggoggam.inklet

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToLong
import kotlin.time.TimeSource

/**
 * Shared pen settings. [roughness] controls the base drawing's irregularity; zero is smooth.
 * [boil] controls frame-to-frame displacement in logical dp, independently of roughness.
 * Small indicators attenuate both amounts to keep their shapes legible. Boil is an amplitude,
 * not a speed: the theme cycles through three drawings, each shown for
 * [InkletMotion.boilFrameMillis] (400ms by default). [animate] = false or
 * the theme's reduceMotion flag disables boil and interaction re-sketching.
 */
@Immutable
data class InkletStyle(
    val roughness: Double = 0.3,
    val boil: Double = 0.3,
    val strokeWidth: Dp = 1.2.dp,
    val animate: Boolean = true,
) {
    init {
        require(roughness.isFinite() && roughness >= 0)
        require(boil.isFinite() && boil >= 0)
        require(strokeWidth.value.isFinite() && strokeWidth > 0.dp)
    }
}

/**
 * Selection animation specs and the boil's pace. Customize duration and easing with tween, use
 * spring for a spring response, or snap to disable a transition. [toggle] drives both thumb and
 * shading. Reduced motion takes precedence over these specs. Loading motion is independent.
 * [boilFrameMillis] is how long the pen shows each of its three drawings; it must be positive.
 * The platform's motion-duration scale stretches it. A new value applies to the running boil
 * right away.
 */
@Immutable
data class InkletMotion(
    val checkbox: FiniteAnimationSpec<Float> = tween(200, easing = LinearEasing),
    val toggle: FiniteAnimationSpec<Float> = tween(180),
    val boilFrameMillis: Int = DEFAULT_BOIL_FRAME_MILLIS,
) {
    init {
        require(boilFrameMillis > 0) { "boilFrameMillis must be positive, was $boilFrameMillis" }
    }

    /** The 0.3.1 constructor, kept so code compiled against it still links. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        checkbox: FiniteAnimationSpec<Float> = tween(200, easing = LinearEasing),
        toggle: FiniteAnimationSpec<Float> = tween(180),
    ) : this(checkbox, toggle, DEFAULT_BOIL_FRAME_MILLIS)

    /** The 0.3.1 copy, kept so code compiled against it still links. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun copy(
        checkbox: FiniteAnimationSpec<Float> = this.checkbox,
        toggle: FiniteAnimationSpec<Float> = this.toggle,
    ) = copy(checkbox = checkbox, toggle = toggle, boilFrameMillis = boilFrameMillis)
}

private const val DEFAULT_BOIL_FRAME_MILLIS = 400

/**
 * Dynamic, unlike the other Inklet locals: a new boil pace recomposes only the controls that read
 * their selection specs here, not the whole subtree.
 */
val LocalInkletMotion = compositionLocalOf { InkletMotion() }
val LocalInkletStyle = staticCompositionLocalOf { InkletStyle(animate = false) }
internal val LocalInkletReduceMotion = staticCompositionLocalOf { false }
internal val LocalSketchFrame =
    staticCompositionLocalOf<State<Int>> {
        object : State<Int> {
            override val value = 0
        }
    }

/**
 * Wrap once around the app: every control shares one three-frame clock. The clock honors the
 * platform motion-duration scale and holds still when animations are off; [reduceMotion] also
 * lets a host opt out.
 * [reduceMotion] also freezes loading indicators at a visible indeterminate pose. Pen settings
 * (including [InkletStyle.animate]) do not stop loading motion. No font or color scheme is
 * imposed. Outside this provider controls render a static sketch, with loading motion enabled.
 * [motion] configures selection transitions and is inherited by nested themes by default.
 * [holdBoil] pauses the boil while it returns true, keeping the current drawing, for example
 * while the user is touching or scrolling. It is read in a snapshot observer, not in
 * composition, so backing it with snapshot state pauses and resumes without recomposing.
 */
@Composable
fun InkletTheme(
    style: InkletStyle = InkletStyle(),
    reduceMotion: Boolean = false,
    motion: InkletMotion = LocalInkletMotion.current,
    holdBoil: () -> Boolean = { false },
    content: @Composable () -> Unit,
) {
    val effective = if (reduceMotion) style.copy(animate = false) else style
    val hold = rememberUpdatedState(holdBoil)
    val frameMillis = rememberUpdatedState(motion.boilFrameMillis)
    val frame =
        if (effective.animate && effective.boil > 0) {
            val pen = remember { mutableIntStateOf(0) }
            LaunchedEffect(pen) { pen.boil(hold, frameMillis) }
            pen
        } else {
            rememberUpdatedState(0)
        }
    CompositionLocalProvider(
        LocalInkletStyle provides effective,
        LocalInkletMotion provides motion,
        LocalSketchFrame provides frame,
        LocalInkletReduceMotion provides reduceMotion,
        content = content,
    )
}

/** The 0.3.1 signature, kept so code compiled against it still links. */
@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
fun InkletTheme(
    style: InkletStyle = InkletStyle(),
    reduceMotion: Boolean = false,
    motion: InkletMotion = LocalInkletMotion.current,
    content: @Composable () -> Unit,
) = InkletTheme(style, reduceMotion, motion, holdBoil = { false }, content = content)

private const val PEN_FRAMES = 3

/** Every clock steps on the same timeline, so nested themes change their drawings in one frame. */
private val penEpoch = TimeSource.Monotonic.markNow()

/**
 * Steps through the pen's drawings on a timer. An infinite transition would ask for a frame on
 * every vsync just to change the drawing three times per cycle; waiting instead leaves the app
 * idle between drawings. A zero [MotionDurationScale] holds the current drawing until it changes,
 * and so does [hold]; the drawing changes again on the first step after the hold ends. A new
 * [frameMillis] restarts the wait at the new pace instead of finishing the old one.
 */
private suspend fun MutableIntState.boil(
    hold: State<() -> Boolean>,
    frameMillis: State<Int>,
) {
    val motion = currentCoroutineContext()[MotionDurationScale]
    while (true) {
        val scale =
            when {
                motion == null -> 1f
                motion.scaleFactor > 0f -> motion.scaleFactor
                else -> snapshotFlow { motion.scaleFactor }.first { it > 0f }
            }
        val millis = frameMillis.value
        val period = (millis * scale).roundToLong().coerceAtLeast(1)
        val wait = period - penEpoch.elapsedNow().inWholeMilliseconds % period
        val repaced = withTimeoutOrNull(wait) { snapshotFlow { frameMillis.value }.first { it != millis } }
        if (repaced != null) continue
        if (hold.value()) {
            snapshotFlow { hold.value() }.first { !it }
            continue
        }
        intValue = (intValue + 1) % PEN_FRAMES
    }
}
