@file:OptIn(
    androidx.compose.runtime.InternalComposeApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.ui.InternalComposeUiApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package dev.ggoggam.inklet.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.findDefaultNavigationEventDispatcherOwner
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInput
import dev.ggoggam.inklet.InkletStyle
import dev.ggoggam.inklet.InkletTheme
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContainerRecipesTest {
    @Test
    fun cardsAndSurfacesRetainPointerKeyboardSelectionAndDisabledBehavior() {
        withScene {
            val disabled = node("The winter picnic · coming later")
            assertTrue(disabled.config.contains(SemanticsProperties.Disabled))
            click("The winter picnic · coming later")
            assertTrue(hasText("A little room for our weekend."))
            click("A Saturday at the market")
            assertTrue(hasText("The market plan has been opened 1 times."))
            val card = node("A Saturday at the market")
            assertTrue(card.config[SemanticsActions.RequestFocus].action!!.invoke())
            advance()
            assertTrue(node("A Saturday at the market").config[SemanticsProperties.Focused])
            key(Key.Enter)
            assertTrue(hasText("The market plan has been opened 2 times."))
            assertFalse(node("Keep this close").config[SemanticsProperties.Selected])
            click("Keep this close")
            assertTrue(node("Kept close to our hearts").config[SemanticsProperties.Selected])
            snapshot("selected")
        }
    }

    @Test
    fun menuLivesInPopupKeepsDisabledItemsPositioningAndDismissal() {
        for (direction in LayoutDirection.entries) {
            withScene(direction = direction) {
                val anchor = node("Plan options").boundsInWindow
                click("Plan options")
                val item = node("Copy our plan")
                assertTrue(item.boundsInWindow.top >= anchor.top)
                assertTrue(item.boundsInWindow.left >= 0 && item.boundsInWindow.right <= 800)
                assertTrue(node("Share · coming soon").config.contains(SemanticsProperties.Disabled))
                click("Share · coming soon")
                assertTrue(hasText("Copy our plan"))
                snapshot("menu-${direction.name}")
                click("Copy our plan")
                assertFalse(hasText("Copy our plan"))
                assertTrue(hasText("A copy is ready for us."))
                click("Plan options")
                key(Key.DirectionDown)
                assertTrue(node("Copy our plan").config[SemanticsProperties.Focused])
                key(Key.Enter)
                assertFalse(hasText("Copy our plan"))
                click("Plan options")
                key(Key.Escape)
                assertFalse(hasText("Copy our plan"))
                click("Plan options")
                clickAt(Offset(780f, 740f))
                assertFalse(hasText("Copy our plan"))
            }
        }
    }

    @Test
    fun dialogSheetTooltipAndSnackbarKeepTheirNativeActions() {
        withScene {
            click("Make a promise")
            snapshot("dialog")
            click("Let's do it")
            assertFalse(hasText("One small promise"))
            assertTrue(hasText("One slow morning, together."))
            click("Make a promise")
            key(Key.Escape)
            assertFalse(hasText("One small promise"))

            click("Packing list")
            assertTrue(hasText("For a slow day outside"))
            snapshot("sheet")
            click("All packed")
            assertFalse(hasText("For a slow day outside"))
            click("Packing list")
            clickAt(Offset(780f, 20f))
            assertFalse(hasText("For a slow day outside"))

            click("A little tip")
            assertTrue(hasText("Leave room for the unexpected."))
            snapshot("tooltip")
            clickAt(Offset(780f, 740f))
            assertFalse(hasText("Leave room for the unexpected."))

            click("Save weekend")
            snapshot("snackbar")
            click("Undo")
            assertFalse(hasText("Our weekend is saved."))
            assertTrue(hasText("Room for a different weekend."))
            click("Save weekend")
            click("Dismiss")
            assertFalse(hasText("Our weekend is saved."))
            assertTrue(hasText("A weekend to look forward to."))
        }
    }

    @Test
    fun lightDarkMaximumPenAndPopupPreviews() {
        for (dark in listOf(false, true)) {
            withScene(dark = dark, maximumPen = true) {
                val theme = if (dark) "dark" else "light"
                snapshot("$theme-containers")
                click("Keep this close")
                snapshot("$theme-selected")
                for ((trigger, name) in listOf(
                    "Plan options" to "menu",
                    "Make a promise" to "dialog",
                    "Packing list" to "sheet",
                    "A little tip" to "tooltip",
                )) {
                    click(trigger)
                    snapshot("$theme-$name")
                    clickAt(Offset(780f, 20f))
                }
                click("Save weekend")
                snapshot("$theme-snackbar")
            }
        }
    }

    @Test
    fun narrowGalleryAndDialogPreview() {
        withScene(width = 380, height = 900) {
            snapshot("narrow-containers")
            click("Make a promise")
            assertTrue(hasText("One small promise"))
            snapshot("narrow-dialog")
        }
    }

    @Test
    fun scrolledAppBarUsesTheSamePenFill() {
        fun image(scrolled: Boolean): ByteArray {
            var bytes = byteArrayOf()
            withScene(content = {
                val state = rememberTopAppBarState(initialContentOffset = if (scrolled) -100f else 0f)
                RecipeAppBar(TopAppBarDefaults.pinnedScrollBehavior(state))
            }) { bytes = pixels() }
            return bytes
        }
        assertTrue(image(false).contentEquals(image(true)), "Native scrolled fill must stay transparent")
    }

    private object NoMotion : MotionDurationScale {
        override val scaleFactor = 0f
    }

    private val backInput =
        object : NavigationEventInput() {
            fun back() = dispatchOnBackCompleted()
        }

    private var time = 0L

    private fun ImageComposeScene.advance() {
        Snapshot.sendApplyNotifications()
        repeat(12) {
            time += 16_000_000
            render(time).close()
        }
    }

    private fun ImageComposeScene.nodes(): List<SemanticsNode> {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
    }

    private fun SemanticsNode.hasText(text: String): Boolean = config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true

    private fun ImageComposeScene.hasText(text: String): Boolean = nodes().any { it.hasText(text) }

    private fun ImageComposeScene.node(text: String): SemanticsNode = nodes().firstOrNull { it.hasText(text) } ?: error("Missing '$text'")

    private fun ImageComposeScene.click(text: String) = clickAt(node(text).boundsInWindow.center)

    private fun ImageComposeScene.clickAt(position: Offset) {
        sendPointerEvent(PointerEventType.Press, position, button = PointerButton.Primary)
        sendPointerEvent(PointerEventType.Release, position, button = PointerButton.Primary)
        advance()
    }

    private fun ImageComposeScene.key(key: Key) {
        // Desktop windows translate Escape into navigation events before scene key dispatch.
        if (key == Key.Escape) {
            backInput.back()
        } else {
            sendKeyEvent(KeyEvent(key, KeyEventType.KeyDown))
            sendKeyEvent(KeyEvent(key, KeyEventType.KeyUp))
        }
        advance()
    }

    private fun ImageComposeScene.pixels(): ByteArray = render(time).use { it.encodeToData()!!.use { data -> data.bytes } }

    private fun ImageComposeScene.snapshot(name: String) {
        val output = File("build/reports/containers/$name.png")
        output.parentFile.mkdirs()
        output.writeBytes(pixels())
    }

    private fun withScene(
        dark: Boolean = false,
        maximumPen: Boolean = false,
        direction: LayoutDirection = LayoutDirection.Ltr,
        width: Int = 800,
        height: Int = 760,
        content: @Composable () -> Unit = { ContainerRecipes() },
        test: ImageComposeScene.() -> Unit,
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeAndWait { withScene(dark, maximumPen, direction, width, height, content, test) }
            return
        }
        time = 0
        val scene =
            ImageComposeScene(
                width = width,
                height = height,
                // Preserve ImageComposeScene's dispatcher when overriding motion scale.
                coroutineContext = Dispatchers.Unconfined + NoMotion,
            ) {
                val dispatcher = findDefaultNavigationEventDispatcherOwner()!!.navigationEventDispatcher
                DisposableEffect(dispatcher) {
                    dispatcher.addInput(backInput)
                    onDispose { dispatcher.removeInput(backInput) }
                }
                MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        InkletTheme(
                            style = if (maximumPen) InkletStyle(roughness = 3.0, boil = 1.0) else InkletStyle(),
                            reduceMotion = true,
                        ) {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) { content() }
                        }
                    }
                }
            }
        try {
            scene.advance()
            scene.test()
        } finally {
            scene.close()
        }
    }
}
