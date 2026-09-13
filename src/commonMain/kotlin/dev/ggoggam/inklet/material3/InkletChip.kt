package dev.ggoggam.inklet.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colors shared by the pen container and native label/icon slots of selectable chips. */
@Immutable
data class InkletSelectableChipColors(
    val containerColor: Color,
    val labelColor: Color,
    val leadingIconColor: Color,
    val trailingIconColor: Color,
    val disabledContainerColor: Color,
    val disabledLabelColor: Color,
    val disabledLeadingIconColor: Color,
    val disabledTrailingIconColor: Color,
    val selectedContainerColor: Color,
    val disabledSelectedContainerColor: Color,
    val selectedLabelColor: Color,
    val selectedLeadingIconColor: Color,
    val selectedTrailingIconColor: Color,
) {
    internal fun container(
        enabled: Boolean,
        selected: Boolean,
    ): Color =
        when {
            !enabled -> if (selected) disabledSelectedContainerColor else disabledContainerColor
            selected -> selectedContainerColor
            else -> containerColor
        }

    internal fun label(
        enabled: Boolean,
        selected: Boolean,
    ): Color =
        when {
            !enabled -> disabledLabelColor
            selected -> selectedLabelColor
            else -> labelColor
        }

    internal fun nativeColors(): SelectableChipColors =
        SelectableChipColors(
            containerColor = Color.Transparent,
            labelColor = labelColor,
            leadingIconColor = leadingIconColor,
            trailingIconColor = trailingIconColor,
            disabledContainerColor = Color.Transparent,
            disabledLabelColor = disabledLabelColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            selectedContainerColor = Color.Transparent,
            disabledSelectedContainerColor = Color.Transparent,
            selectedLabelColor = selectedLabelColor,
            selectedLeadingIconColor = selectedLeadingIconColor,
            selectedTrailingIconColor = selectedTrailingIconColor,
        )
}

object InkletChipDefaults {
    /** Use [InkletSelectableChipColors.copy] to customize individual states or icon slots. */
    @Composable
    fun selectableChipColors(): InkletSelectableChipColors {
        val scheme = MaterialTheme.colorScheme
        val disabled = scheme.onSurface.copy(alpha = 0.38f)
        return InkletSelectableChipColors(
            containerColor = Color.Transparent,
            labelColor = scheme.onSurfaceVariant,
            leadingIconColor = scheme.onSurfaceVariant,
            trailingIconColor = scheme.onSurfaceVariant,
            disabledContainerColor = Color.Transparent,
            disabledLabelColor = disabled,
            disabledLeadingIconColor = disabled,
            disabledTrailingIconColor = disabled,
            selectedContainerColor = scheme.secondaryContainer,
            disabledSelectedContainerColor = scheme.onSurface.copy(alpha = 0.12f),
            selectedLabelColor = scheme.onSecondaryContainer,
            selectedLeadingIconColor = scheme.onSecondaryContainer,
            selectedTrailingIconColor = scheme.onSecondaryContainer,
        )
    }
}

/** Flat sketched assist chip; Material owns layout, focus, ripple and button semantics.
 * Content slots retain their native colors and accessibility.
 */
@Composable
fun InkletAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = 8.dp,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    interactionSource: MutableInteractionSource? = null,
    seed: Int? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    AssistChip(
        onClick = { if (enabled) onClick() },
        label = label,
        modifier =
            modifier.chipSketch(
                enabled = enabled,
                fill = if (enabled) colors.containerColor else colors.disabledContainerColor,
                ink = if (enabled) colors.labelColor else colors.disabledLabelColor,
                cornerRadius = cornerRadius,
                source = source,
                seed = seed,
            ),
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(cornerRadius),
        colors = colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        elevation = null,
        border = null,
        interactionSource = source,
    )
}

/** Flat sketched suggestion chip; Material owns layout, focus, ripple and button semantics.
 * Content slots retain their native colors and accessibility.
 */
@Composable
fun InkletSuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = 8.dp,
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
    interactionSource: MutableInteractionSource? = null,
    seed: Int? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    SuggestionChip(
        onClick = { if (enabled) onClick() },
        label = label,
        modifier =
            modifier.chipSketch(
                enabled = enabled,
                fill = if (enabled) colors.containerColor else colors.disabledContainerColor,
                ink = if (enabled) colors.labelColor else colors.disabledLabelColor,
                cornerRadius = cornerRadius,
                source = source,
                seed = seed,
            ),
        enabled = enabled,
        icon = icon,
        shape = RoundedCornerShape(cornerRadius),
        colors = colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        elevation = null,
        border = null,
        interactionSource = source,
    )
}

/** Flat sketched filter chip; Material owns layout, focus, ripple and selection semantics.
 * Selection is hoisted; provide a leading icon when a selected checkmark is desired.
 */
@Composable
fun InkletFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = 8.dp,
    colors: InkletSelectableChipColors = InkletChipDefaults.selectableChipColors(),
    interactionSource: MutableInteractionSource? = null,
    seed: Int? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    FilterChip(
        selected = selected,
        onClick = { if (enabled) onClick() },
        label = label,
        modifier =
            modifier.chipSketch(
                enabled = enabled,
                fill = colors.container(enabled, selected),
                ink = colors.label(enabled, selected),
                cornerRadius = cornerRadius,
                source = source,
                seed = seed,
            ),
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(cornerRadius),
        colors = colors.nativeColors(),
        elevation = null,
        border = null,
        interactionSource = source,
    )
}

/** Flat sketched input chip; Material owns layout, focus, ripple and selection semantics.
 * Content slots retain their native colors and accessibility.
 */
@Composable
fun InkletInputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    avatar: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = 8.dp,
    colors: InkletSelectableChipColors = InkletChipDefaults.selectableChipColors(),
    interactionSource: MutableInteractionSource? = null,
    seed: Int? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    InputChip(
        selected = selected,
        onClick = { if (enabled) onClick() },
        label = label,
        modifier =
            modifier.chipSketch(
                enabled = enabled,
                fill = colors.container(enabled, selected),
                ink = colors.label(enabled, selected),
                cornerRadius = cornerRadius,
                source = source,
                seed = seed,
            ),
        enabled = enabled,
        leadingIcon = leadingIcon,
        avatar = avatar,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(cornerRadius),
        colors = colors.nativeColors(),
        elevation = null,
        border = null,
        interactionSource = source,
    )
}

@Composable
private fun Modifier.chipSketch(
    enabled: Boolean,
    fill: Color,
    ink: Color,
    cornerRadius: Dp,
    source: MutableInteractionSource,
    seed: Int?,
): Modifier {
    val focused by source.collectIsFocusedAsState()
    // Keep the drawing and native hit region together, with a 48dp minimum height.
    return defaultMinSize(minHeight = 48.dp).inkletSurface(
        containerColor = fill,
        ink = if (enabled && focused) MaterialTheme.colorScheme.primary else ink,
        cornerRadius = cornerRadius,
        seed = interactionSeed(source, seed),
    )
}
