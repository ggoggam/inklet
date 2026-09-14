package dev.ggoggam.inklet.sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.ComposeViewport
import inklet.sample.generated.resources.Lora_Bold
import inklet.sample.generated.resources.Lora_Regular
import inklet.sample.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "inklet") {
        val regular by preloadFont(Res.font.Lora_Regular)
        val bold by preloadFont(Res.font.Lora_Bold, FontWeight.Bold)
        if (regular != null && bold != null) {
            val serif = remember(regular, bold) { FontFamily(listOfNotNull(regular, bold)) }
            CompositionLocalProvider(LocalGallerySerif provides serif) {
                Gallery()
            }
        }
    }
}
