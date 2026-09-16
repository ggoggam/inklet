@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.ggoggam.inklet.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.InkletTheme
import org.jetbrains.skia.Bitmap
import java.io.File
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RippleTest {
    private val controls = listOf("radio", "checkbox", "toggle", "icon", "icon toggle", "button", "assist", "suggestion", "filter", "input")

    @Test
    fun pressedFeedbackFollowsEachControlsShapeInLightAndDarkThemes() {
        for (dark in listOf(false, true)) {
            for (control in controls.filter { it != "toggle" }) {
                withScene(control, dark = dark) { scene ->
                    val bounds = scene.controlNode().boundsInRoot
                    assertTrue(bounds.width >= 48 && bounds.height >= 48, "$control touch target")
                    val before = scene.colors(0)
                    scene.sendPointerEvent(PointerEventType.Press, bounds.center, button = PointerButton.Primary)
                    // Advance actual ripple animation frames while holding the pointer down.
                    for (frame in 1..30) scene.render(frame * 16_000_000L).close()
                    val pressed = scene.colors(496_000_000)
                    val radius =
                        when (control) {
                            "radio", "icon", "icon toggle" -> 24f
                            "checkbox" -> 6f
                            else -> 12f
                        }
                    var changed = 0
                    var outside = 0
                    for (y in 0 until bounds.height.toInt()) {
                        for (x in 0 until bounds.width.toInt()) {
                            val dx = (radius - minOf(x + 0.5f, bounds.width - x - 0.5f)).coerceAtLeast(0f)
                            val dy = (radius - minOf(y + 0.5f, bounds.height - y - 0.5f)).coerceAtLeast(0f)
                            val index = (bounds.top.toInt() + y) * 200 + bounds.left.toInt() + x
                            if (hypot(dx, dy) > radius + 1) {
                                assertEquals(before[index], pressed[index], "$control leaked feedback at ($x, $y), dark=$dark")
                                outside++
                            } else if (before[index] != pressed[index]) {
                                changed++
                            }
                        }
                    }
                    assertTrue(outside > 0, "$control must check pixels outside its shape")
                    assertTrue(changed > 20, "$control must show pressed feedback, dark=$dark")
                    scene.render(512_000_000).use { image ->
                        val file = File("build/reports/ripples/${if (dark) "dark" else "light"}-${control.replace(' ', '-')}.png")
                        file.parentFile.mkdirs()
                        image.encodeToData()!!.use { file.writeBytes(it.bytes) }
                    }
                    scene.sendPointerEvent(PointerEventType.Release, bounds.center, button = PointerButton.Primary)
                }
            }
        }
    }

    @Test
    fun switchFeedbackIsACircleCenteredOnTheThumbInBothStatesAndDirections() {
        for (dark in listOf(false, true)) {
            for (direction in LayoutDirection.entries) {
                for (checked in listOf(false, true)) {
                    for (event in listOf(PointerEventType.Move, PointerEventType.Press)) {
                        withScene("toggle", dark = dark, checked = checked, direction = direction) { scene ->
                            val bounds = scene.controlNode().boundsInRoot
                            val thumbOnRight = checked != (direction == LayoutDirection.Rtl)
                            val center = bounds.topLeft + Offset(if (thumbOnRight) 38f else 18f, 24f)
                            // Hover/press the far end: feedback must still originate at the thumb.
                            val press = bounds.topLeft + Offset(if (thumbOnRight) 8f else 48f, 24f)
                            val before = scene.colors(0)
                            val button = if (event == PointerEventType.Press) PointerButton.Primary else null
                            scene.sendPointerEvent(event, press, button = button)
                            for (frame in 1..30) scene.render(frame * 16_000_000L).close()
                            val pressed = scene.colors(496_000_000)
                            var haloPixels = 0
                            for (y in 0 until 100) {
                                for (x in 0 until 200) {
                                    val distance = hypot(x + 0.5f - center.x, y + 0.5f - center.y)
                                    val index = y * 200 + x
                                    if (distance > 21f) {
                                        // Focus can recolor the pen; only new coverage is feedback leakage.
                                        assertEquals(
                                            before[index] ushr 24,
                                            pressed[index] ushr 24,
                                            "Switch feedback outside thumb circle: checked=$checked, $direction, $event",
                                        )
                                    } else if (distance in 14f..18f && before[index] != pressed[index]) {
                                        haloPixels++
                                    }
                                }
                            }
                            assertTrue(
                                haloPixels > 200,
                                "Switch must show a circular halo around the thumb: checked=$checked, $direction, $event",
                            )
                            scene.render(512_000_000).use { image ->
                                val file =
                                    File("build/reports/ripples/${if (dark) "dark" else "light"}-toggle-$checked-$direction-$event.png")
                                file.parentFile.mkdirs()
                                image.encodeToData()!!.use { file.writeBytes(it.bytes) }
                            }
                            if (event == PointerEventType.Press) {
                                scene.sendPointerEvent(PointerEventType.Release, press, button = PointerButton.Primary)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun disabledControlsDoNotShowPressedFeedbackOrInvokeCallbacks() {
        for (control in controls) {
            var clicks = 0
            withScene(control, enabled = false, onClick = { clicks++ }) { scene ->
                val center = scene.controlNode().boundsInRoot.center
                val before = scene.colors(0)
                scene.sendPointerEvent(PointerEventType.Press, center, button = PointerButton.Primary)
                for (frame in 1..30) scene.render(frame * 16_000_000L).close()
                assertTrue(before.contentEquals(scene.colors(496_000_000)), "$control disabled feedback")
                scene.sendPointerEvent(PointerEventType.Release, center, button = PointerButton.Primary)
                assertEquals(0, clicks, "$control disabled callback")
            }
        }
    }

    @Test
    fun customControlsKeepTheirFullRectangularTouchTargets() {
        for (control in listOf("radio", "checkbox", "toggle")) {
            var clicks = 0
            withScene(control, onClick = { clicks++ }) { scene ->
                val bounds = scene.controlNode().boundsInRoot
                val corner = bounds.topLeft + Offset(1f, 1f)
                scene.sendPointerEvent(PointerEventType.Press, corner, button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, corner, button = PointerButton.Primary)
                assertEquals(1, clicks, "$control must remain clickable outside the feedback shape")
            }
        }
    }

    @Composable
    private fun Control(
        name: String,
        enabled: Boolean,
        checked: Boolean,
        onClick: () -> Unit,
    ) {
        val modifier = Modifier.semantics { contentDescription = "control" }
        val label: @Composable () -> Unit = { Box(Modifier.size(24.dp)) }
        when (name) {
            "radio" -> InkletRadioButton(false, onClick, modifier, enabled, seed = 42)
            "checkbox" -> InkletCheckbox(false, { onClick() }, modifier, enabled, seed = 42)
            "toggle" -> InkletToggle(checked, { onClick() }, modifier, enabled, seed = 42)
            "icon" -> InkletIconButton(onClick, modifier, enabled, seed = 42, content = label)
            "icon toggle" -> InkletIconToggleButton(false, { onClick() }, modifier, enabled, seed = 42, content = label)
            "button" -> InkletButton(onClick, modifier, enabled, cornerRadius = 12.dp, seed = 42) { label() }
            "assist" -> InkletAssistChip(onClick, label, modifier, enabled, cornerRadius = 12.dp, seed = 42)
            "suggestion" -> InkletSuggestionChip(onClick, label, modifier, enabled, cornerRadius = 12.dp, seed = 42)
            "filter" -> InkletFilterChip(false, onClick, label, modifier, enabled, cornerRadius = 12.dp, seed = 42)
            "input" -> InkletInputChip(false, onClick, label, modifier, enabled, cornerRadius = 12.dp, seed = 42)
        }
    }

    private fun ImageComposeScene.controlNode(): SemanticsNode {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }.single {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("control") == true
        }
    }

    private fun ImageComposeScene.colors(time: Long): IntArray =
        render(time).use { image ->
            Bitmap.makeFromImage(image).use { bitmap ->
                IntArray(200 * 100) { index -> bitmap.getColor(index % 200, index / 200) }
            }
        }

    private fun withScene(
        control: String,
        dark: Boolean = false,
        enabled: Boolean = true,
        checked: Boolean = false,
        direction: LayoutDirection = LayoutDirection.Ltr,
        onClick: () -> Unit = {},
        test: (ImageComposeScene) -> Unit,
    ) {
        val scene =
            ImageComposeScene(200, 100) {
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    InkletTheme(reduceMotion = true) {
                        CompositionLocalProvider(LocalLayoutDirection provides direction) {
                            Box(Modifier.padding(16.dp)) { Control(control, enabled, checked, onClick) }
                        }
                    }
                }
            }
        try {
            scene.render(0).close()
            test(scene)
        } finally {
            scene.close()
        }
    }
}
