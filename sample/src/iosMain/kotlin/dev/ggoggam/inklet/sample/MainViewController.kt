package dev.ggoggam.inklet.sample

import androidx.compose.ui.window.ComposeUIViewController

@Suppress("FunctionName")
fun MainViewController(onDarkChanged: (Boolean) -> Unit) = ComposeUIViewController { Gallery(onDarkChanged = onDarkChanged) }
