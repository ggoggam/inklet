@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ggoggam.inklet.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.ggoggam.inklet.material3.inkletBorder
import dev.ggoggam.inklet.material3.inkletSurface
import kotlinx.coroutines.launch

/** Native containers decorated through their public APIs; see docs/container-recipes.md. */
@Composable
internal fun ContainerRecipes() {
    var kept by remember { mutableStateOf(false) }
    var visits by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("A little room for our weekend.") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("A place for our plans", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        RecipeAppBar()
        RecipeSurface(kept, { kept = it })
        RecipeCard("A Saturday at the market", true, { visits++ })
        RecipeCard("The winter picnic · coming later", false, {})
        Text(
            if (visits == 0) message else "The market plan has been opened $visits times.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                TextButton(onClick = { menu = true }) { Text("Plan options") }
                RecipeMenu(menu, { menu = false }, { message = "A copy is ready for us." })
            }
            TextButton(onClick = { dialog = true }) { Text("Make a promise") }
            TextButton(onClick = { sheet = true }) { Text("Packing list") }
            TextButton(onClick = {
                scope.launch {
                    val result = snackbar.showSnackbar("Our weekend is saved.", actionLabel = "Undo", withDismissAction = true)
                    message =
                        if (result == SnackbarResult.ActionPerformed) "Room for a different weekend." else "A weekend to look forward to."
                }
            }) { Text("Save weekend") }
        }
        SnackbarHost(snackbar) { RecipeSnackbar(it) }
    }
    if (dialog) {
        RecipeDialog({ dialog = false }, {
            message = "One slow morning, together."
            dialog = false
        })
    }
    if (sheet) RecipeSheet { sheet = false }
}

@Composable
internal fun RecipeCard(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier.fillMaxWidth().inkletSurface(
                containerColor = if (enabled) colors.surfaceContainerLow else colors.surfaceContainerHighest,
                ink = if (enabled) colors.outline else colors.outline.copy(alpha = 0.38f),
                cornerRadius = 16.dp,
                seed = 80,
            ),
        shape = RoundedCornerShape(16.dp),
        border = null,
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                contentColor = colors.onSurface,
                disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                draggedElevation = 0.dp,
                disabledElevation = 0.dp,
            ),
    ) {
        Text(label, Modifier.padding(24.dp))
    }
}

@Composable
internal fun RecipeSurface(
    selected: Boolean,
    onSelect: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = { onSelect(!selected) },
        modifier =
            Modifier.fillMaxWidth().inkletSurface(
                containerColor = if (selected) colors.secondaryContainer else colors.surface,
                ink = if (selected) colors.primary else colors.outline,
                cornerRadius = 16.dp,
                seed = 81,
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
        border = null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(if (selected) "Kept close to our hearts" else "Keep this close", Modifier.padding(24.dp))
    }
}

@Composable
internal fun RecipeAppBar(scrollBehavior: TopAppBarScrollBehavior? = null) {
    TopAppBar(
        title = { Text("Weekend notebook") },
        modifier = Modifier.inkletSurface(cornerRadius = 16.dp, seed = 82).padding(horizontal = 12.dp),
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        // This is an inset gallery bar. Real screen bars should retain their default window insets.
        windowInsets = WindowInsets(0, 0, 0, 0),
        scrollBehavior = scrollBehavior,
        actions = { RecipeTooltip() },
    )
}

@Composable
internal fun RecipeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        // This modifier reaches the popup's Column, INSIDE its Surface clip.
        modifier = Modifier.inkletSurface(cornerRadius = 16.dp, seed = 83).padding(horizontal = 8.dp),
        shape = RectangleShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
    ) {
        DropdownMenuItem(text = { Text("Copy our plan") }, onClick = {
            onCopy()
            onDismiss()
        }, contentPadding = PaddingValues(16.dp))
        DropdownMenuItem(text = { Text("Share · coming soon") }, onClick = {}, enabled = false, contentPadding = PaddingValues(16.dp))
    }
}

@Composable
internal fun RecipeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.inkletSurface(cornerRadius = 28.dp, seed = 84),
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("One small promise") },
        text = { Text("Leave a little time for a slow morning together.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Let's do it") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Maybe later") } },
    )
}

@Composable
internal fun RecipeSheet(onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        // The sheet modifier precedes its sliding offset. Decorate content inside it instead.
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .inkletSurface(cornerRadius = 16.dp, seed = 85)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("For a slow day outside", style = MaterialTheme.typography.titleLarge)
            Text("A blanket, something sweet, and nowhere else to be.")
            TextButton(onClick = {
                scope.launch {
                    state.hide()
                    if (!state.isVisible) onDismiss()
                }
            }) { Text("All packed") }
        }
    }
}

@Composable
internal fun RecipeTooltip() {
    val state = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        state = state,
        tooltip = {
            PlainTooltip(
                modifier =
                    Modifier
                        .inkletSurface(
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            ink = MaterialTheme.colorScheme.inverseOnSurface,
                            cornerRadius = 12.dp,
                            seed = 86,
                        ).padding(8.dp),
                caretShape = null,
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) { Text("Leave room for the unexpected.") }
        },
    ) {
        TextButton(onClick = { scope.launch { state.show() } }) { Text("A little tip") }
    }
}

@Composable
internal fun RecipeSnackbar(data: SnackbarData) {
    // Content overload: the data overload inserts 12dp INSIDE the supplied modifier.
    // Keep native inverse fill and fixed shadow; only replace the outer outline.
    Snackbar(
        modifier =
            Modifier.padding(4.dp).inkletBorder(
                color = MaterialTheme.colorScheme.inverseOnSurface,
                cornerRadius = 12.dp,
                seed = 87,
            ),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionContentColor = MaterialTheme.colorScheme.inversePrimary,
        dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface,
        action =
            data.visuals.actionLabel?.let { label ->
                { TextButton(onClick = data::performAction) { Text(label, color = MaterialTheme.colorScheme.inversePrimary) } }
            },
        dismissAction =
            if (data.visuals.withDismissAction) {
                {
                    TextButton(onClick = data::dismiss, modifier = Modifier.padding(end = 12.dp)) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                }
            } else {
                null
            },
    ) { Text(data.visuals.message) }
}
