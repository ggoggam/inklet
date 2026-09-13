package dev.ggoggam.inklet.material3

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.LocalInkletStyle
import dev.ggoggam.inklet.PenShape
import dev.ggoggam.inklet.sketch
import kotlin.random.Random

enum class InkletVariant { Outline, Solid, Scribble }

@Composable
private fun interactionSeed(
    source: MutableInteractionSource,
    seed: Int?,
): Int {
    val base = remember(seed) { seed ?: Random.nextInt() }
    var revision by remember(base) { mutableIntStateOf(0) }
    val animate = LocalInkletStyle.current.animate
    LaunchedEffect(source, base, animate) {
        if (animate) {
            source.interactions.collect {
                if (it is PressInteraction.Press || it is HoverInteraction.Enter || it is FocusInteraction.Focus) revision++
            }
        }
    }
    return base + revision
}

/** Keeps Material's focus, keyboard activation, disabled state, ripple, and button semantics. */
@Composable
fun InkletButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: InkletVariant = InkletVariant.Solid,
    cornerRadius: Dp = 12.dp,
    colors: ButtonColors = if (variant == InkletVariant.Solid) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    seed: Int? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val ink =
        when {
            !enabled -> colors.disabledContentColor
            focused -> MaterialTheme.colorScheme.onSurface
            variant == InkletVariant.Solid && colors.containerColor.alpha > 0f -> colors.containerColor
            else -> colors.contentColor
        }
    Button(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier =
            modifier.defaultMinSize(minHeight = 48.dp).sketch(
                PenShape.Rectangle,
                ink,
                fill = if (enabled) colors.containerColor else colors.disabledContainerColor,
                cornerRadius = cornerRadius,
                scribble = variant == InkletVariant.Scribble,
                seed = interactionSeed(interactions, seed),
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = colors.copy(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        elevation = null,
        interactionSource = interactions,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun InkletCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    ink: Color = MaterialTheme.colorScheme.outlineVariant,
    cornerRadius: Dp = 12.dp,
    seed: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier.sketch(PenShape.Rectangle, ink, containerColor, cornerRadius, seed = seed),
            content = content,
        )
    }
}

@Composable
fun InkletBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    scribble: Boolean = false,
    seed: Int? = null,
) {
    Text(
        text,
        modifier
            .sketch(PenShape.Rectangle, color, cornerRadius = 6.dp, scribble = scribble, seed = seed)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
    )
}

/** Label this control at the call site, or merge it into a labelled toggleable row. */
@Composable
fun InkletCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    seed: Int? = null,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ink = controlInk(enabled, checked || focused)
    val sketchSeed = interactionSeed(source, seed)
    Box(
        modifier.size(48.dp).toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            interactionSource = source,
            indication = androidx.compose.foundation.LocalIndication.current,
            onValueChange = { if (enabled) onCheckedChange(it) },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(28.dp).sketch(PenShape.Rectangle, ink, cornerRadius = 3.dp, seed = sketchSeed))
        if (checked) Box(Modifier.size(20.dp, 18.dp).sketch(PenShape.Check, ink, seed = sketchSeed + 1))
    }
}

@Composable
fun InkletRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    seed: Int? = null,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ink = controlInk(enabled, selected || focused)
    val sketchSeed = interactionSeed(source, seed)
    Box(
        modifier.size(48.dp).selectable(
            selected,
            enabled = enabled,
            role = Role.RadioButton,
            interactionSource = source,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = { if (enabled) onClick() },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(30.dp).sketch(PenShape.Ellipse, ink, seed = sketchSeed))
        if (selected) Box(Modifier.size(18.dp).sketch(PenShape.Dot, ink, ink, seed = sketchSeed + 1))
    }
}

@Composable
fun InkletToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    seed: Int? = null,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ink = controlInk(enabled, checked || focused)
    val sketchSeed = interactionSeed(source, seed)
    Box(
        modifier.size(56.dp, 48.dp).toggleable(
            checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = source,
            indication = androidx.compose.foundation.LocalIndication.current,
            onValueChange = { if (enabled) onCheckedChange(it) },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(52.dp, 32.dp).sketch(PenShape.Rectangle, ink, cornerRadius = 16.dp, seed = sketchSeed)) {
            Box(
                Modifier
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 4.dp)
                    .size(24.dp)
                    .sketch(PenShape.Ellipse, ink, ink, seed = sketchSeed),
            )
        }
    }
}

@Composable
private fun controlInk(
    enabled: Boolean,
    active: Boolean,
): Color =
    (if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        .let { if (enabled) it else it.copy(alpha = it.alpha * 0.38f) }

@Composable
fun InkletDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    seed: Int? = null,
) {
    Box(modifier.fillMaxWidth().height(8.dp).sketch(PenShape.Line, color, seed = seed))
}

/** Native editable field; multiline input is supported with singleLine = false. */
@Composable
fun InkletTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = true,
    placeholder: (@Composable () -> Unit)? = null,
    seed: Int? = null,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ink = if (isError) MaterialTheme.colorScheme.error else controlInk(enabled, focused)
    OutlinedTextField(
        value,
        onValueChange,
        modifier.inkletBorder(ink, seed = interactionSeed(source, seed)),
        enabled = enabled,
        isError = isError,
        singleLine = singleLine,
        placeholder = placeholder,
        interactionSource = source,
        shape = RoundedCornerShape(12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
            ),
    )
}
