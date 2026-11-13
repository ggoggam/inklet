@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.ggoggam.inklet.material3

import androidx.compose.ui.ImageComposeScene

/** Longer than one pen drawing (400ms), for checks that the boil holds still. */
internal const val BOIL_WAIT_MILLIS = 600L

/**
 * Waits for the pen's next drawing. Boil advances on a wall-clock delay rather than on the
 * scene's frame times, and requests no frame until then.
 */
internal fun ImageComposeScene.awaitBoil() {
    val deadline = System.nanoTime() + 5_000_000_000L
    while (!hasInvalidations()) {
        check(System.nanoTime() < deadline) { "Boil never advanced" }
        Thread.sleep(10)
    }
}
