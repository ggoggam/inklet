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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

/**
 * Shared pen settings. [roughness] controls the base drawing's irregularity; zero is smooth.
 * [boil] controls frame-to-frame displacement in logical dp, independently of roughness.
 * Small indicators attenuate both amounts to keep their shapes legible. Boil is an amplitude,
 * not a speed: the theme cycles through three drawings every 1200ms. [animate] = false or
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
 * Selection animation specs. Customize duration and easing with tween, use spring for a
 * spring response, or snap to disable a transition. [toggle] drives both thumb and shading.
 * Reduced motion takes precedence over these specs. Pen boil and loading are independent.
 */
@Immutable
data class InkletMotion(
    val checkbox: FiniteAnimationSpec<Float> = tween(200, easing = LinearEasing),
    val toggle: FiniteAnimationSpec<Float> = tween(180),
)

val LocalInkletMotion = staticCompositionLocalOf { InkletMotion() }
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
 */
@Composable
fun InkletTheme(
    style: InkletStyle = InkletStyle(),
    reduceMotion: Boolean = false,
    motion: InkletMotion = LocalInkletMotion.current,
    content: @Composable () -> Unit,
) {
    val effective = if (reduceMotion) style.copy(animate = false) else style
    val frame =
        if (effective.animate && effective.boil > 0) {
            val pen = remember { mutableIntStateOf(0) }
            LaunchedEffect(pen) { pen.boil() }
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

private const val PEN_FRAMES = 3
private const val PEN_FRAME_MILLIS = 400L

/**
 * Steps through the pen's drawings on a timer. An infinite transition would ask for a frame on
 * every vsync just to change the drawing three times per cycle; waiting instead leaves the app
 * idle between drawings. A zero [MotionDurationScale] holds the current drawing until it changes.
 */
private suspend fun MutableIntState.boil() {
    val motion = currentCoroutineContext()[MotionDurationScale]
    while (true) {
        val scale =
            when {
                motion == null -> 1f
                motion.scaleFactor > 0f -> motion.scaleFactor
                else -> snapshotFlow { motion.scaleFactor }.first { it > 0f }
            }
        delay((PEN_FRAME_MILLIS * scale).roundToLong())
        intValue = (intValue + 1) % PEN_FRAMES
    }
}
