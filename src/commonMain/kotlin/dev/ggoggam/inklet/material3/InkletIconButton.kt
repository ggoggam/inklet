package dev.ggoggam.inklet.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.PenShape
import dev.ggoggam.inklet.sketch

/**
 * Circular pen button with native focus, keyboard, ripple and disabled behavior.
 * Label the icon or this button with a content description. Content is typically a 24dp icon.
 */
@Composable
fun InkletIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: InkletVariant = InkletVariant.Outline,
    colors: IconButtonColors =
        if (variant ==
            InkletVariant.Solid
        ) {
            IconButtonDefaults.filledIconButtonColors()
        } else {
            IconButtonDefaults.outlinedIconButtonColors()
        },
    interactionSource: MutableInteractionSource? = null,
    seed: Int? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val fill = if (enabled) colors.containerColor else colors.disabledContainerColor
    val ink = if (enabled) colors.contentColor else colors.disabledContentColor
    IconButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.iconSketch(enabled, fill, ink, variant, source, seed),
        enabled = enabled,
        colors = colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        interactionSource = source,
        shape = CircleShape,
        content = content,
    )
}

/** State-hoisted icon toggle with native checkbox semantics; label it at the call site. */
@Composable
fun InkletIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: InkletVariant = InkletVariant.Outline,
    colors: IconToggleButtonColors =
        if (variant ==
            InkletVariant.Solid
        ) {
            IconButtonDefaults.filledIconToggleButtonColors()
        } else {
            IconButtonDefaults.outlinedIconToggleButtonColors()
        },
    interactionSource: MutableInteractionSource? = null,
    seed: Int? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val fill =
        when {
            !enabled -> colors.disabledContainerColor
            checked -> colors.checkedContainerColor
            else -> colors.containerColor
        }
    val ink =
        when {
            !enabled -> colors.disabledContentColor
            checked -> colors.checkedContentColor
            else -> colors.contentColor
        }
    IconToggleButton(
        checked = checked,
        onCheckedChange = { if (enabled) onCheckedChange(it) },
        modifier = modifier.iconSketch(enabled, fill, ink, variant, source, seed),
        enabled = enabled,
        colors =
            colors.copy(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                checkedContainerColor = Color.Transparent,
            ),
        interactionSource = source,
        shape = CircleShape,
        content = content,
    )
}

@Composable
private fun Modifier.iconSketch(
    enabled: Boolean,
    fill: Color,
    ink: Color,
    variant: InkletVariant,
    source: MutableInteractionSource,
    seed: Int?,
): Modifier {
    val focused by source.collectIsFocusedAsState()
    return defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).sketch(
        shape = PenShape.Ellipse,
        ink = if (enabled && focused) MaterialTheme.colorScheme.primary else ink,
        fill = fill,
        scribble = variant == InkletVariant.Scribble,
        seed = interactionSeed(source, seed),
    )
}
