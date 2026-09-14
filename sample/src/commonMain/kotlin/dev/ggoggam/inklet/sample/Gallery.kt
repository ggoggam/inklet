package dev.ggoggam.inklet.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ggoggam.inklet.InkletDecoration
import dev.ggoggam.inklet.InkletStyle
import dev.ggoggam.inklet.InkletTheme
import dev.ggoggam.inklet.LocalInkletStyle
import dev.ggoggam.inklet.material3.InkletAssistChip
import dev.ggoggam.inklet.material3.InkletBadge
import dev.ggoggam.inklet.material3.InkletButton
import dev.ggoggam.inklet.material3.InkletCard
import dev.ggoggam.inklet.material3.InkletCheckbox
import dev.ggoggam.inklet.material3.InkletCircularProgressIndicator
import dev.ggoggam.inklet.material3.InkletDivider
import dev.ggoggam.inklet.material3.InkletFilterChip
import dev.ggoggam.inklet.material3.InkletIconButton
import dev.ggoggam.inklet.material3.InkletIconToggleButton
import dev.ggoggam.inklet.material3.InkletInputChip
import dev.ggoggam.inklet.material3.InkletLinearProgressIndicator
import dev.ggoggam.inklet.material3.InkletRadioButton
import dev.ggoggam.inklet.material3.InkletSlider
import dev.ggoggam.inklet.material3.InkletSuggestionChip
import dev.ggoggam.inklet.material3.InkletTabIndicator
import dev.ggoggam.inklet.material3.InkletTextField
import dev.ggoggam.inklet.material3.InkletToggle
import dev.ggoggam.inklet.material3.InkletVariant
import dev.ggoggam.inklet.material3.inkletDecoration
import dev.ggoggam.inklet.sample.icons.ArrowUpRight
import dev.ggoggam.inklet.sample.icons.Check
import dev.ggoggam.inklet.sample.icons.Heart
import dev.ggoggam.inklet.sample.icons.Lucide
import dev.ggoggam.inklet.sample.icons.Plus
import dev.ggoggam.inklet.sample.icons.X

