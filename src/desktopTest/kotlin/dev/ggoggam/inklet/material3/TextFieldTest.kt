@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)

package dev.ggoggam.inklet.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.InkletStyle
import dev.ggoggam.inklet.InkletTheme
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextFieldTest {
    @Test
    fun editingFocusKeyboardActionsAndHoistedInteractions() {
        var value by mutableStateOf("")
        var focused = false
        var done = 0
        withScene({
            val interactions = remember { MutableInteractionSource() }
            focused = interactions.collectIsFocusedAsState().value
            InkletTextField(
                value,
                { value = it },
                named("field"),
                label = { Text("Adventure") },
                interactionSource = interactions,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { done++ }),
            )
        }) { scene ->
            val bounds = scene.node("field").boundsInRoot
            assertTrue(bounds.width >= 280 && bounds.height >= 56)
            scene.sendPointerEvent(PointerEventType.Press, bounds.center, button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, bounds.center, button = PointerButton.Primary)
            scene.advance()
            assertTrue(focused)
            assertTrue(scene.node("field").config[SemanticsProperties.Focused])
            assertTrue(
                scene
                    .node("field")
                    .config[SemanticsActions.SetText]
                    .action!!
                    .invoke(AnnotatedString("abc")),
            )
            scene.advance()
            assertEquals("abc", value)
            assertEquals("abc", scene.node("field").config[SemanticsProperties.EditableText].text)
            scene
                .node("field")
                .config[SemanticsActions.SetSelection]
                .action!!
                .invoke(3, 3, false)
            scene.key(Key.Backspace)
            assertEquals("ab", value)
            scene
                .node("field")
                .config[SemanticsActions.OnImeAction]
                .action!!
                .invoke()
            assertEquals(1, done)
        }
    }

    @Test
    fun disabledReadOnlyErrorAndPasswordSemantics() {
        var edits = 0
        withScene({
            Column {
                InkletTextField("Disabled", { edits++ }, named("disabled"), enabled = false, isError = true)
                InkletTextField("Keepsake", { edits++ }, named("readonly"), readOnly = true)
                InkletTextField(
                    "0",
                    {},
                    named("error"),
                    isError = true,
                    errorMessage = "Invite a guest",
                    supportingText = { Text("Invite a guest") },
                )
                InkletTextField("secret", {}, named("password"), visualTransformation = PasswordVisualTransformation())
            }
        }) { scene ->
            val disabled = scene.node("disabled")
            assertTrue(disabled.config.contains(SemanticsProperties.Disabled))
            assertTrue(
                disabled.config
                    .getOrNull(SemanticsActions.SetText)
                    ?.action
                    ?.invoke(AnnotatedString("changed")) != true,
            )
            val readonly = scene.node("readonly")
            assertTrue(
                readonly.config
                    .getOrNull(SemanticsActions.SetText)
                    ?.action
                    ?.invoke(AnnotatedString("changed")) != true,
            )
            assertTrue(readonly.config[SemanticsActions.RequestFocus].action!!.invoke())
            scene.advance()
            assertTrue(scene.node("readonly").config[SemanticsProperties.Focused])
            assertTrue(
                scene
                    .node("readonly")
                    .config[SemanticsActions.SetSelection]
                    .action!!
                    .invoke(0, 4, false),
            )
            scene.key(Key.Backspace)
            assertEquals(0, edits)
            assertEquals("Invite a guest", scene.node("error").config[SemanticsProperties.Error])
            assertTrue(scene.node("password").config.contains(SemanticsProperties.Password))
        }
    }

    @Test
    fun floatingGapTracksFocusValueRtlAndLargeTextWithoutEnclosingSupportingText() {
        for (direction in LayoutDirection.entries) {
            for (fontScale in listOf(1f, 2f)) {
                for (noMotion in listOf(false, true)) {
                    var value by mutableStateOf("")
                    var label = Rect.Zero
                    var support = Rect.Zero
                    var leading = Rect.Zero
                    var trailing = Rect.Zero
                    withScene(
                        content = {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides direction,
                                LocalDensity provides Density(1f, fontScale),
                            ) {
                                InkletTheme(InkletStyle(roughness = 3.0, boil = 1.0), reduceMotion = true) {
                                    Column {
                                        InkletTextField(
                                            value,
                                            { value = it },
                                            named("field").fillMaxWidth(),
                                            label = {
                                                Text(
                                                    "Adventure",
                                                    Modifier.onGloballyPositioned {
                                                        label = it.boundsInRoot()
                                                    },
                                                )
                                            },
                                            leadingIcon = {
                                                Box(
                                                    Modifier.size(24.dp).onGloballyPositioned { leading = it.boundsInRoot() },
                                                )
                                            },
                                            trailingIcon = {
                                                Box(
                                                    Modifier.size(24.dp).onGloballyPositioned { trailing = it.boundsInRoot() },
                                                )
                                            },
                                            supportingText = {
                                                Text(
                                                    "Room for a little possibility",
                                                    Modifier.onGloballyPositioned {
                                                        support =
                                                            it.boundsInRoot()
                                                    },
                                                )
                                            },
                                            colors =
                                                OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.Red,
                                                    unfocusedBorderColor = Color.Red,
                                                ),
                                            seed = 3,
                                        )
                                        InkletTextField(
                                            "Rest",
                                            {},
                                            named("other"),
                                            readOnly = true,
                                            colors =
                                                OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent,
                                                ),
                                        )
                                    }
                                }
                            }
                        },
                        coroutineContext = if (noMotion) NoMotion else EmptyCoroutineContext,
                    ) { scene ->
                        val expanded = label
                        assertTrue(if (direction == LayoutDirection.Ltr) leading.left < trailing.left else leading.left > trailing.left)
                        scene
                            .node("field")
                            .config[SemanticsActions.RequestFocus]
                            .action!!
                            .invoke()
                        scene.advance()
                        assertTrue(label.center.y < expanded.center.y, "Label must float on focus")
                        assertTrue(label.height < expanded.height, "Label uses minimized typography")

                        fun assertGap() {
                            val pixels = scene.redPixels()
                            assertTrue(pixels.isNotEmpty())
                            assertTrue(
                                pixels.none { (x, y) ->
                                    x > label.left - 2 && x < label.right + 2 && y >= label.top &&
                                        y < label.bottom
                                },
                                "Pen crosses floating label: $direction $fontScale",
                            )
                            assertTrue(pixels.all { (_, y) -> y < support.top }, "Supporting text belongs below the border")
                            assertTrue(
                                pixels.any { (x, y) -> (x < label.left - 6 || x > label.right + 6) && y < label.bottom },
                                "Top border must remain on either side of gap",
                            )
                        }
                        assertGap()
                        value = "A café"
                        scene.advance()
                        scene
                            .node("other")
                            .config[SemanticsActions.RequestFocus]
                            .action!!
                            .invoke()
                        scene.advance()
                        assertGap()
                        val still = scene.pixels()
                        scene.advance()
                        assertTrue(still.contentEquals(scene.pixels()), "Reduced-motion field must settle")
                        value = ""
                        scene.advance()
                        assertEquals(expanded, label, "Empty unfocused label returns inside")
                    }
                }
            }
        }
    }

    @Test
    fun multilineSizingAndUnspecifiedLabelLineHeight() {
        var value by mutableStateOf("First line")
        withScene({
            MaterialTheme(typography = Typography(bodySmall = MaterialTheme.typography.bodySmall.copy(lineHeight = TextUnit.Unspecified))) {
                Column {
                    InkletTextField("", {}, named("single"), label = { Text("One line") })
                    InkletTextField(
                        value,
                        { value = it },
                        named("multi"),
                        singleLine = false,
                        minLines = 3,
                        maxLines = 4,
                        label = { Text("Notes") },
                    )
                }
            }
        }) { scene ->
            val single = scene.node("single").boundsInRoot.height
            val initial = scene.node("multi").boundsInRoot.height
            assertTrue(initial > single + 24)
            scene
                .node("multi")
                .config[SemanticsActions.SetText]
                .action!!
                .invoke(AnnotatedString("One\nTwo\nThree\nFour\nFive"))
            scene.advance()
            assertEquals("One\nTwo\nThree\nFour\nFive", value)
            val capped = scene.node("multi").boundsInRoot.height
            assertTrue(capped > initial)
            value += "\nSix\nSeven"
            scene.advance()
            assertEquals(capped, scene.node("multi").boundsInRoot.height, "maxLines caps viewport while editing keeps all text")
        }
    }

    @Test
    fun animatedAndZeroRoughnessPenKeepTheFloatingLabelGap() {
        for (roughness in listOf(0.0, 3.0)) {
            var label = Rect.Zero
            withScene({
                InkletTheme(InkletStyle(roughness = roughness, boil = 1.0)) {
                    InkletTextField(
                        "A café",
                        {},
                        named("field"),
                        readOnly = true,
                        seed = 5,
                        label = {
                            Text(
                                "Adventure",
                                Modifier.onGloballyPositioned {
                                    label = it.boundsInRoot()
                                },
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Red),
                    )
                }
            }) { scene ->
                val frames = mutableListOf<ByteArray>()
                repeat(3) {
                    frames += scene.pixels()
                    val ink = scene.redPixels()
                    assertTrue(ink.isNotEmpty())
                    assertTrue(ink.none { (x, y) -> x > label.left - 2 && x < label.right + 2 && y >= label.top && y < label.bottom })
                    times[scene] = times.getValue(scene) + 400_000_000
                    scene.render(times.getValue(scene)).close()
                    scene.render(times.getValue(scene)).close()
                }
                assertTrue(frames.drop(1).any { !it.contentEquals(frames.first()) }, "Boil stays active")
            }
        }
    }

    @Test
    fun penColorsFollowFocusErrorAndDisabledPrecedence() {
        var enabled by mutableStateOf(true)
        var error by mutableStateOf(false)
        withScene({
            InkletTextField(
                "",
                {},
                named("field"),
                enabled = enabled,
                isError = error,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Red,
                        unfocusedBorderColor = Color.Green,
                        errorBorderColor = Color.Blue,
                        disabledBorderColor = Color.Magenta,
                    ),
            )
        }) { scene ->
            fun assertColor(color: Int) {
                val image = ImageIO.read(ByteArrayInputStream(scene.pixels()))
                assertTrue((0 until image.height).any { y -> (0 until image.width).any { x -> image.getRGB(x, y) == color } })
            }
            assertColor(0xff00ff00.toInt())
            scene
                .node("field")
                .config[SemanticsActions.RequestFocus]
                .action!!
                .invoke()
            scene.advance()
            assertColor(0xffff0000.toInt())
            error = true
            scene.advance()
            assertColor(0xff0000ff.toInt())
            enabled = false
            scene.advance()
            assertColor(0xffff00ff.toInt())
        }
    }

    @Test
    fun keyboardTraversalSkipsDisabledFieldsAndKeepsReadOnlyFocusable() {
        withScene({
            Column {
                InkletTextField("", {}, named("first"))
                InkletTextField("", {}, named("disabled"), enabled = false)
                InkletTextField("Kept", {}, named("readonly"), readOnly = true)
                InkletTextField("", {}, named("last"))
            }
        }) { scene ->
            scene
                .node("first")
                .config[SemanticsActions.RequestFocus]
                .action!!
                .invoke()
            scene.advance()
            scene.key(Key.Tab)
            assertTrue(scene.node("readonly").config[SemanticsProperties.Focused])
            assertFalse(scene.node("disabled").config.getOrNull(SemanticsProperties.Focused) == true)
            scene.key(Key.Tab)
            assertTrue(scene.node("last").config[SemanticsProperties.Focused])
        }
    }

    @Test
    fun transparentPenHasNoNativeBorderAndFillFollowsState() {
        var enabled by mutableStateOf(true)
        var error by mutableStateOf(false)
        var fill by mutableStateOf(false)
        withScene({
            InkletTextField(
                "",
                {},
                named("field"),
                enabled = enabled,
                isError = error,
                readOnly = true,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        errorBorderColor = Color.Transparent,
                        focusedContainerColor = if (fill) Color.Red else Color.Transparent,
                        unfocusedContainerColor = if (fill) Color.Green else Color.Transparent,
                        disabledContainerColor = if (fill) Color.Magenta else Color.Transparent,
                        errorContainerColor = if (fill) Color.Blue else Color.Transparent,
                    ),
            )
        }) { scene ->
            fun assertDrawing(expected: Int?) {
                val image = ImageIO.read(ByteArrayInputStream(scene.pixels()))
                val colored =
                    buildList {
                        for (y in 0 until image.height) {
                            for (x in 0 until image.width) {
                                val pixel = image.getRGB(x, y)
                                if (pixel ushr 24 > 0 && pixel != 0xffffffff.toInt()) add(pixel)
                            }
                        }
                    }
                if (expected == null) {
                    assertTrue(colored.isEmpty(), "Native drawing must not remain under a transparent pen")
                } else {
                    assertTrue(expected in colored, "Missing state container color")
                }
            }
            assertDrawing(null)
            fill = true
            scene.advance()
            assertDrawing(0xff00ff00.toInt())
            scene
                .node("field")
                .config[SemanticsActions.RequestFocus]
                .action!!
                .invoke()
            scene.advance()
            assertDrawing(0xffff0000.toInt())
            fill = false
            scene.advance()
            assertDrawing(null)
            error = true
            scene.advance()
            assertDrawing(null)
            fill = true
            scene.advance()
            assertDrawing(0xff0000ff.toInt())
            enabled = false
            scene.advance()
            assertDrawing(0xffff00ff.toInt())
            fill = false
            scene.advance()
            assertDrawing(null)
        }
    }

    @Test
    fun labelTransitionKeepsInkClearBetweenSettledPositions() {
        for (direction in LayoutDirection.entries) {
            for (fontScale in listOf(1f, 2f)) {
                for (reduceMotion in listOf(false, true)) {
                    var label = Rect.Zero
                    withScene({
                        CompositionLocalProvider(
                            LocalLayoutDirection provides direction,
                            LocalDensity provides Density(1f, fontScale),
                        ) {
                            InkletTheme(InkletStyle(roughness = 3.0, boil = 1.0), reduceMotion = reduceMotion) {
                                Column {
                                    InkletTextField(
                                        "",
                                        {},
                                        named("field"),
                                        readOnly = true,
                                        seed = 3,
                                        label = {
                                            Text(
                                                "Adventure",
                                                Modifier.onGloballyPositioned {
                                                    label = it.boundsInRoot()
                                                },
                                            )
                                        },
                                        colors =
                                            OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Red,
                                                unfocusedBorderColor = Color.Red,
                                            ),
                                    )
                                    InkletTextField(
                                        "Rest",
                                        {},
                                        named("other"),
                                        readOnly = true,
                                        colors =
                                            OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                            ),
                                    )
                                }
                            }
                        }
                    }) { scene ->
                        val expanded = label.center.y
                        for (target in listOf("field", "other")) {
                            scene
                                .node(target)
                                .config[SemanticsActions.RequestFocus]
                                .action!!
                                .invoke()
                            val centers = mutableSetOf<Float>()
                            Snapshot.sendApplyNotifications()
                            repeat(60) {
                                times[scene] = times.getValue(scene) + 10_000_000
                                scene.render(times.getValue(scene)).close()
                                centers += label.center.y
                                val ink = scene.redPixels()
                                assertTrue(
                                    ink.none { (x, y) -> x > label.left && x < label.right && y >= label.top && y < label.bottom },
                                    "Ink overlaps moving label: $direction $target $label",
                                )
                            }
                            assertTrue(centers.size > 2, "Observe actual intermediate label positions")
                        }
                        assertEquals(expanded, label.center.y)
                    }
                }
            }
        }
    }

    private fun named(name: String) = Modifier.semantics { contentDescription = name }

    private object NoMotion : MotionDurationScale {
        override val scaleFactor = 0f
    }

    private val times = mutableMapOf<ImageComposeScene, Long>()

    private fun ImageComposeScene.advance() {
        Snapshot.sendApplyNotifications()
        repeat(20) {
            times[this] = times.getValue(this) + 50_000_000
            render(times.getValue(this)).close()
        }
    }

    private fun ImageComposeScene.key(key: Key) {
        sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
        sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
        advance()
    }

    private fun ImageComposeScene.pixels(): ByteArray = render(times.getValue(this)).use { it.encodeToData()!!.use { data -> data.bytes } }

    private fun ImageComposeScene.redPixels(): List<Pair<Int, Int>> {
        val image = ImageIO.read(ByteArrayInputStream(pixels()))
        return buildList {
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val argb = image.getRGB(x, y)
                    if ((argb shr 16 and 0xff) > 200 && (argb shr 8 and 0xff) < 120 && (argb and 0xff) < 120) add(x to y)
                }
            }
        }
    }

    private fun ImageComposeScene.node(name: String): SemanticsNode {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }.single {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(name) == true
        }
    }

    private fun withScene(
        content: @Composable () -> Unit,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        test: (ImageComposeScene) -> Unit,
    ) {
        val scene =
            ImageComposeScene(600, 600, coroutineContext = coroutineContext) {
                MaterialTheme {
                    InkletTheme(reduceMotion = true) {
                        Box(Modifier.background(Color.White).padding(20.dp)) { content() }
                    }
                }
            }
        times[scene] = 0
        try {
            scene.advance()
            test(scene)
        } finally {
            times.remove(scene)
            scene.close()
        }
    }
}
