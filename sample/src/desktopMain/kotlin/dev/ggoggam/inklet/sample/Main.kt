@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.ggoggam.inklet.sample

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File

fun main(args: Array<String>) {
    if (args.firstOrNull() == "--snapshot") {
        val width = args.getOrNull(2)?.toInt() ?: 1120
        val scene =
            ImageComposeScene(width, args.getOrNull(3)?.toIntOrNull() ?: 1040) {
                Gallery(
                    static = true,
                    initialDark =
                        "--dark" in args,
                )
            }
        try {
            scene.render().close()
            scene.render().use { image ->
                image.encodeToData()!!.use { data -> File(args.getOrElse(1) { "inklet.png" }).writeBytes(data.bytes) }
            }
        } finally {
            scene.close()
        }
    } else {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Inklet · a little less perfect",
                state = rememberWindowState(width = 1120.dp, height = 960.dp),
            ) {
                Gallery()
            }
        }
    }
}
