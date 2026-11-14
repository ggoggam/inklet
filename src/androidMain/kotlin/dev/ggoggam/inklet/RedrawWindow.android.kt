package dev.ggoggam.inklet

import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalView

/**
 * A GraphicsLayer is a RenderNode here, which the parent's display list draws by reference, so a
 * changed layer property shows up on the next frame. Invalidating the view only re-records the
 * reference to Compose's root layer, not the content between the root and the node.
 */
internal actual fun CompositionLocalConsumerModifierNode.windowRedraw(): (() -> Unit)? = currentValueOf(LocalView)::invalidate