@Composable
fun Gallery(
    static: Boolean = false,
    initialDark: Boolean = false,
    onDarkChanged: (Boolean) -> Unit = {},
) {
    var dark by remember { mutableStateOf(initialDark) }
    SideEffect { onDarkChanged(dark) }
    var motion by remember { mutableStateOf(!static) }
    var roughness by remember { mutableFloatStateOf(InkletStyle().roughness.toFloat()) }
    var boil by remember { mutableFloatStateOf(0.3f) }
    var notebook by remember { mutableIntStateOf(0) }
    var chapter by remember { mutableIntStateOf(0) }
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
        InkletTheme(style = InkletStyle(roughness = roughness.toDouble(), boil = boil.toDouble()), reduceMotion = !motion) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Inklet",
                        fontFamily = LocalGallerySerif.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = colors.onSurface,
                    )
                    InkletBadge("a shared little life", color = colors.primary, scribble = true, seed = 4)
                }
                PenSettings(
                    roughness = roughness,
                    setRoughness = { roughness = it },
                    boil = boil,
                    setBoil = { boil = it },
                    motion = motion,
                    setMotion = { motion = it },
                    static = static,
                    dark = dark,
                    setDark = { dark = it },
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Good things,\none scribble at a time.",
                        fontFamily = LocalGallerySerif.current,
                        fontSize = 38.sp,
                        lineHeight = 44.sp,
                        color = colors.onSurface,
                    )
                    Text("Our plans. Our someday list. The small things worth keeping.", color = colors.onSurfaceVariant)
                }
                InkletDivider(seed = 3)
                NotebookTabs(notebook, { notebook = it }, chapter, { chapter = it })
                BoxWithConstraints {
                    if (maxWidth < 740.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            OurList(Modifier.fillMaxWidth())
                            PenTray(Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            OurList(Modifier.weight(1.2f))
                            PenTray(Modifier.weight(1f))
                        }
                    }
                }
                LittleChoices()
                TextFieldExamples()
                ContainerRecipes()
                Text(
                    "Made for our beautifully unfinished plans.",
                    color = colors.onSurfaceVariant,
                    fontFamily = LocalGallerySerif.current,
                    fontSize = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun NotebookTabs(
    notebook: Int,
    onNotebookChange: (Int) -> Unit,
    chapter: Int,
    onChapterChange: (Int) -> Unit,
) {
    val pen = LocalInkletStyle.current
    val lineHeight = maxOf(8.0, 2 * (2.1 * pen.roughness + pen.boil) + pen.strokeWidth.value + 2).dp
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryTabRow(
            selectedTabIndex = notebook,
            containerColor = Color.Transparent,
            indicator = {
                InkletTabIndicator(Modifier.tabIndicatorOffset(notebook, matchContentSize = true).height(lineHeight), seed = 70)
            },
            divider = { InkletDivider(Modifier.height(lineHeight), seed = 71) },
        ) {
            listOf("Our plans", "Memories", "Someday").forEachIndexed { index, label ->
                Tab(
                    selected = notebook == index,
                    onClick = { onNotebookChange(index) },
                    enabled = index != 2,
                    unselectedContentColor =
                        if (index ==
                            2
                        ) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    text = { Text(label) },
                )
            }
        }
        val chapters =
            listOf(
                "Little adventures",
                "Slow weekends",
                "At our table",
                "Far from home",
                "Rainy days",
                "Just because",
                "With friends",
                "Traditions",
            )
        SecondaryScrollableTabRow(
            selectedTabIndex = chapter,
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            minTabWidth = 144.dp,
            indicator = {
                InkletTabIndicator(Modifier.tabIndicatorOffset(chapter, matchContentSize = false).height(lineHeight), seed = 72)
            },
            divider = { InkletDivider(Modifier.height(lineHeight), seed = 73) },
        ) {
            chapters.forEachIndexed { index, label ->
                Tab(selected = chapter == index, onClick = { onChapterChange(index) }, text = { Text(label) })
            }
        }
        Text(
            if (notebook == 0) "${chapters[chapter]} — a few things to look forward to." else "${chapters[chapter]} — the moments we keep.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        val completed = wishes.count { it.done }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InkletCircularProgressIndicator(
                progress = { completed.toFloat() / wishes.size },
                modifier = Modifier.semantics { contentDescription = "Wishes completed" },
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                seed = 22,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "$completed of ${wishes.size} little adventures",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                InkletLinearProgressIndicator(
                    progress = { completed.toFloat() / wishes.size },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Someday list progress" },
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    seed = 23,
                )
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
        ) {
            Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add to our list")
        }
        InkletCard(Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), seed = 30) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InkletBadge("a note for us", seed = 31)
                Text(
                    "We don't have to do it all.\nJust a little, together.",
                    fontFamily = LocalGallerySerif.current,
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                )
            }
        }
    }
}

@Composable
private fun PenTray(modifier: Modifier) {
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
            ) {
                Text(if (saved) "Saved for us" else "Save a little moment")
                if (saved) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Lucide.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            InkletButton({}, Modifier.fillMaxWidth(), variant = InkletVariant.Outline, seed = 42) { Text("Make a plan") }
            InkletButton(
                {},
                Modifier.fillMaxWidth(),
                variant = InkletVariant.Scribble,
                seed = 43,
            ) { Text("Leave room for a little magic") }
            InkletDivider(seed = 44)
            Text("Who is this plan for?", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InkletCircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = "Loading preview" },
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    seed = 63,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A moment in the making", style = MaterialTheme.typography.labelLarge)
                    InkletLinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Loading bar preview" },
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        seed = 64,
                    )
                }
            }
            Text(
                "Imperfect lines. Perfectly us.",
                fontFamily = LocalGallerySerif.current,
                fontSize = 18.sp,
                modifier = Modifier.inkletDecoration(InkletDecoration.Highlight, seed = 48).padding(6.dp),
            )
        }
    }
}

