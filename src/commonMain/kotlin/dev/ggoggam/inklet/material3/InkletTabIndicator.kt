package dev.ggoggam.inklet.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.PenShape
import dev.ggoggam.inklet.sketch

/**
 * A selection underline for Material tab rows and other selection hosts. In a tab row's
 * indicator slot, pass `Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true)`
 * for a content-width line, or `matchContentSize = false` for a full-tab line.
 *
 * The host owns positioning, selection, semantics and transition motion. This draws a cached
 * pen line in an 8dp-high area, aligned with [InkletDivider] in the divider slot. Override the
 * height in [modifier] if a larger pen needs more room. Reduced motion freezes the pen;
 * native tab movement and scrolling continue to follow the platform motion-duration scale.
 */
@Composable
fun InkletTabIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    seed: Int? = null,
) {
    Box(modifier.fillMaxWidth().height(8.dp).sketch(PenShape.Line, color, seed = seed))
}
