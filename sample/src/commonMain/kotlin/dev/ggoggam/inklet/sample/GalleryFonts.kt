package dev.ggoggam.inklet.sample

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

// Native targets use their system serif; Wasm provides a bundled font.
internal val LocalGallerySerif = staticCompositionLocalOf<FontFamily> { FontFamily.Serif }
