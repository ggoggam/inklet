package dev.ggoggam.inklet.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.PenShape
import dev.ggoggam.inklet.sketch

/**
 * Continuous Material slider with pen-drawn track and thumb. Material owns dragging, keyboard
 * input, RTL, focus and accessibility progress actions. Label it at the call site.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkletSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    seed: Int? = null,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val active = if (enabled) colors.activeTrackColor else colors.disabledActiveTrackColor
    val inactive = if (enabled) colors.inactiveTrackColor else colors.disabledInactiveTrackColor
    val thumb = if (enabled) colors.thumbColor else colors.disabledThumbColor
    Slider(
        value = value,
        onValueChange = { if (enabled) onValueChange(it) },
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        onValueChangeFinished = { if (enabled) onValueChangeFinished?.invoke() },
        interactionSource = interactionSource,
        thumb = {
            Box(Modifier.size(28.dp, 48.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(28.dp).sketch(
                        PenShape.Ellipse,
                        if (focused) MaterialTheme.colorScheme.onSurface else thumb,
                        fill = thumb,
                        seed = seed,
                    ),
                )
            }
        },
        track = { state ->
            val length = valueRange.endInclusive - valueRange.start
            val fraction = if (length > 0f) ((state.value - valueRange.start) / length).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(12.dp)) {
                Box(Modifier.fillMaxWidth().height(12.dp).sketch(PenShape.Line, inactive, seed = seed))
                if (fraction > 0f) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(fraction)
                            .height(12.dp)
                            .sketch(PenShape.Line, active, seed = seed),
                    )
                }
            }
        },
    )
}
