package dev.ggoggam.inklet

import androidx.compose.ui.node.CompositionLocalConsumerModifierNode

internal actual fun CompositionLocalConsumerModifierNode.windowRedraw(): (() -> Unit)? = null
