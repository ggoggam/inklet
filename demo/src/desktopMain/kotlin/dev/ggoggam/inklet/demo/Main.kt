@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package dev.ggoggam.inklet.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.ggoggam.inklet.InkletBadge
import dev.ggoggam.inklet.InkletButton
import dev.ggoggam.inklet.InkletCard
import dev.ggoggam.inklet.InkletCheckbox
import dev.ggoggam.inklet.InkletDecoration
import dev.ggoggam.inklet.InkletDivider
import dev.ggoggam.inklet.InkletRadioButton
import dev.ggoggam.inklet.InkletTextField
import dev.ggoggam.inklet.InkletTheme
import dev.ggoggam.inklet.InkletToggle
import dev.ggoggam.inklet.InkletVariant
import dev.ggoggam.inklet.inkletDecoration
import java.io.File

fun main(args: Array<String>) {
    if (args.firstOrNull() == "--snapshot") {
        val width = args.getOrNull(2)?.toInt() ?: 1120
        val scene =
            ImageComposeScene(width, args.getOrNull(3)?.toIntOrNull() ?: 1040) {
                Gallery(
                    static = true,
                    initialDark =
                        "--dark" in args,
                )
            }
        try {
            scene.render().close()
            scene.render().use { image ->
                image.encodeToData()!!.use { data -> File(args.getOrElse(1) { "inklet.png" }).writeBytes(data.bytes) }
            }
        } finally {
            scene.close()
        }
    } else {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Inklet · a little less perfect",
                state = rememberWindowState(width = 1120.dp, height = 960.dp),
            ) {
                Gallery()
            }
        }
    }
}

@Composable
private fun Gallery(
    static: Boolean = false,
    initialDark: Boolean = false,
) {
    var dark by remember { mutableStateOf(initialDark) }
    var motion by remember { mutableStateOf(!static) }
    val colors =
        if (dark) {
            darkColorScheme(
                primary = Color(0xFFEBAE91),
                onPrimary = Color(0xFF38271E),
                background = Color(0xFF242321),
                surface = Color(0xFF302E2B),
                onSurface = Color(0xFFF4EDE2),
                outline = Color(0xFFB7A995),
                outlineVariant = Color(0xFF8B7C6B),
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF994C32),
                onPrimary = Color.White,
                background = Color(0xFFFAF7EF),
                surface = Color(0xFFFFFDF8),
                onSurface = Color(0xFF35362F),
                onSurfaceVariant = Color(0xFF6D7064),
                outline = Color(0xFF7D806C),
                outlineVariant = Color(0xFFA5A18E),
            )
        }
    MaterialTheme(colorScheme = colors) {
        InkletTheme(reduceMotion = !motion) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Inklet", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = colors.onSurface)
                    InkletBadge("a shared little life", color = colors.primary, scribble = true, seed = 4)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Good things,\none scribble at a time.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 38.sp,
                        lineHeight = 44.sp,
                        color = colors.onSurface,
                    )
                    Text("Our plans. Our someday list. The small things worth keeping.", color = colors.onSurfaceVariant)
                }
                InkletDivider(seed = 3)
                BoxWithConstraints {
                    if (maxWidth < 740.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            OurList(Modifier.fillMaxWidth())
                            PenTray(Modifier.fillMaxWidth(), motion, { motion = it }, dark, { dark = it })
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            OurList(Modifier.weight(1.2f))
                            PenTray(Modifier.weight(1f), motion, { motion = it }, dark, { dark = it })
                        }
                    }
                }
                Text(
                    "Made for our beautifully unfinished plans.",
                    color = colors.onSurfaceVariant,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                )
            }
        }
    }
}

private data class Wish(
    val id: Int,
    val title: String,
    val note: String,
    val done: Boolean = false,
)

@Composable
private fun OurList(modifier: Modifier) {
    var wishes by remember {
        mutableStateOf(
            listOf(
                Wish(1, "Find a new favourite café", "Somewhere with a sunny window"),
                Wish(2, "A slow Sunday by the river", "Pack a blanket and something sweet"),
                Wish(3, "Make pasta from scratch", "Flour everywhere. Worth it.", true),
            ),
        )
    }
    var draft by remember { mutableStateOf("") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Our someday list",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.inkletDecoration(InkletDecoration.Underline, seed = 5).padding(bottom = 7.dp),
            )
            InkletBadge("${wishes.count { it.done }} / ${wishes.size}", seed = 8)
        }
        wishes.forEach { wish ->
            key(wish.id) {
                InkletCard(Modifier.fillMaxWidth(), seed = wish.id) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        InkletCheckbox(
                            wish.done,
                            { checked ->
                                wishes =
                                    wishes.map {
                                        if (it.id ==
                                            wish.id
                                        ) {
                                            it.copy(done = checked)
                                        } else {
                                            it
                                        }
                                    }
                            },
                            Modifier.semantics { contentDescription = wish.title },
                            seed = wish.id,
                        )
                        Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(wish.title, fontWeight = FontWeight.Medium)
                            Text(wish.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        InkletTextField(
            draft,
            { draft = it },
            Modifier.fillMaxWidth().semantics { contentDescription = "A new wish" },
            placeholder = { Text("What should we do together?") },
            seed = 20,
        )
        InkletButton(
            onClick = {
                wishes = wishes + Wish((wishes.maxOfOrNull { it.id } ?: 0) + 1, draft.trim(), "Added to our someday list")
                draft =
                    ""
            },
            enabled = draft.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            seed = 21,
        ) { Text("+  Add to our list") }
        InkletCard(Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), seed = 30) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InkletBadge("a note for us", seed = 31)
                Text(
                    "We don't have to do it all.\nJust a little, together.",
                    fontFamily = FontFamily.Serif,
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                )
            }
        }
    }
}

@Composable
private fun PenTray(
    modifier: Modifier,
    motion: Boolean,
    setMotion: (Boolean) -> Unit,
    dark: Boolean,
    setDark: (Boolean) -> Unit,
) {
    var selected by remember { mutableIntStateOf(0) }
    var saved by remember { mutableStateOf(false) }
    InkletCard(modifier, seed = 40) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("The pen tray", style = MaterialTheme.typography.titleLarge)
            Text("A few everyday things, with a human touch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            InkletButton(
                { saved = !saved },
                Modifier.fillMaxWidth(),
                seed = 41,
            ) { Text(if (saved) "Saved for us ✓" else "Save a little moment") }
            InkletButton({}, Modifier.fillMaxWidth(), variant = InkletVariant.Outline, seed = 42) { Text("Make a plan") }
            InkletButton(
                {},
                Modifier.fillMaxWidth(),
                variant = InkletVariant.Scribble,
                seed = 43,
            ) { Text("Leave room for a little magic") }
            InkletDivider(seed = 44)
            Text("Who is this plan for?", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Us", "Me").forEachIndexed { index, name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InkletRadioButton(
                            selected == index,
                            { selected = index },
                            Modifier.semantics { contentDescription = name },
                            seed =
                                index + 50,
                        )
                        Text(name)
                    }
                }
            }
            InkletDivider(seed = 45)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("A little motion")
                InkletToggle(motion, setMotion, Modifier.semantics { contentDescription = "A little motion" }, seed = 46)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Evening paper")
                InkletToggle(dark, setDark, Modifier.semantics { contentDescription = "Evening paper" }, seed = 47)
            }
            Text(
                "Imperfect lines. Perfectly us.",
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                modifier = Modifier.inkletDecoration(InkletDecoration.Highlight, seed = 48).padding(6.dp),
            )
        }
    }
}