@Composable
private fun PenSettings(
    roughness: Float,
    setRoughness: (Float) -> Unit,
    boil: Float,
    setBoil: (Float) -> Unit,
    motion: Boolean,
    setMotion: (Boolean) -> Unit,
    static: Boolean,
    dark: Boolean,
    setDark: (Boolean) -> Unit,
) {
    InkletCard(Modifier.fillMaxWidth(), seed = 59) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 600.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PenSetting("Roughness", "Smooth to loosely sketched", roughness, 0f..3f, setRoughness, seed = 60)
                        PenSetting("Boil", "How much the ink moves", boil, 0f..1f, setBoil, seed = 61)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        PenSetting(
                            "Roughness",
                            "Smooth to loosely sketched",
                            roughness,
                            0f..3f,
                            setRoughness,
                            seed = 60,
                            modifier = Modifier.weight(1f),
                        )
                        PenSetting(
                            "Boil",
                            "How much the ink moves",
                            boil,
                            0f..1f,
                            setBoil,
                            seed = 61,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (!motion) {
                Text("Turn on motion to preview boil.", style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A little motion")
                    // Keep this setting's own feedback independent of the motion it controls.
                    InkletTheme(style = LocalInkletStyle.current, reduceMotion = static) {
                        InkletToggle(motion, setMotion, Modifier.semantics { contentDescription = "A little motion" }, seed = 46)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Evening paper")
                    InkletToggle(dark, setDark, Modifier.semantics { contentDescription = "Evening paper" }, seed = 47)
                }
                InkletButton(
                    {
                        setRoughness(InkletStyle().roughness.toFloat())
                        setBoil(0.3f)
                    },
                    variant = InkletVariant.Outline,
                    seed = 62,
                ) { Text("Reset pen settings") }
            }
        }
    }
}

@Composable
private fun PenSetting(
    label: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    seed: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            val tenths = kotlin.math.round(value * 10).toInt()
            Text("${tenths / 10}.${tenths % 10}", style = MaterialTheme.typography.labelLarge)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        InkletSlider(
            value,
            onValueChange,
            Modifier.fillMaxWidth().semantics { contentDescription = label },
            valueRange = range,
            colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            seed = seed,
        )
    }
}

@Composable
private fun LittleChoices() {
    var outdoors by remember { mutableStateOf(true) }
    var together by remember { mutableStateOf(true) }
    var favorite by remember { mutableStateOf(false) }
    var suggestion by remember { mutableStateOf("Try a picnic") }
    var message by remember { mutableStateOf("A few ways to make a plan our own.") }
    InkletCard(Modifier.fillMaxWidth(), seed = 60) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Little choices", style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InkletAssistChip(
                    onClick = { message = "Saturday afternoon is saved for us." },
                    label = { Text("Find a day") },
                    leadingIcon = { Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    seed = 61,
                )
                InkletSuggestionChip(
                    onClick = {
                        message = "$suggestion — added to our ideas."
                        suggestion = if (suggestion == "Try a picnic") "Visit a bookshop" else "Try a picnic"
                    },
                    label = { Text(suggestion) },
                    seed = 62,
                )
                InkletFilterChip(
                    selected = outdoors,
                    onClick = { outdoors = !outdoors },
                    label = { Text("Outdoors") },
                    leadingIcon = {
                        Icon(
                            if (outdoors) Lucide.Check else Lucide.Plus,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    seed = 63,
                )
                if (together) {
                    InkletInputChip(
                        selected = true,
                        onClick = { together = false },
                        label = { Text("The two of us") },
                        trailingIcon = { Icon(Lucide.X, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.semantics { contentDescription = "Remove the two of us" },
                        seed = 64,
                    )
                } else {
                    InkletAssistChip(onClick = { together = true }, label = { Text("Add us back") }, seed = 64)
                }
                InkletFilterChip(selected = true, onClick = {}, label = { Text("Someday") }, enabled = false, seed = 65)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                InkletIconButton(
                    onClick = { message = "One more little adventure." },
                    modifier = Modifier.semantics { contentDescription = "Add an adventure" },
                    variant = InkletVariant.Solid,
                    seed = 66,
                ) { Icon(Lucide.Plus, contentDescription = null) }
                InkletIconToggleButton(
                    checked = favorite,
                    onCheckedChange = { favorite = it },
                    modifier = Modifier.semantics { contentDescription = "Favorite this plan" },
                    variant = InkletVariant.Scribble,
                    seed = 67,
                ) { Icon(Lucide.Heart, contentDescription = null) }
                InkletIconButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.semantics { contentDescription = "Share plan, unavailable" },
                    seed = 68,
                ) { Icon(Lucide.ArrowUpRight, contentDescription = null) }
                Text(if (favorite) "A favourite plan" else "Keep it close", style = MaterialTheme.typography.labelLarge)
            }
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
