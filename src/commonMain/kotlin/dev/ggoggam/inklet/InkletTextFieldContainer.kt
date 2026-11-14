package dev.ggoggam.inklet

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** Align the top pen stroke with Material's label cutout, even at large roughness. */
@Composable
internal fun InkletTextFieldContainer(
    ink: Color,
    fill: Color,
    cornerRadius: Dp,
    seed: Int,
    modifier: Modifier = Modifier,
    labelBounds: () -> Rect? = { null },
) {
    Box(modifier.sketch(PenShape.Field, ink, fill, cornerRadius, seed = seed, labelBounds = labelBounds))
}
