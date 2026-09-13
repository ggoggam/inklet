@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.ggoggam.inklet.material3

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import dev.ggoggam.inklet.InkletStyle
import dev.ggoggam.inklet.InkletTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControlsTest {
    @Test
    fun nativeActionsPreserveButtonDisabledCheckboxAndEditableTextBehavior() {
        var clicks = 0
        var checked by mutableStateOf(false)
        var text by mutableStateOf("")
        withScene({
            Column {
                InkletButton({ clicks++ }, labelled("save")) { Text("Save") }
                InkletButton({ clicks++ }, labelled("disabled"), enabled = false) { Text("Disabled") }
                InkletCheckbox(checked, { checked = it }, labelled("wish"))
                InkletTextField(text, { text = it }, labelled("note"))
            }
        }) { scene ->
            val save = scene.node("save")
            assertEquals(Role.Button, save.config[SemanticsProperties.Role])
            assertTrue(save.config[SemanticsActions.OnClick].action!!.invoke())
            assertEquals(1, clicks)

            val disabled = scene.node("disabled")
            assertTrue(disabled.config.contains(SemanticsProperties.Disabled))
            disabled.config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1, clicks)

            val wish = scene.node("wish")
            assertEquals(Role.Checkbox, wish.config[SemanticsProperties.Role])
            assertEquals(ToggleableState.Off, wish.config[SemanticsProperties.ToggleableState])
            assertTrue(wish.boundsInRoot.width >= 48 && wish.boundsInRoot.height >= 48)
            wish.config[SemanticsActions.OnClick].action!!.invoke()
            assertTrue(checked)

            val note = scene.node("note")
            assertTrue(note.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("A Sunday picnic")))
            assertEquals("A Sunday picnic", text)
            scene.render().close()
            assertEquals(ToggleableState.On, scene.node("wish").config[SemanticsProperties.ToggleableState])
        }
    }

    @Test
    fun radioAndSwitchExposeTheirRolesAndStateWithoutDecorativeSemanticsNodes() {
        var selected = false
        var on = false
        withScene({
            Column {
                InkletRadioButton(false, { selected = true }, labelled("us"))
                InkletToggle(false, { on = it }, labelled("motion"))
                InkletToggle(false, { on = it }, labelled("locked"), enabled = false)
            }
        }) { scene ->
            val radio = scene.node("us")
            assertEquals(Role.RadioButton, radio.config[SemanticsProperties.Role])
            assertFalse(radio.config[SemanticsProperties.Selected])
            radio.config[SemanticsActions.OnClick].action!!.invoke()
            assertTrue(selected)
            val toggle = scene.node("motion")
            assertEquals(Role.Switch, toggle.config[SemanticsProperties.Role])
            scene
                .node("locked")
                .config[SemanticsActions.OnClick]
                .action!!
                .invoke()
            assertFalse(on)
            toggle.config[SemanticsActions.OnClick].action!!.invoke()
            assertTrue(on)
        }
    }

    @Test
    fun reducedMotionRendersTheSamePixelsAtDifferentFrameTimes() {
        withScene({ InkletCard(seed = 42) { Text("A static sketch") } }) { scene ->
            fun pixels(time: Long) = scene.render(time).use { image -> image.encodeToData()!!.use { it.bytes } }
            assertTrue(pixels(0).contentEquals(pixels(1_000_000_000)))
        }
    }

    @Test
    fun sliderPreservesNativeProgressRangeCallbacksAndDisabledBehavior() {
        var value by mutableStateOf(1f)
        var finished = 0
        withScene({
            Column {
                InkletSlider(value, { value = it }, labelled("roughness"), valueRange = 0f..3f, onValueChangeFinished = { finished++ })
                InkletSlider(value, { value = it }, labelled("locked slider"), enabled = false, valueRange = 0f..3f)
            }
        }) { scene ->
            val slider = scene.node("roughness")
            assertEquals(ProgressBarRangeInfo(1f, 0f..3f), slider.config[SemanticsProperties.ProgressBarRangeInfo])
            assertTrue(slider.boundsInRoot.height >= 48)
            slider.config[SemanticsActions.SetProgress].action!!.invoke(2f)
            assertEquals(2f, value)
            assertEquals(1, finished)
            // Deliver the externally invoked action's state write before reading updated semantics.
            Snapshot.sendApplyNotifications()
            scene.render().close()
            assertEquals(2f, scene.node("roughness").config[SemanticsProperties.ProgressBarRangeInfo].current)
            val locked = scene.node("locked slider")
            assertTrue(locked.config.contains(SemanticsProperties.Disabled))
            locked.config[SemanticsActions.SetProgress].action!!.invoke(0f)
            assertEquals(2f, value)
        }
    }

    @Test
    fun liveStyleUpdatesRedrawIndicatorsAndRestoringStyleRestoresPixels() {
        var style by mutableStateOf(InkletStyle(roughness = 0.0, animate = false))
        withScene({
            InkletTheme(style) {
                Column {
                    InkletCheckbox(true, {}, seed = 42)
                    InkletRadioButton(true, {}, seed = 43)
                }
            }
        }) { scene ->
            fun pixels() = scene.render().use { image -> image.encodeToData()!!.use { it.bytes } }
            val smooth = pixels()
            style = style.copy(roughness = 3.0)
            assertFalse(smooth.contentEquals(pixels()))
            style = style.copy(roughness = 0.0)
            assertTrue(smooth.contentEquals(pixels()))
        }
    }

    @Test
    fun boilChangesFramesAndReducedMotionSuppressesIt() {
        var reduceMotion by mutableStateOf(false)
        withScene({
            InkletTheme(InkletStyle(boil = 1.0), reduceMotion = reduceMotion) {
                InkletRadioButton(true, {}, seed = 42)
            }
        }) { scene ->
            fun pixels(time: Long) = scene.render(time).use { image -> image.encodeToData()!!.use { it.bytes } }
            val first = pixels(0)
            // Feed frames as a window does, including the animation's initial effect frames.
            for (i in 1..9) scene.render(i * 50_000_000L).close()
            assertFalse(first.contentEquals(pixels(500_000_000)))
            reduceMotion = true
            val still = pixels(600_000_000)
            assertTrue(still.contentEquals(pixels(1_100_000_000)))
        }
    }

    private fun labelled(label: String) = Modifier.semantics { contentDescription = label }

    private fun ImageComposeScene.node(label: String): SemanticsNode {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }.single {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true
        }
    }

    private fun withScene(
        content: @Composable () -> Unit,
        test: (ImageComposeScene) -> Unit,
    ) {
        val scene = ImageComposeScene(400, 500) { MaterialTheme { InkletTheme(reduceMotion = true, content = content) } }
        try {
            scene.render(0).close()
            test(scene)
        } finally {
            scene.close()
        }
    }
}
