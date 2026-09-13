package dev.ggoggam.inklet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared pen settings. [roughness] controls the base drawing's irregularity; zero is smooth.
 * [boil] controls frame-to-frame displacement in logical dp, independently of roughness.
 * Small indicators attenuate both amounts to keep their shapes legible. Boil is an amplitude,
 * not a speed: the theme cycles through three drawings every 1200ms. [animate] = false or
 * the theme's reduceMotion flag disables boil and interaction re-sketching.
 */
@Immutable
data class InkletStyle(
    val roughness: Double = 1.0,
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

val LocalInkletStyle = staticCompositionLocalOf { InkletStyle(animate = false) }
internal val LocalInkletReduceMotion = staticCompositionLocalOf { false }
internal val LocalSketchFrame =
    staticCompositionLocalOf<State<Int>> {
        object : State<Int> {
            override val value = 0
        }
    }

/**
 * Wrap once around the app: every control shares one three-frame clock. Compose's animation
 * clock honors the platform motion-duration scale; [reduceMotion] also lets a host opt out.
 * [reduceMotion] also freezes loading indicators at a visible indeterminate pose. Pen settings
 * (including [InkletStyle.animate]) do not stop loading motion. No font or color scheme is
 * imposed. Outside this provider controls render a static sketch, with loading motion enabled.
 */
@Composable
fun InkletTheme(
    style: InkletStyle = InkletStyle(),
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val effective = if (reduceMotion) style.copy(animate = false) else style
    val frame =
        if (effective.animate && effective.boil > 0) {
            val transition = rememberInfiniteTransition(label = "Inklet pen")
            val phase =
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 3f,
                    animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                    label = "Pen frame",
                )
            // Draw observers invalidate only at the three frame boundaries, not at display refresh rate.
            remember(phase) { derivedStateOf { phase.value.toInt() % 3 } }
        } else {
            rememberUpdatedState(0)
        }
    CompositionLocalProvider(
        LocalInkletStyle provides effective,
        LocalSketchFrame provides frame,
        LocalInkletReduceMotion provides reduceMotion,
        content = content,
    )
}
