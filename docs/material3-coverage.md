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
| Tabs | `InkletTabIndicator` and `InkletDivider` slot recipes for native primary/secondary fixed and scrollable rows | Native labels, icons and ripple; no indicator-shape parity or tab-row adapter |
| Navigation, app bars | No dedicated adapter | Selected indicators, containers, drawers, rails, bars and scroll behavior |
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

### Tab slot audit (Material 3 1.9.0)

The pinned [Material sources](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/1.9.0/material3-1.9.0-sources.jar),
`commonMain/androidx/compose/material3/TabRow.kt` and `Tab.kt`, expose these contracts:

| Native API | Indicator and divider slots | Retained behavior |
| --- | --- | --- |
| `PrimaryTabRow`, `SecondaryTabRow` | `TabIndicatorScope.() -> Unit`, divider composable | Equal-width tab layout; `tabIndicatorOffset` tracks the selected tab, optionally matching content width |
| `PrimaryScrollableTabRow`, `SecondaryScrollableTabRow` | Same slots; native `scrollState`, `edgePadding`, `minTabWidth` | Variable-width tabs, RTL scrolling and automatic scroll-to-selection |
| Deprecated `TabRow`, `ScrollableTabRow` | Indicator receives `List<TabPosition>`; divider composable | Legacy positioning modifier; audited, but not the gallery recipe or tested coverage |
| `Tab`, `LeadingIconTab` | Native text/icon or content slots, `enabled`, colors, interaction source | Selection role/state, disabled input, focus, keyboard activation and ripple |

These current tab APIs are stable in the pinned source; the drawing primitive
exposes no experimental upstream types. Both default drawing slots are replaced,
so no native indicator or divider is drawn underneath. `InkletTabIndicator` and
the existing `InkletDivider` share an 8dp drawing height. The gallery increases
both heights with its live pen settings to leave room for roughness and boil.
The host supplies valid selection and associated content, and can customize
native tab colors and interactions. See the [copyable tab recipe](../README.md#tabs-and-selection-indicators).

Reduced motion freezes Inklet drawing; native indicator transitions and scrolling
retain platform motion-duration scale behavior. `reduceMotion` is not a global
Material animation override. The pen indicator can also be used outside tabs with
a bounded width and host-owned positioning and selection semantics.

Inventory against the pinned Material version, including experimental APIs, before
claiming complete coverage. Experimental families should remain opt-in and should
not force unstable upstream types into otherwise stable public signatures.

## Next work checklist

Work through these priorities in order. A component whose only visual change is
its outer border or fill can be covered by a documented modifier recipe without
a dedicated Inklet composable. Record the supported treatment and its limits in
the coverage table; a recipe does not imply parity with every Material variant.

### 1. Tabs and a reusable pen indicator

- [x] Audit fixed and scrollable tab APIs in the pinned Material version for
  indicator and divider slots.
- [x] Add a reusable sketched selection underline and divider, retaining Material
  tab layout, selection semantics, focus, keyboard input and scrolling behavior.
- [x] Demonstrate fixed and scrollable tabs in the gallery, with hoisted selection.
- [x] Verify selection changes, disabled tabs, RTL, scrolling the selected tab into
  view, dark mode and reduced motion with appropriate behavior tests and visual review.

Validated with desktop Compose scene tests for all four current row variants,
including keyboard focus/activation, pointer selection, content/full-tab indicator
placement and scroll-to-selection in LTR/RTL with normal and disabled platform
animations. Light/dark LTR/RTL previews include the maximum gallery roughness;
review images are generated in `build/reports/tabs/`. Android/iOS device visual
review remains part of the public-release checklist below.

### 2. Container recipes in the gallery

- [ ] Audit cards, surfaces, app bars, menus, dialogs, sheets, tooltips and snackbars
  for treatments that only need an outer border or fill.
- [ ] Add gallery examples and copyable recipes using `inkletBorder` or
  `inkletSurface` wherever public APIs allow the intended treatment.
- [ ] Document native border suppression, container colors in every supported
  state, elevation, clipping order and padding needed to keep the pen visible.
- [ ] Apply popup decoration to the popup's own container and retain native
  dismissal, focus and positioning; record cases that need a dedicated adapter.
- [ ] Check representative recipes in light/dark themes and relevant interaction
  states, and update the coverage table to distinguish recipes from adapters.

### 3. Richer text fields

- [ ] Audit public text-field decoration/container APIs in the pinned version.
- [ ] Add floating labels with sketched outline gaps, leading/trailing icons,
  supporting text and error styling while retaining native editing behavior.
- [ ] Add gallery cases and verify focus, editing, disabled/read-only/error states,
  label transitions, RTL, large text and reduced motion.
- [ ] Document supported overloads and remaining transformation/state-based API gaps.

### Implementation checklist for each family

- [ ] If only the outer border or fill changes, use a native component with an
  Inklet modifier and document a recipe.
- [ ] Add a small convenience adapter when repeated state, color or interaction
  wiring makes the recipe cumbersome.
- [ ] Use component-specific drawing for internal indicators, tracks, checkmarks
  and outline gaps, preferring public drawing/content slots.
- [ ] Keep text, icons and layout-only components as existing Compose content.
- [ ] Verify the intended treatment against the pinned API: modifiers add drawing;
  they do not suppress native borders or automatically restyle descendants.

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
