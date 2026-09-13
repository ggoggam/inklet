package dev.ggoggam.inklet.material3

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.InkletDecoration
import dev.ggoggam.inklet.inkletBorder as coreInkletBorder
import dev.ggoggam.inklet.inkletDecoration as coreInkletDecoration
import dev.ggoggam.inklet.inkletSurface as coreInkletSurface

/** Draw a pen outline using Material's outline color by default. */
@Composable
fun Modifier.inkletBorder(
    color: Color = MaterialTheme.colorScheme.outline,
    cornerRadius: Dp = 12.dp,
    seed: Int? = null,
): Modifier = coreInkletBorder(color, cornerRadius, seed)

/**
 * Sketch a container using Material's surface and outline colors by default.
 * Set the host component's own container and border colors to transparent.
 */
@Composable
fun Modifier.inkletSurface(
    containerColor: Color = MaterialTheme.colorScheme.surface,
    ink: Color = MaterialTheme.colorScheme.outline,
    cornerRadius: Dp = 12.dp,
    scribble: Boolean = false,
    seed: Int? = null,
): Modifier = coreInkletSurface(containerColor, ink, cornerRadius, scribble, seed)

/** Decorate a single label/block using Material's primary color by default. */
@Composable
fun Modifier.inkletDecoration(
    decoration: InkletDecoration,
    color: Color = MaterialTheme.colorScheme.primary,
    seed: Int? = null,
): Modifier = coreInkletDecoration(decoration, color, seed)
