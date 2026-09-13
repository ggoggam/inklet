@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.ggoggam.inklet.material3

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.InkletTheme
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChipsAndIconButtonsTest {
    @Test
    fun chipActionsAndSelectionRemainHoistedAndDisabledCallbacksAreSuppressed() {
        var clicks = 0
        var selected by mutableStateOf(false)
        withScene({
            Column {
                InkletAssistChip({ clicks++ }, { Text("Assist") }, labelled("assist"))
                InkletSuggestionChip({ clicks++ }, { Text("Suggest") }, labelled("suggest"))
                InkletFilterChip(selected, { selected = !selected }, { Text("Filter") }, labelled("filter"))
                InkletInputChip(selected, { selected = !selected }, { Text("Input") }, labelled("input"))
                InkletAssistChip({ clicks++ }, { Text("Disabled") }, labelled("disabled assist"), enabled = false)
                InkletSuggestionChip({ clicks++ }, { Text("Disabled") }, labelled("disabled suggest"), enabled = false)
                InkletFilterChip(true, { clicks++ }, { Text("Disabled") }, labelled("disabled filter"), enabled = false)
                InkletInputChip(false, { clicks++ }, { Text("Disabled") }, labelled("disabled input"), enabled = false)
            }
        }) { scene ->
            for (name in listOf("assist", "suggest")) {
                val node = scene.node(name)
                assertEquals(Role.Button, node.config[SemanticsProperties.Role])
                assertTrue(node.boundsInRoot.height >= 48)
                node.click()
            }
            assertEquals(2, clicks)
            val filter = scene.node("filter")
            assertEquals(Role.Checkbox, filter.config[SemanticsProperties.Role])
            assertFalse(filter.config[SemanticsProperties.Selected])
            filter.click()
            scene.refresh()
            assertTrue(scene.node("filter").config[SemanticsProperties.Selected])
            assertTrue(scene.node("input").config[SemanticsProperties.Selected])
            scene.node("input").click()
            assertFalse(selected)
            for (name in listOf("assist", "suggest", "filter", "input")) {
                val node = scene.node("disabled $name")
                assertTrue(node.config.contains(SemanticsProperties.Disabled))
                node.click()
            }
            assertEquals(2, clicks)
        }
    }

    @Test
    fun iconButtonsKeepRolesTargetsPointerInputAndCheckedState() {
        var clicks = 0
        var checked by mutableStateOf(false)
        withScene({
            Column {
                InkletIconButton({ clicks++ }, labelled("add")) { Text("+") }
                InkletIconToggleButton(checked, { checked = it }, labelled("favorite")) { Text("*") }
                InkletIconButton({ clicks++ }, labelled("disabled"), enabled = false) { Text("+") }
                InkletIconToggleButton(true, { clicks++ }, labelled("disabled toggle"), enabled = false) { Text("*") }
            }
        }) { scene ->
            val add = scene.node("add")
            assertEquals(Role.Button, add.config[SemanticsProperties.Role])
            assertTrue(add.boundsInRoot.width >= 48 && add.boundsInRoot.height >= 48)
            val center = add.boundsInRoot.center
            scene.sendPointerEvent(PointerEventType.Press, center, button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, center, button = PointerButton.Primary)
            assertEquals(1, clicks)
            val toggle = scene.node("favorite")
            assertEquals(Role.Checkbox, toggle.config[SemanticsProperties.Role])
            assertEquals(ToggleableState.Off, toggle.config[SemanticsProperties.ToggleableState])
            toggle.click()
            scene.refresh()
            assertTrue(checked)
            assertEquals(ToggleableState.On, scene.node("favorite").config[SemanticsProperties.ToggleableState])
            for (name in listOf("disabled", "disabled toggle")) {
                val node = scene.node(name)
                assertTrue(node.config.contains(SemanticsProperties.Disabled))
                node.click()
            }
            assertEquals(1, clicks)
        }
    }

    @Test
    fun customContainerAndSlotColorsFollowEverySelectableState() {
        var selected by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        var labelColor = Color.Unspecified
        var leadingColor = Color.Unspecified
        var trailingColor = Color.Unspecified
        withScene({
            val colors =
                InkletChipDefaults.selectableChipColors().copy(
                    containerColor = Color.Red,
                    selectedContainerColor = Color.Green,
                    disabledContainerColor = Color.Blue,
                    disabledSelectedContainerColor = Color.Yellow,
                    labelColor = Color.Magenta,
                    selectedLabelColor = Color.Cyan,
                    disabledLabelColor = Color.Gray,
                    leadingIconColor = Color.White,
                    selectedLeadingIconColor = Color.Black,
                    disabledLeadingIconColor = Color.DarkGray,
                    trailingIconColor = Color.Black,
                    selectedTrailingIconColor = Color.White,
                    disabledTrailingIconColor = Color.LightGray,
                )
            InkletFilterChip(
                selected,
                {},
                {
                    labelColor = LocalContentColor.current
                    Box(Modifier.size(24.dp))
                },
                modifier = Modifier.size(160.dp, 48.dp),
                enabled = enabled,
                leadingIcon = {
                    leadingColor = LocalContentColor.current
                    Box(Modifier.size(18.dp))
                },
                trailingIcon = {
                    trailingColor = LocalContentColor.current
                    Box(Modifier.size(18.dp))
                },
                colors = colors,
                seed = 42,
            )
        }) { scene ->
            fun assertState(
                fill: Int,
                label: Color,
                leading: Color,
                trailing: Color,
            ) {
                scene.refresh()
                scene.render().use { image ->
                    val bitmap =
                        org.jetbrains.skia.Bitmap
                            .makeFromImage(image)
                    try {
                        assertEquals(fill, bitmap.getColor(80, 24))
                    } finally {
                        bitmap.close()
                    }
                }
                assertEquals(label, labelColor)
                assertEquals(leading, leadingColor)
                assertEquals(trailing, trailingColor)
            }
            assertState(0xFFFF0000.toInt(), Color.Magenta, Color.White, Color.Black)
            selected = true
            assertState(0xFF00FF00.toInt(), Color.Cyan, Color.Black, Color.White)
            enabled = false
            assertState(0xFFFFFF00.toInt(), Color.Gray, Color.DarkGray, Color.LightGray)
            selected = false
            assertState(0xFF0000FF.toInt(), Color.Gray, Color.DarkGray, Color.LightGray)
        }
    }

    @Test
    fun nativeActionAndToggleColorsReachThePenFill() {
        var checked by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        withScene({
            Column {
                InkletAssistChip(
                    {},
                    { Box(Modifier.size(24.dp)) },
                    Modifier.size(100.dp, 48.dp),
                    enabled = enabled,
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color.Red, disabledContainerColor = Color.Blue),
                    seed = 1,
                )
                InkletIconToggleButton(
                    checked,
                    {},
                    enabled = enabled,
                    colors =
                        IconButtonDefaults.iconToggleButtonColors(
                            containerColor = Color.Red,
                            checkedContainerColor = Color.Green,
                            disabledContainerColor = Color.Blue,
                        ),
                    seed = 2,
                ) {}
            }
        }) { scene ->
            fun fills(): Pair<Int, Int> {
                scene.refresh()
                return scene.render().use { image ->
                    val bitmap =
                        org.jetbrains.skia.Bitmap
                            .makeFromImage(image)
                    try {
                        bitmap.getColor(24, 24) to bitmap.getColor(24, 72)
                    } finally {
                        bitmap.close()
                    }
                }
            }
            assertEquals(0xFFFF0000.toInt() to 0xFFFF0000.toInt(), fills())
            checked = true
            assertEquals(0xFFFF0000.toInt() to 0xFF00FF00.toInt(), fills())
            enabled = false
            assertEquals(0xFF0000FF.toInt() to 0xFF0000FF.toInt(), fills())
        }
    }

    @Test
    fun rtlReordersNativeSlotsAndReducedMotionKeepsSeededDrawingStable() {
        var direction by mutableStateOf(LayoutDirection.Ltr)
        var avatarLeft = 0f
        var trailingLeft = 0f
        withScene({
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                InkletInputChip(
                    true,
                    {},
                    { Text("Person") },
                    avatar = { Box(Modifier.size(24.dp).onGloballyPositioned { avatarLeft = it.positionInRoot().x }) },
                    trailingIcon = { Box(Modifier.size(18.dp).onGloballyPositioned { trailingLeft = it.positionInRoot().x }) },
                    seed = 42,
                )
            }
        }) { scene ->
            assertTrue(avatarLeft < trailingLeft)
            direction = LayoutDirection.Rtl
            scene.refresh()
            assertTrue(avatarLeft > trailingLeft)

            fun pixels(time: Long) = scene.render(time).use { it.encodeToData()!!.use { it.bytes } }
            assertTrue(pixels(0).contentEquals(pixels(1_000_000_000)))
        }
    }

    @Test
    fun callerInteractionSourceDrivesFocusDrawing() {
        val source = MutableInteractionSource()
        withScene({ InkletAssistChip({}, { Text("Focus") }, interactionSource = source, seed = 42) }) { scene ->
            fun pixels() = scene.render().use { it.encodeToData()!!.use { it.bytes } }
            val idle = pixels()
            val focus = FocusInteraction.Focus()
            runBlocking { source.emit(focus) }
            scene.refresh()
            assertFalse(idle.contentEquals(pixels()))
            runBlocking { source.emit(FocusInteraction.Unfocus(focus)) }
            scene.refresh()
            assertTrue(idle.contentEquals(pixels()))
        }
    }

    private fun labelled(label: String) = Modifier.semantics { contentDescription = label }

    private fun SemanticsNode.click() = config[SemanticsActions.OnClick].action!!.invoke()

    private fun ImageComposeScene.refresh() {
        Snapshot.sendApplyNotifications()
        render().close()
    }

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
