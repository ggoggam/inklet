@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)

package dev.ggoggam.inklet.material3

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.InkletStyle
import dev.ggoggam.inklet.InkletTheme
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabsTest {
    @Test
    fun nativeTabsKeepSelectionDisabledPointerAndKeyboardBehavior() {
        for (kind in RowKind.entries) {
            var selected by mutableIntStateOf(0)
            withScene({
                InkletTheme(reduceMotion = true) { tabs(kind, selected, { selected = it }) }
            }) { scene ->
                val first = scene.node("tab 0")
                assertEquals(Role.Tab, first.config[SemanticsProperties.Role])
                assertTrue(first.config[SemanticsProperties.Selected])
                assertTrue(first.boundsInRoot.height >= 48)
                val second = scene.node("tab 1")
                scene.sendPointerEvent(PointerEventType.Press, second.boundsInRoot.center, button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, second.boundsInRoot.center, button = PointerButton.Primary)
                scene.advance()
                assertEquals(1, selected)
                assertTrue(scene.node("tab 1").config[SemanticsProperties.Selected])
                assertFalse(scene.node("tab 0").config[SemanticsProperties.Selected])

                val disabled = scene.node("tab 2")
                assertTrue(disabled.config.contains(SemanticsProperties.Disabled))
                scene.sendPointerEvent(PointerEventType.Press, disabled.boundsInRoot.center, button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, disabled.boundsInRoot.center, button = PointerButton.Primary)
                scene.advance()
                assertEquals(1, selected)

                assertTrue(
                    scene
                        .node("tab 0")
                        .config[SemanticsActions.RequestFocus]
                        .action!!
                        .invoke(),
                )
                scene.advance()
                assertTrue(scene.node("tab 0").config[SemanticsProperties.Focused])
                scene.sendKeyEvent(KeyEvent(Key.Enter, KeyEventType.KeyDown))
                scene.sendKeyEvent(KeyEvent(Key.Enter, KeyEventType.KeyUp))
                scene.advance()
                assertEquals(0, selected)
                scene.sendKeyEvent(KeyEvent(Key.Tab, KeyEventType.KeyDown))
                scene.sendKeyEvent(KeyEvent(Key.Tab, KeyEventType.KeyUp))
                scene.advance()
                assertTrue(scene.node("tab 1").config[SemanticsProperties.Focused])
            }
        }
    }

    @Test
    fun indicatorFollowsSelectionAndContentWidthInBothDirections() {
        for (kind in RowKind.entries) {
            for (direction in LayoutDirection.entries) {
                var selected by mutableIntStateOf(0)
                withScene({
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        InkletTheme(InkletStyle(roughness = 0.0), reduceMotion = true) {
                            tabs(kind, selected, { selected = it })
                        }
                    }
                }) { scene ->
                    fun assertIndicator(): Float {
                        val bounds = scene.node("tab $selected").boundsInRoot
                        val pixels = scene.redPixels()
                        assertTrue(pixels.isNotEmpty(), "$kind $direction missing indicator")
                        assertTrue(
                            pixels.all { (x, y) ->
                                x >= bounds.left && x < bounds.right && y >= bounds.bottom - 8 &&
                                    y < bounds.bottom
                            },
                        )
                        val width = pixels.maxOf { it.first } - pixels.minOf { it.first }
                        if (kind.primary) {
                            assertTrue(width < bounds.width - 12, "Primary indicator follows label width")
                        } else {
                            assertTrue(width > bounds.width - 12, "Secondary indicator spans the tab")
                        }
                        return pixels.map { it.first }.average().toFloat()
                    }
                    val before = assertIndicator()
                    selected = 1
                    scene.advance()
                    val after = assertIndicator()
                    assertTrue(if (direction == LayoutDirection.Ltr) after > before else after < before)
                }
            }
        }
    }

    @Test
    fun hoistedSelectionScrollsTheLastTabIntoViewIncludingRtlAndDisabledPlatformMotion() {
        for (kind in RowKind.entries.filter { it.scrollable }) {
            for (direction in LayoutDirection.entries) {
                for (disabledMotion in listOf(false, true)) {
                    var selected by mutableIntStateOf(0)
                    lateinit var scrollState: ScrollState
                    withScene(
                        content = {
                            scrollState = rememberScrollState()
                            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                                InkletTheme(reduceMotion = true) {
                                    tabs(kind, selected, { selected = it }, count = 8, scrollState = scrollState)
                                }
                            }
                        },
                        coroutineContext = if (disabledMotion) NoMotion else EmptyCoroutineContext,
                    ) { scene ->
                        assertTrue(scrollState.maxValue > 0)
                        selected = 7
                        scene.advance()
                        assertTrue(scrollState.value > 0, "$kind $direction must reveal the selected tab")
                        val last = scene.node("tab 7").boundsInRoot
                        assertTrue(last.width >= 89 && last.left >= 0 && last.right <= 360)
                        val ink = scene.redPixels()
                        assertTrue(ink.isNotEmpty() && ink.all { (x, _) -> x >= last.left && x < last.right })
                        val still = scene.pixels()
                        scene.advance()
                        assertTrue(still.contentEquals(scene.pixels()), "Settled reduced-motion ink must stay still")
                    }
                }
            }
        }
    }

    @Test
    fun ribbonSurroundsSelectedTabAndFollowsClicksInBothDirections() {
        for (kind in RowKind.entries) {
            for (direction in LayoutDirection.entries) {
                var selected by mutableIntStateOf(0)
                withScene({
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        InkletTheme(reduceMotion = true) {
                            tabs(kind, selected, { selected = it }, ribbon = true)
                        }
                    }
                }) { scene ->
                    fun assertRibbon() {
                        val bounds = scene.node("tab $selected").boundsInRoot
                        val pixels = scene.redPixels()
                        assertTrue(pixels.isNotEmpty(), "$kind $direction missing ribbon")
                        assertTrue(pixels.all { (x, y) -> x >= bounds.left && x < bounds.right && y >= bounds.top && y < bounds.bottom })
                        assertTrue(pixels.any { (_, y) -> y < bounds.top + 12 }, "Ribbon must pass above the label")
                        assertTrue(pixels.any { (_, y) -> y > bounds.bottom - 12 }, "Ribbon must pass below the label")
                        assertTrue(pixels.any { (x, _) -> x < bounds.left + 12 }, "Ribbon must wrap the left side")
                        assertTrue(pixels.any { (x, _) -> x > bounds.right - 12 }, "Ribbon must wrap the right side")
                    }
                    assertRibbon()
                    val second = scene.node("tab 1")
                    scene.sendPointerEvent(PointerEventType.Press, second.boundsInRoot.center, button = PointerButton.Primary)
                    scene.sendPointerEvent(PointerEventType.Release, second.boundsInRoot.center, button = PointerButton.Primary)
                    scene.advance()
                    assertEquals(1, selected)
                    assertTrue(scene.node("tab 1").config[SemanticsProperties.Selected])
                    assertRibbon()
                    val still = scene.pixels()
                    scene.advance()
                    assertTrue(still.contentEquals(scene.pixels()))
                }
            }
        }
    }

    @Test
    fun ribbonStaysAlignedAfterScrollingWithEdgePaddingAndNoPlatformMotion() {
        for (kind in RowKind.entries.filter { it.scrollable }) {
            for (direction in LayoutDirection.entries) {
                var selected by mutableIntStateOf(0)
                withScene(
                    content = {
                        CompositionLocalProvider(LocalLayoutDirection provides direction) {
                            InkletTheme(reduceMotion = true) {
                                tabs(kind, selected, { selected = it }, count = 8, ribbon = true)
                            }
                        }
                    },
                    coroutineContext = NoMotion,
                ) { scene ->
                    selected = 7
                    scene.advance()
                    val last = scene.node("tab 7").boundsInRoot
                    val ink = scene.redPixels()
                    assertTrue(last.left >= 0 && last.right <= 360)
                    assertTrue(ink.isNotEmpty() && ink.all { (x, _) -> x >= last.left && x < last.right })
                }
            }
        }
    }

    @Test
    fun fractionalRibbonUnwindsAcrossUnequalTabsAndClampsProgress() {
        for (direction in LayoutDirection.entries) {
            var progress by mutableFloatStateOf(0f)
            withScene({
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    InkletTheme(reduceMotion = true) {
                        tabs(
                            RowKind.ScrollablePrimary,
                            if (progress >= 0.5f) 1 else 0,
                            {},
                            ribbon = true,
                            ribbonProgress = { progress },
                        )
                    }
                }
            }) { scene ->
                val initial = scene.pixels()
                val first = scene.node("tab 0").boundsInRoot
                val second = scene.node("tab 1").boundsInRoot
                assertTrue(first.width != second.width, "Exercise different section lengths")
                progress = 0.5f
                scene.advance()
                val ink = scene.redPixels()
                val span = ink.maxOf { it.first } - ink.minOf { it.first }
                assertTrue(span > maxOf(first.width, second.width) * 1.4f, "A moving ribbon must span both tabs, not slide a closed oval")
                assertTrue(ink.any { (x, _) -> x >= first.left && x < first.right })
                assertTrue(ink.any { (x, _) -> x >= second.left && x < second.right })
                for (step in 0..8) {
                    progress = step / 8f
                    scene.advance()
                    File("build/reports/tabs/ribbon-${direction.name}-$step.png").apply { parentFile.mkdirs() }.writeBytes(scene.pixels())
                }
                progress = 0f
                scene.advance()
                assertTrue(initial.contentEquals(scene.pixels()), "Reversing progress restores the same seeded path")
                for (invalid in listOf(-1f, Float.NEGATIVE_INFINITY, Float.NaN)) {
                    progress = invalid
                    scene.advance()
                    assertTrue(initial.contentEquals(scene.pixels()))
                }
                progress = 2f
                scene.advance()
                val last = scene.pixels()
                progress = Float.POSITIVE_INFINITY
                scene.advance()
                assertTrue(last.contentEquals(scene.pixels()))
            }
        }
    }

    @Test
    fun lightDarkRtlAndMaximumPenPreviews() {
        for (dark in listOf(false, true)) {
            for (direction in LayoutDirection.entries) {
                withScene({
                    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                        CompositionLocalProvider(LocalLayoutDirection provides direction) {
                            InkletTheme(reduceMotion = true) {
                                Column {
                                    RowKind.entries.forEach { kind ->
                                        tabs(
                                            kind,
                                            1,
                                            {},
                                            count = if (kind.scrollable) 8 else 3,
                                            ink = MaterialTheme.colorScheme.primary,
                                            ribbon = kind.primary,
                                        )
                                    }
                                    InkletTheme(InkletStyle(roughness = 3.0, boil = 1.0), reduceMotion = true) {
                                        // The ribbon reserves space for the gallery's largest pen settings.
                                        tabs(
                                            RowKind.FixedPrimary,
                                            0,
                                            {},
                                            ink = MaterialTheme.colorScheme.primary,
                                            ribbon = true,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }) { scene ->
                    val preview = scene.pixels()
                    val output = File("build/reports/tabs/${if (dark) "dark" else "light"}-${direction.name}.png")
                    output.parentFile.mkdirs()
                    output.writeBytes(preview)
                    scene.advance()
                    assertTrue(preview.contentEquals(scene.pixels()))
                }
            }
        }
    }

    private enum class RowKind(
        val primary: Boolean,
        val scrollable: Boolean,
    ) {
        FixedPrimary(true, false),
        FixedSecondary(false, false),
        ScrollablePrimary(true, true),
        ScrollableSecondary(false, true),
    }

    @Composable
    private fun tabs(
        kind: RowKind,
        selected: Int,
        onSelect: (Int) -> Unit,
        count: Int = 3,
        scrollState: ScrollState = rememberScrollState(),
        ink: Color = Color.Red,
        roomy: Boolean = false,
        ribbon: Boolean = false,
        ribbonProgress: (() -> Float)? = null,
    ) {
        val indicator: @Composable TabIndicatorScope.() -> Unit = {
            if (ribbonProgress != null) {
                InkletTabRibbonIndicator(
                    progress = ribbonProgress,
                    selectedTabIndex = selected,
                    color = ink,
                    seed = 42,
                )
            } else if (ribbon) {
                InkletTabRibbonIndicator(
                    selectedTabIndex = selected,
                    color = ink,
                    seed = 42,
                )
            } else {
                InkletTabIndicator(
                    Modifier
                        .tabIndicatorOffset(
                            selected,
                            matchContentSize = kind.primary,
                        ).then(if (roomy) Modifier.height(20.dp) else Modifier),
                    color = ink,
                    seed = 42,
                )
            }
        }
        val divider: @Composable () -> Unit = {
            if (!ribbon) InkletDivider(if (roomy) Modifier.height(20.dp) else Modifier, seed = 43)
        }
        val content: @Composable () -> Unit = {
            repeat(count) { index ->
                Tab(
                    selected = selected == index,
                    onClick = { onSelect(index) },
                    modifier = Modifier.semantics { contentDescription = "tab $index" },
                    enabled = index != 2,
                    unselectedContentColor =
                        if (index ==
                            2
                        ) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            LocalContentColor.current
                        },
                    text = { Text(listOf("Plans", "Memories", "Someday")[index % 3]) },
                )
            }
        }
        when (kind) {
            RowKind.FixedPrimary -> {
                PrimaryTabRow(selected, indicator = indicator, divider = divider, tabs = content)
            }

            RowKind.FixedSecondary -> {
                SecondaryTabRow(selected, indicator = indicator, divider = divider, tabs = content)
            }

            RowKind.ScrollablePrimary -> {
                PrimaryScrollableTabRow(
                    selected,
                    scrollState = scrollState,
                    edgePadding = if (ribbon) 20.dp else 0.dp,
                    indicator = indicator,
                    divider = divider,
                    tabs = content,
                )
            }

            RowKind.ScrollableSecondary -> {
                SecondaryScrollableTabRow(
                    selected,
                    scrollState = scrollState,
                    edgePadding = if (ribbon) 20.dp else 0.dp,
                    indicator = indicator,
                    divider = divider,
                    tabs = content,
                )
            }
        }
    }

    private object NoMotion : MotionDurationScale {
        override val scaleFactor = 0f
    }

    private val times = mutableMapOf<ImageComposeScene, Long>()

    private fun ImageComposeScene.advance() {
        Snapshot.sendApplyNotifications()
        repeat(30) {
            times[this] = times.getValue(this) + 50_000_000
            render(times.getValue(this)).close()
        }
    }

    private fun ImageComposeScene.pixels(): ByteArray {
        Snapshot.sendApplyNotifications()
        return render(times.getValue(this)).use { it.encodeToData()!!.use { data -> data.bytes } }
    }

    private fun ImageComposeScene.redPixels(): List<Pair<Int, Int>> {
        val image = ImageIO.read(ByteArrayInputStream(pixels()))
        return buildList {
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val argb = image.getRGB(x, y)
                    if ((argb ushr 24) > 100 && (argb shr 16 and 0xff) > 200 && (argb shr 8 and 0xff) < 120 &&
                        (argb and 0xff) < 120
                    ) {
                        add(x to y)
                    }
                }
            }
        }
    }

    private fun ImageComposeScene.node(label: String): SemanticsNode {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }.single {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true
        }
    }

    private fun withScene(
        content: @Composable () -> Unit,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        test: (ImageComposeScene) -> Unit,
    ) {
        val scene = ImageComposeScene(360, 260, coroutineContext = coroutineContext) { MaterialTheme(content = content) }
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
