package dev.ggoggam.inklet

import androidx.compose.ui.node.CompositionLocalConsumerModifierNode

/**
 * Returns a way to make this node's window draw again without invalidating any recorded drawing,
 * after the node changed a property of one of its own graphics layers. Returns null where the
 * platform cannot show such a change that way; the caller then invalidates its own drawing.
 */
internal expect fun CompositionLocalConsumerModifierNode.windowRedraw(): (() -> Unit)?
