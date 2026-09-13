# Material 3 container recipes

These recipes decorate native Material containers with `inkletSurface` (pen fill
and outline) or `inkletBorder` (outline over a native fill). They add no library
adapters. The interactive gallery's **A place for our plans** section runs the
[complete, copyable Kotlin recipes](../sample/src/commonMain/kotlin/dev/ggoggam/inklet/sample/ContainerRecipes.kt).
The functions there use ordinary Compose state and native callbacks; copy only
the ones you need. Imports for the drawing modifiers are:

```kotlin
import dev.ggoggam.inklet.material3.inkletBorder
import dev.ggoggam.inklet.material3.inkletSurface
```

## Audit of the pinned version

Audited against [Material 3 1.9.0 sources](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/1.9.0/material3-1.9.0-sources.jar),
including `Card.kt`, `Surface.kt`, `AppBar.kt`, `Menu.kt`, `AlertDialog.kt`,
`ModalBottomSheet.kt`, `BottomSheetScaffold.kt`, `Tooltip.kt`, `Snackbar.kt` and
`skikoMain` popup implementations. These findings apply to that version, with
default Material component implementations. Recheck them on upgrades or when
installing component overrides.

| Family / public API | Treatment and native drawing configuration | Scope / remaining work |
| --- | --- | --- |
| `Card`, clickable `Card` | `RecipeCard`: rough fill, transparent enabled **and disabled** native containers, `border = null`, all six elevation states zero | Native content colors, disabled semantics, input, focus and ripple remain. `ElevatedCard` accepts the same treatment but a flat recipe removes its distinguishing elevation. `OutlinedCard.border` is non-null: use `BorderStroke(0.dp, Color.Transparent)` to suppress its border. Those variants are audited, not gallery coverage. |
| `Surface` overloads | `RecipeSurface`: selectable surface, transparent native color, explicit selected/unselected pen and content colors, null border, zero tonal/shadow elevation | Clickable, selectable and toggleable surfaces expose the same drawing controls; the host owns enabled/selected/checked palettes. Only selectable and basic surfaces are demonstrated. |
| Small `TopAppBar` | `RecipeAppBar`: both normal and scrolled native container colors transparent; pen fill constant; native content colors and optional scroll behavior | Gallery uses an inset small bar. Centered/medium/large bars expose colors and a modifier; collapsed clipping, two-row layouts and inset behavior need their own visual checks. `BottomAppBar` allows transparent container and zero tonal elevation; audited, not demonstrated. |
| `DropdownMenu` | `RecipeMenu`: decorate the popup Column through its modifier; transparent native surface, null border, zero tonal/shadow elevation, rectangular parent clip | Native anchor positioning, scrolling, focusable popup, disabled items, keyboard and outside/back dismissal remain. This does not cover exposed text-field menus or arbitrary popup shapes. |
| `AlertDialog`, `BasicAlertDialog` | `RecipeDialog`: decorate the dialog's own root, transparent native container, zero tonal elevation; explicit title/text colors | Native dialog window, sizing, focus and dismissal remain. `BasicAlertDialog` permits a decorated `Surface` inside its content. No custom dialog window or scrim is needed. |
| `ModalBottomSheet` | `RecipeSheet`: native sheet plus a pen panel **inside** its content; keep native fill, handle, scrim, insets and motion | Its outer modifier precedes native sliding placement and transforms: pen drawing there can stay at the old position. Whole-sheet fill/outline replacement needs an adapter or a new upstream drawing slot. |
| `BottomSheetScaffold` | Decorate a panel inside `sheetContent`; preserve native sheet fill and drag handle | The scaffold modifier targets the whole scaffold; no sheet modifier slot is exposed. Whole-sheet replacement needs an adapter/slot, not a border on the scaffold. |
| `PlainTooltip`, `RichTooltip` | `RecipeTooltip`: decorate `PlainTooltip` inside `TooltipBox.tooltip`, transparent native container, explicit inverse content color, zero tonal/shadow elevation, no caret | Native positioning, hover/long press, focus and dismissal remain. Rich tooltip exposes equivalent container/color/elevation controls but is not demonstrated. Pen carets and arbitrary shapes need dedicated geometry. |
| `Snackbar` in `SnackbarHost` | `RecipeSnackbar`: content overload with pen border over native inverse fill and fixed native shadow; forward `SnackbarData` actions | Host still owns queue, timeout, live-region/dismiss semantics and transitions. Shadow elevation has no public parameter; a fully flat rough-fill snackbar needs an adapter. |

