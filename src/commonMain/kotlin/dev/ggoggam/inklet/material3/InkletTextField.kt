package dev.ggoggam.inklet.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ggoggam.inklet.InkletTextFieldContainer
import dev.ggoggam.inklet.LocalInkletStyle

/**
 * String-based native editing with Material's floating label, outline cutout and content slots.
 * [colors] also controls the pen outline (the indicator colors), cursor and selection.
 * [errorMessage] is announced when [isError]; supply localized, specific validation text.
 * Supporting text is outside the outline. [singleLine] defaults to true for compatibility.
 * Inklet reduced motion freezes the pen; Material label transitions follow platform motion scale.
 */
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
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding(),
    errorMessage: String = "Invalid input",
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val textColor =
        textStyle.color.takeOrElse {
            when {
                !enabled -> colors.disabledTextColor
                isError -> colors.errorTextColor
                focused -> colors.focusedTextColor
                else -> colors.unfocusedTextColor
            }
        }
    val ink =
        when {
            !enabled -> colors.disabledIndicatorColor
            isError -> colors.errorIndicatorColor
            focused -> colors.focusedIndicatorColor
            else -> colors.unfocusedIndicatorColor
        }
    val fill =
        when {
            !enabled -> colors.disabledContainerColor
            isError -> colors.errorContainerColor
            focused -> colors.focusedContainerColor
            else -> colors.unfocusedContainerColor
        }
    val style = LocalInkletStyle.current
    val penMargin = (2.1 * style.roughness + (if (style.animate) style.boil else 0.0) + style.strokeWidth.value / 2).dp
    val labelLineHeight =
        MaterialTheme.typography.bodySmall.lineHeight
            .let { if (it.isSp) it else 16.sp }
    val labelMargin = with(LocalDensity.current) { labelLineHeight.toDp() / 2 }
    val topPadding = maxOf(penMargin, if (label != null) labelMargin else 0.dp)
    val sketchSeed = interactionSeed(source, seed)
    val labelBounds = remember(label != null) { TextFieldLabelBounds() }

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                modifier
                    .semantics(mergeDescendants = true) { if (isError) error(errorMessage) }
                    .padding(top = topPadding)
                    .defaultMinSize(OutlinedTextFieldDefaults.MinWidth, OutlinedTextFieldDefaults.MinHeight),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = source,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    interactionSource = source,
                    isError = isError,
                    label =
                        label?.let { content ->
                            { Box(Modifier.onGloballyPositioned { labelBounds.label = it }) { content() } }
                        },
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    colors = colors,
                    contentPadding = contentPadding,
                    container = {
                        InkletTextFieldContainer(
                            ink,
                            fill,
                            cornerRadius,
                            sketchSeed,
                            modifier = Modifier.onGloballyPositioned { labelBounds.container = it },
                            labelBounds = { labelBounds.bounds },
                        )
                    },
                )
            },
        )
    }
}

/** Layout updates invalidate drawing only; glyph bounds never become cached pen geometry. */
private class TextFieldLabelBounds {
    var bounds by mutableStateOf<Rect?>(null)
        private set
    var label: LayoutCoordinates? = null
        set(value) {
            field = value
            update()
        }
    var container: LayoutCoordinates? = null
        set(value) {
            field = value
            update()
        }

    private fun update() {
        val label = label
        val container = container
        bounds =
            if (label?.isAttached == true && container?.isAttached == true) {
                container.localBoundingBoxOf(label, clipBounds = false)
            } else {
                null
            }
    }
}
