@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.ggoggam.inklet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressIndicatorTest {
    @Test
    fun semanticsFollowProgressAndClampInvalidValuesWithoutInputActions() {
        var progress by mutableFloatStateOf(0.4f)
        withScene({
            InkletTheme(reduceMotion = true) {
                Column {
                    InkletLinearProgressIndicator({ progress }, labelled("linear"))
                    InkletCircularProgressIndicator({ progress }, labelled("circular"))
                    InkletLinearProgressIndicator(labelled("loading linear"))
                    InkletCircularProgressIndicator(labelled("loading circular"))
                }
            }
        }) { scene ->
            listOf(0.4f to 0.4f, -1f to 0f, 2f to 1f, Float.NaN to 0f, Float.POSITIVE_INFINITY to 1f).forEach { (input, expected) ->
                progress = input
                Snapshot.sendApplyNotifications()
                scene.render().close()
                listOf("linear", "circular").forEach { label ->
                    val node = scene.node(label)
                    assertEquals(ProgressBarRangeInfo(expected, 0f..1f), node.config[SemanticsProperties.ProgressBarRangeInfo])
                    assertFalse(node.config.contains(SemanticsActions.SetProgress))
                    assertFalse(node.config.contains(SemanticsActions.OnClick))
                }
            }
            listOf("loading linear", "loading circular").forEach { label ->
                assertEquals(ProgressBarRangeInfo.Indeterminate, scene.node(label).config[SemanticsProperties.ProgressBarRangeInfo])
            }
        }
    }

    @Test
    fun drawingFollowsProgressAndHasEmptyAndCompleteEndpoints() {
        for (circular in listOf(false, true)) {
            var progress by mutableFloatStateOf(0f)
            withScene({
                InkletTheme(InkletStyle(roughness = 0.0), reduceMotion = true) {
                    indicator(circular, { progress })
                }
            }) { scene ->
                val empty = scene.pixels()
                assertTrue(scene.redPixels().isEmpty())
                progress = 0.25f
                val quarter = scene.redPixels().size
                assertTrue(quarter > 0)
                progress = 1f
                val full = scene.pixels()
                assertTrue(scene.redPixels().size > quarter * 2)
                progress = 5f
                assertTrue(full.contentEquals(scene.pixels()))
                progress = -1f
                assertTrue(empty.contentEquals(scene.pixels()))
                progress = Float.NaN
                assertTrue(empty.contentEquals(scene.pixels()))
            }
        }
    }

    @Test
    fun linearProgressFollowsLayoutDirectionAndCircularProgressStaysClockwise() {
        for (circular in listOf(false, true)) {
            var direction by mutableStateOf(LayoutDirection.Ltr)
            withScene({
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    InkletTheme(InkletStyle(roughness = 0.0), reduceMotion = true) {
                        indicator(circular, { 0.25f })
                    }
                }
            }) { scene ->
                val ltr = scene.redPixels()
                assertTrue(ltr.isNotEmpty())
                if (circular) {
                    // At a quarter turn the arc occupies the top-right quadrant.
                    assertTrue(ltr.all { (x, y) -> x >= 19 && y <= 21 })
                } else {
                    assertTrue(ltr.all { (x, _) -> x < 62 })
                }
                direction = LayoutDirection.Rtl
                val rtl = scene.redPixels()
                if (circular) {
                    assertEquals(ltr, rtl)
                } else {
                    assertTrue(rtl.isNotEmpty() && rtl.all { (x, _) -> x >= 178 })
                }
            }
        }
    }

    @Test
    fun loadingMovesWithZeroBoilOrDisabledPenAnimationAndReducedMotionFreezesIt() {
        for (circular in listOf(false, true)) {
            for (style in listOf(InkletStyle(boil = 0.0), InkletStyle(animate = false))) {
                var reduceMotion by mutableStateOf(false)
                withScene({
                    InkletTheme(style, reduceMotion) { indicator(circular) }
                }) { scene ->
                    val first = scene.pixels(0)
                    for (i in 1..9) scene.render(i * 50_000_000L).close()
                    assertFalse(first.contentEquals(scene.pixels(500_000_000)), "Loading must move: circular=$circular, $style")
                    reduceMotion = true
                    val still = scene.pixels(600_000_000)
                    assertTrue(still.contentEquals(scene.pixels(1_100_000_000)))
                    assertTrue(scene.redPixels(1_200_000_000).isNotEmpty(), "Reduced motion must leave a visible loading stroke")
                }
            }
        }
    }

    @Test
    fun loadingAlsoMovesOutsideInkletTheme() {
        for (circular in listOf(false, true)) {
            withScene({ indicator(circular) }) { scene ->
                val first = scene.pixels(0)
                for (i in 1..9) scene.render(i * 50_000_000L).close()
                assertFalse(first.contentEquals(scene.pixels(500_000_000)))
            }
        }
    }

    @Test
    fun circularLoadingStaysOnTheRingThroughoutEachLap() {
        withScene({
            InkletTheme(InkletStyle(roughness = 0.0, boil = 0.0)) { indicator(circular = true) }
        }) { scene ->
            // Include every seam crossing and the exact endpoint of two 6000ms growth cycles.
            for (millis in 0L..12000L step 30) {
                val pixels = scene.redPixels(millis * 1_000_000)
                assertTrue(pixels.isNotEmpty(), "Missing arc at ${millis}ms")
                assertTrue(
                    pixels.all { (x, y) -> hypot(x + 0.5f - 20f, y + 0.5f - 20f) in 18f..21f },
                    "Arc left the ring at ${millis}ms",
                )
            }
        }
    }

    @Test
    fun circularLoadingDoesNotDropTheTailAtThePathSeamOrLoopBoundary() {
        withScene({
            InkletTheme(InkletStyle(roughness = 0.0, boil = 0.0)) { indicator(circular = true) }
        }) { scene ->
            var previous = scene.redPixels(0).toSet()
            for (millis in 10L..12000L step 10) {
                val current = scene.redPixels(millis * 1_000_000).toSet()
                val changed = (previous - current).size + (current - previous).size
                assertTrue(changed < 25, "Arc jumped at ${millis}ms: $changed pixels changed")
                previous = current
            }
        }
    }

    @Test
    fun platformDisabledAnimationsLeaveAVisibleStaticLoadingStroke() {
        val disabledMotion =
            object : MotionDurationScale {
                override val scaleFactor = 0f
            }
        for (circular in listOf(false, true)) {
            withScene(
                content = { InkletTheme(InkletStyle(boil = 0.0)) { indicator(circular) } },
                coroutineContext = disabledMotion,
            ) { scene ->
                for (i in 1..9) scene.render(i * 50_000_000L).close()
                val still = scene.pixels(500_000_000)
                assertTrue(still.contentEquals(scene.pixels(1_000_000_000)))
                assertTrue(scene.redPixels(1_100_000_000).isNotEmpty())
                if (circular) {
                    assertTrue(
                        scene.redPixels(1_100_000_000).all { (x, y) -> hypot(x + 0.5f - 20f, y + 0.5f - 20f) in 14f..21f },
                        "Disabled animations must leave an arc on the ring, without a diagonal from the path origin",
                    )
                }
            }
        }
    }

    @Test
    fun customBoundsAndLivePenStylesRenderDeterministically() {
        for (circular in listOf(false, true)) {
            var style by mutableStateOf(InkletStyle(roughness = 0.0))
            withScene({
                InkletTheme(style, reduceMotion = true) {
                    indicator(circular, { 0.6f }, Modifier.size(100.dp, 60.dp))
                }
            }) { scene ->
                val smooth = scene.pixels()
                assertTrue(scene.redPixels().all { (x, y) -> x < 100 && y < 60 })
                if (circular) assertTrue(scene.redPixels().all { (x, _) -> x in 20..79 })
                style = style.copy(roughness = 3.0, boil = 1.0)
                assertFalse(smooth.contentEquals(scene.pixels()))
                style = style.copy(roughness = 0.0)
                assertTrue(smooth.contentEquals(scene.pixels()))
            }
        }
    }

    @Composable
    private fun indicator(
        circular: Boolean,
        progress: (() -> Float)? = null,
        modifier: Modifier = Modifier,
    ) {
        if (circular) {
            if (progress == null) {
                InkletCircularProgressIndicator(modifier, Color.Red, Color.Transparent, seed = 42)
            } else {
                InkletCircularProgressIndicator(progress, modifier, Color.Red, Color.Transparent, seed = 42)
            }
        } else {
            if (progress == null) {
                InkletLinearProgressIndicator(modifier, Color.Red, Color.Transparent, seed = 42)
            } else {
                InkletLinearProgressIndicator(progress, modifier, Color.Red, Color.Transparent, seed = 42)
            }
        }
    }

    private fun ImageComposeScene.pixels(time: Long = 0): ByteArray {
        Snapshot.sendApplyNotifications()
        return render(time).use { image -> image.encodeToData()!!.use { it.bytes } }
    }

    private fun ImageComposeScene.redPixels(time: Long = 0): List<Pair<Int, Int>> {
        val image = ImageIO.read(ByteArrayInputStream(pixels(time)))
        return buildList {
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val argb = image.getRGB(x, y)
                    if ((argb ushr 24) > 100 && (argb shr 16 and 0xff) > 200 && (argb and 0xffff) == 0) add(x to y)
                }
            }
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
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        test: (ImageComposeScene) -> Unit,
    ) {
        val scene = ImageComposeScene(240, 160, coroutineContext = coroutineContext) { MaterialTheme(content = content) }
        try {
            scene.render(0).close()
            test(scene)
        } finally {
            scene.close()
        }
    }
}
