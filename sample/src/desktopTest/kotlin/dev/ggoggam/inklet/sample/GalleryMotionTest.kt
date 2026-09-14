@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.ggoggam.inklet.sample

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import org.jetbrains.skia.Bitmap
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

class GalleryMotionTest {
    @Test
    fun motionSettingSlidesItsOwnThumbInBothDirections() {
        val scene = ImageComposeScene(1120, 1040) { Gallery() }
        var time = 0L

        fun frame() {
            Snapshot.sendApplyNotifications()
            scene.render(time).close()
            time += 16_000_000
        }

        fun thumbCenter(): Double {
            val bounds = scene.node("A little motion").boundsInRoot
            return scene.render(time).use { image ->
                Bitmap.makeFromImage(image).use { bitmap ->
                    // Sample the solid thumb through its middle, excluding the track's border.
                    val xs =
                        (6..49).filter { x ->
                            val color = bitmap.getColor(bounds.left.roundToInt() + x, bounds.center.y.roundToInt())
                            color == 0xFF994C32.toInt() || color == 0xFF6D7064.toInt()
                        }
                    assertTrue(xs.isNotEmpty(), "The switch thumb should be visible")
                    xs.average()
                }
            }
        }

        try {
            frame()
            // Remove pen movement so only the switch's sliding motion affects the measurement.
            for (label in listOf("Roughness", "Boil")) {
                scene
                    .node(label)
                    .config[SemanticsActions.SetProgress]
                    .action!!
                    .invoke(0f)
            }
            repeat(20) { frame() }
            val on = thumbCenter()
            scene
                .node("A little motion")
                .config[SemanticsActions.OnClick]
                .action!!
                .invoke()
            repeat(5) { frame() }
            val turningOff = thumbCenter()
            repeat(20) { frame() }
            val off = thumbCenter()
            assertTrue(on - off > 15, "The thumb should travel across the track")
            assertTrue(turningOff > off + 1 && turningOff < on - 1, "Turning motion off should visibly slide the thumb")

            scene
                .node("A little motion")
                .config[SemanticsActions.OnClick]
                .action!!
                .invoke()
            repeat(5) { frame() }
            val turningOn = thumbCenter()
            assertTrue(turningOn > off + 1 && turningOn < on - 1, "Turning motion on should visibly slide the thumb")
            repeat(20) { frame() }
            assertTrue(kotlin.math.abs(thumbCenter() - on) < 1, "The thumb should return to the on position")
        } finally {
            scene.close()
        }
    }

    private fun ImageComposeScene.node(label: String): SemanticsNode {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }.single {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true
        }
    }
}