Top app bars, sheets and tooltips require `ExperimentalMaterial3Api` opt-in in
these sample functions. No experimental types are added to Inklet's public API.

## Drawing order, colors and space

For a replaced fill, make the native container transparent in **every supported
state**, and set its border to null (or transparent when non-null is required).
The Inklet modifier draws its fill behind native content and its outline over it.
It does not infer disabled or selected colors, turn off elevation, or recolor
children. Supply content colors explicitly: `contentColorFor(Color.Transparent)`
can inherit a color intended for a different background.

Use `Modifier.padding(outside).inkletSurface(...).padding(inside)` when a native
API accepts padding in its modifier. The first padding separates the container
from its surroundings; the second protects its content. For `Card`/`Surface`, put
content padding on the child. Put Inklet drawing before native clipping:
`inkletSurface(...).clip(shape)` when clipping content yourself. An ancestor clip
still clips everything, including the pen. Avoid `clip(shape).inkletSurface(...)`
when you expect pen strokes to remain outside that shape.

The renderer reserves an inset based on roughness, boil and stroke width. The
recipes allow 24dp content padding in cards, surfaces, dialogs and sheet panels;
menus use 8dp horizontal clearance plus 16dp item padding; tooltips add 8dp around
the native content container. App bars add 12dp horizontally. These choices were
reviewed at the gallery maximum (`roughness = 3`, `boil = 1`) with the default
stroke width. Larger pens need more space. Native ripple and content clipping
keep their smooth native shape; a scalar pen radius does not reproduce arbitrary
Material shapes or unequal corners.

Zero **all** clickable card elevations, including pressed, focused, hovered,
dragged and disabled. Zero `Surface` tonal and shadow elevation for a flat fill.
Native tonal elevation does not recolor the Inklet fill: choose the desired theme
color yourself, such as `surfaceContainerLow`. Border-only recipes deliberately
keep their native background and elevation; do not make those transparent.

## Cards and surfaces

`RecipeCard` includes the full enabled/disabled palette and elevation setup:

```kotlin
val colors = MaterialTheme.colorScheme
Card(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth().inkletSurface(
        containerColor = if (enabled) colors.surfaceContainerLow else colors.surfaceContainerHighest,
        ink = if (enabled) colors.outline else colors.outline.copy(alpha = 0.38f),
        cornerRadius = 16.dp,
        seed = 80,
    ),
    shape = RoundedCornerShape(16.dp),
    border = null,
    colors = CardDefaults.cardColors(
        containerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        contentColor = colors.onSurface,
        disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp, pressedElevation = 0.dp,
        focusedElevation = 0.dp, hoveredElevation = 0.dp,
        draggedElevation = 0.dp, disabledElevation = 0.dp,
    ),
) { Text(label, Modifier.padding(24.dp)) }
```

