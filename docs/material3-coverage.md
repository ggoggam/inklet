# Material 3 coverage and public-library design

Inklet should be a companion design system with explicit components and drawing
primitives. Full coverage is possible through component-specific work. A global
theme or a border modifier cannot replace internal checkmarks, tracks, indicators,
popup surfaces or text-field outlines on every Material component.

This follows Compose's [custom design system guidance](https://developer.android.com/develop/ui/compose/designsystems/custom):
extend Material's theme and wrap components while retaining their behavior where
possible. The implemented slider uses the thumb and track slots in the pinned
[Material 3 1.9.0 source](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/1.9.0/material3-1.9.0-sources.jar).
An upgrade should review those contracts before extending coverage claims. Progress
indicators do not expose track or arc drawing slots in that version; Inklet draws
cached pen paths with Foundation-compatible progress semantics and a separate
loading clock.
`InkletTheme(reduceMotion = true)` freezes loading; pen `boil = 0` and
`animate = false` leave loading active. See the [progress API](../README.md#progress-indicators).

## Structure

Keep three boundaries in the source now; split artifacts when independent consumers
justify the extra publishing and versioning cost:

1. Geometry: deterministic logical-coordinate strokes, seeded randomness and boil
   variants. No component state or Material styling belongs here.
2. Compose drawing: cached paths, fills, outlines, shared style and motion clock.
   `inkletBorder`, `inkletSurface` and `inkletDecoration` are the extension points.
3. Material adapters: native components with replaced drawing slots or transparent
   containers; Foundation interaction primitives only where no drawing slot exists.
   Expose native state, colors, interaction sources and content slots as adapters mature.

Geometry and Compose drawing live in `dev.ggoggam.inklet`, with explicit colors
on the core modifiers and no Material imports. Components and theme-default
modifier wrappers live under `dev.ggoggam.inklet.material3` in a matching nested
directory. Choose the modifier import according to the design system in use.

The current artifact includes all three and depends on Material 3. A future split
could offer `inklet-core`, `inklet-compose` and `inklet-material3`, with the existing
artifact kept as a compatibility facade. Avoid copying Material internals or
promising automatic restyling of arbitrary descendants.

## Coverage in this revision

“Adapter” means an Inklet API exists, not parity with every Material overload.

| Component family | Current support | Work required for full coverage |
| --- | --- | --- |
| Buttons | `InkletButton`, `InkletIconButton`, `InkletIconToggleButton`, solid/outline/scribble | Named tonal/text/elevated variants, FABs, segmented and split buttons |
| Cards, surfaces, badges, dividers | Basic containers and drawing modifiers | Native clickable/elevated variants, shapes, badge host and vertical divider |
| Checkbox, radio, switch | Boolean controls with Foundation semantics | Tri-state checkbox, nullable callbacks for labelled rows, native color/interaction configuration |
| Text fields | String-based outlined input | Labels with outline gaps, supporting text, leading/trailing icons, transformations and state-based APIs |
| Sliders | Continuous `InkletSlider` using Material slots | Discrete ticks, range and vertical variants |
| Chips | `InkletAssistChip`, `InkletSuggestionChip`, `InkletFilterChip`, `InkletInputChip`; native slots, selected/disabled colors and interactions | Elevated variants and arbitrary shapes; selected checkmarks/removal actions remain caller content |
| Navigation, tabs, app bars | No dedicated adapter | Selected indicators, containers, drawers, rails, bars and scroll behavior |
| Menus, dialogs, sheets, tooltips, snackbars | No dedicated adapter | Decorate each popup/container while retaining native dismissal, focus and positioning |
| Progress indicators | Linear/circular, determinate/indeterminate, progress semantics and RTL linear direction; loading clock independent of boil | Material track gaps, stop markers, exact loading choreography and expressive variants |
| Search, date/time pickers, carousels and other composites | No dedicated adapter | Audit public slots; compose supported subcomponents or provide documented native fallbacks |
| Text, icons and layout-only components | Use existing Compose content | Optional text decorations; no distortion of glyphs or layout |

Chips and icon buttons use transparent native containers and no native borders or
chip elevations. Their pen drawing covers a minimum 48dp layout (chips are taller
than Material's compact 32dp visual). Corner radii apply to chips; icon buttons use
a circular outline. Both accept hoisted interaction sources. Icon button glyphs,
chip icons and avatars remain native content. Selectable chips use
`InkletSelectableChipColors` because Material 3 1.9.0 keeps its selectable-chip
color fields private. Inklet's shared selectable palette uses surface-variant
content and secondary-container selected colors; customize it with `copy`.
See the [chip and icon-button API](../README.md#chips-and-icon-buttons).

Inventory against the pinned Material version, including experimental APIs, before
claiming complete coverage. Experimental families should remain opt-in and should
not force unstable upstream types into otherwise stable public signatures.

## Extend an existing container

For example, a host can decorate a Material `Surface` while keeping its content and
behavior. The rough fill is drawn behind the native component and the outline over it:

```kotlin
import dev.ggoggam.inklet.material3.inkletSurface

Surface(
    modifier = Modifier.inkletSurface(
        containerColor = MaterialTheme.colorScheme.surface,
        cornerRadius = 16.dp,
        seed = 42,
    ),
    color = Color.Transparent,
    shape = RoundedCornerShape(16.dp),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
) {
    Text("A small thought", Modifier.padding(20.dp))
}
```

Set native borders and container colors transparent in every supported state.
Place the drawing modifier outside native clipping; give content enough padding
for the pen. This technique decorates a container, not its internal indicators.

## Before a public stable release

- Define the promised Material version and per-component coverage. Stabilize the
  public API with API dumps/compatibility validation and explicit API declarations.
- Keep new adapters state-hoisted and support enabled/disabled, focused, pressed,
  hovered, selected and error states as appropriate. Preserve host typography and colors.
- Add gallery cases and behavior tests per family: keyboard and touch input, RTL,
  labelled semantics, 48dp targets, dark mode, large text, zero roughness, maximum
  preview settings and reduced motion. Visually review Android, iOS and desktop.
- Measure path-cache rebuilding and the shared clock on long lists. Sliders change
  the style live, so cache invalidation during a drag is intentional; idle drawing
  should only select precomputed frames.
- Choose a supported stable toolchain before 1.0; the current Kotlin version is an RC.
  Configure publication metadata, sources/docs, signing and a release workflow for
  the chosen repository, preserving Drawably's MIT attribution. `maven-publish`
  alone is not a configured public release.

Deliver coverage by family with these checks, rather than introducing dozens of
thin wrappers that only draw an extra border and imply full visual support.
