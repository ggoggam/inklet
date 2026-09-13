@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.ggoggam.inklet.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.InkletStyle
import dev.ggoggam.inklet.InkletTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextFieldExamplesTest {
    @Test
    fun galleryEditingClearValidationAndLightDarkRtlLargeTextPreviews() {
        for (dark in listOf(false, true)) {
            for (direction in LayoutDirection.entries) {
                for (large in listOf(false, true)) {
                    val scene =
                        ImageComposeScene(640, if (large) 1450 else 950) {
                            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides direction,
                                    LocalDensity provides Density(1f, if (large) 2f else 1f),
                                ) {
                                    InkletTheme(InkletStyle(roughness = 3.0, boil = 1.0), reduceMotion = true) {
                                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
                                            TextFieldExamples()
                                        }
                                    }
                                }
                            }
                        }
                    var time = 0L

                    fun advance() {
                        Snapshot.sendApplyNotifications()
                        repeat(20) {
                            time += 50_000_000
                            scene.render(time).close()
                        }
                    }
                    try {
                        advance()
                        val output =
                            File(
                                "build/reports/text-fields/${if (dark) "dark" else "light"}-${direction.name}-${if (large) "large" else "normal"}.png",
                            )
                        output.parentFile.mkdirs()
                        scene.render(time).use { image -> image.encodeToData()!!.use { output.writeBytes(it.bytes) } }
                        val adventure = scene.node("Our next little adventure")
                        adventure.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("A café"))
                        advance()
                        assertEquals("A café", scene.node("Our next little adventure").config[SemanticsProperties.EditableText].text)
                        scene
                            .nodes()
                            .single {
                                it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Clear adventure") ==
                                    true
                            }.config[SemanticsActions.OnClick]
                            .action!!
                            .invoke()
                        advance()
                        assertEquals("", scene.node("Our next little adventure").config[SemanticsProperties.EditableText].text)
                        assertTrue(scene.node("At our table").config.contains(SemanticsProperties.Error))
                        scene
                            .node("At our table")
                            .config[SemanticsActions.SetText]
                            .action!!
                            .invoke(AnnotatedString("2"))
                        advance()
                        assertFalse(scene.node("At our table").config.contains(SemanticsProperties.Error))
                    } finally {
                        scene.close()
                    }
                }
            }
        }
    }

    private fun ImageComposeScene.nodes(): List<SemanticsNode> {
        fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descendants)
        return semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
    }

    private fun ImageComposeScene.node(label: String) =
        nodes().single {
            it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true &&
                it.config.contains(SemanticsProperties.EditableText)
        }
}