`RecipeSurface` uses native `Surface(selected = ..., onClick = ...)` for selection
semantics. It picks `secondaryContainer`/`onSecondaryContainer` when selected,
`surface`/`onSurface` otherwise, and keeps native `color = Color.Transparent`,
`border = null`, `tonalElevation = 0.dp`, `shadowElevation = 0.dp` in both states.
For a noninteractive surface, see the [basic surface recipe](material3-coverage.md#extend-an-existing-container).

## App bars

`RecipeAppBar` sets `TopAppBarDefaults.topAppBarColors(containerColor =
Color.Transparent, scrolledContainerColor = Color.Transparent)` and applies
`Modifier.inkletSurface(...).padding(horizontal = 12.dp)` to the native bar.
Setting only `containerColor` allows the native scrolled fill to reappear.

The gallery's bar is inside an already inset screen, so it uses zero window
insets. A screen-level bar should retain native window insets and connect its
`scrollBehavior.nestedScrollConnection` to the scrolling host. Keep the chosen
scroll behavior and state native. This recipe promises a constant pen fill on a
small bar; it does not replace collapse choreography or draw internal icons.

## Menus belong to the popup

Put the menu and its trigger in the same anchor `Box`; draw on `DropdownMenu`,
not on the trigger or the enclosing screen:

```kotlin
Box {
    TextButton(onClick = { expanded = true }) { Text("Plan options") }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.inkletSurface(cornerRadius = 16.dp, seed = 83)
            .padding(horizontal = 8.dp),
        shape = RectangleShape,
        containerColor = Color.Transparent,
        border = null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        DropdownMenuItem(
            text = { Text("Copy our plan") },
            onClick = { onCopy(); expanded = false },
            contentPadding = PaddingValues(16.dp),
        )
    }
}
```

In 1.9.0 the supplied modifier decorates a Column **inside** the popup's clipped
Surface, before its built-in vertical padding and scrolling. `RectangleShape`
prevents that parent from trimming the rough rounded corners. The viewport gets
the outline while items scroll inside it. Items retain native enabled/disabled
colors and ripple; the menu itself has one container state. Defaults retain the
native focusable popup and outside/back dismissal. Do not replace it with a
positioned in-screen Box just to draw a border.

## Dialogs, sheets and tooltips

`RecipeDialog` applies `inkletSurface(cornerRadius = 28.dp)` to `AlertDialog`'s
modifier, then sets a matching native shape, transparent container, zero tonal
elevation, and explicit title/text colors. Keep `onDismissRequest` and native
confirm/dismiss button slots. This modifier reaches the dialog's root inside its
own window; decorating the button that opens the dialog would have no effect.

`RecipeSheet` keeps the modal's native container and drag handle. Its content is:

```kotlin
Column(
    Modifier.fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        .inkletSurface(cornerRadius = 16.dp, seed = 85)
        .padding(24.dp),
) {
    Text("For a slow day outside")
    // Native content and actions here.
}
```

This is an inset panel, not whole-sheet coverage. It follows native sheet placement,
dragging and clipping because it lives inside the moving content. Leave native
window insets and the drag handle in place, including the handle's accessibility
actions. The gallery's close button calls `sheetState.hide()` in a coroutine and
removes the modal after it becomes hidden; scrim/back dismissal uses
`onDismissRequest`. The same panel approach works in persistent `sheetContent`.

`RecipeTooltip` uses the native `TooltipBox` with
`TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above)`.
Inside its `tooltip` slot, `PlainTooltip` receives the pen modifier, inverse fill
and outline colors, transparent native container, explicit inverse text color,
zero elevations and `caretShape = null`. Its additional 8dp padding leaves room
for the pen around Material's compact tooltip padding. Native hover and long
press still work; the gallery also calls `state.show()` from a button so touch
and keyboard users can open it explicitly. Decorate this popup content, not
`TooltipBox`'s anchor modifier. Persistent rich tooltips need a native action and
appropriate tooltip state; adding a pen caret requires dedicated drawing.

## Snackbars retain their host

`RecipeSnackbar` is used as `SnackbarHost(hostState) { RecipeSnackbar(it) }`.
Call `hostState.showSnackbar(...)` from a coroutine as usual and handle its
`SnackbarResult`. It uses the content overload of `Snackbar` and forwards:

```kotlin
// Inside Snackbar's native action/dismiss slots:
TextButton(onClick = data::performAction) { Text(data.visuals.actionLabel.orEmpty()) }
TextButton(onClick = data::dismiss) { Text("Dismiss") }
// In its content slot:
Text(data.visuals.message)
```

The complete function only supplies each action when requested by `data.visuals`.
It sets the pen to `inverseOnSurface`, retains the native `inverseSurface` fill,
and uses `inversePrimary` for the action. Its modifier is
`Modifier.padding(4.dp).inkletBorder(cornerRadius = 12.dp, ...)`; the native
shape is `RoundedCornerShape(12.dp)`. The shadow stays native because this overload
has no elevation parameter. `Snackbar(snackbarData, modifier = ...)` inserts an
extra 12dp padding **after** the supplied modifier, so it would outline a larger
area than its native surface. Forwarding actions through the content overload
avoids that mismatch while `SnackbarHost` preserves timing, queueing, transition,
live-region and accessibility-dismiss behavior. Adapt/localize the visible
“Dismiss” text in your app.

## Validation and limits

`ContainerRecipesTest` in the sample exercises the actual gallery recipes:
card pointer/keyboard input and disabled state, surface selection, normal/scrolled
app-bar rendering, menu positioning in LTR/RTL, popup actions and outside/back
dismissal, and snackbar undo/dismiss results. It renders light/dark previews at
maximum gallery roughness, including open popups and selected/disabled containers.
Images are written to `sample/build/reports/containers/`; run `mise run test`.

The scene harness runs on the desktop UI thread. It sends back navigation through
the native navigation dispatcher, matching the window's Escape translation.
Snapshots freeze Inklet motion and set native motion duration scale to zero for
repeatability. Inklet `reduceMotion` alone does not freeze native popup, ripple or
sheet transitions. Native animated transitions, Android/iOS device visuals,
platform back gestures and large-text layouts remain release checks; these
recipes do not claim every variant of a family.
