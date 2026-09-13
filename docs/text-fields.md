# Richer outlined text fields

## Pinned API audit

Audited against the [Material 3 1.9.0 source archive](https://repo.maven.apache.org/maven2/org/jetbrains/compose/material3/material3/1.9.0/material3-1.9.0-sources.jar),
especially `OutlinedTextField.kt`, `TextFieldDefaults.kt` and
`internal/TextFieldImpl.kt`. Inklet uses public APIs only.

| API | Public drawing contract | Inklet decision |
| --- | --- | --- |
| `OutlinedTextField` String and `TextFieldValue` overloads | Text/icon slots, colors and shape; no replaceable container slot | An outer modifier cannot follow the internal label cutout or exclude supporting text |
| `OutlinedTextFieldDefaults.DecorationBox` | `innerTextField`, content slots, colors, padding and `container`; automatically clips the floating-label cutout around the container | Use with the String `BasicTextField` overload; share value, visual transformation, enabled/single-line flags and interaction source |
| `OutlinedTextFieldDefaults.Container` | Native background and border drawing with state colors and thickness | Replace entirely with cached pen fill and outline; no transparent native border workaround needed |
| `OutlinedTextFieldDefaults.decorator` | State-based Foundation decorator with output transformation and label-position options | Available for a future state-based adapter; not exposed by Inklet yet |
| Filled `TextFieldDefaults.DecorationBox` / `decorator` | Container slot for filled text fields and their bottom indicator | Out of scope for this outlined adapter |

The decoration box retains Material label typography, focus/value transitions,
placeholder visibility, prefix/suffix behavior, icon layout, supporting-text layout
and RTL positioning. Foundation owns selection, cursor, keyboard/IME input,
scrolling, editing and disabled/read-only behavior. The adapter supplies native
text/cursor/selection colors, minimum dimensions, room above the label and error
semantics, which are responsibilities outside the decoration box.

## Drawing and behavior

The pen container occupies only the editable outline, excluding supporting text.
Its top stroke is centered at the container's top, where Material cuts the label
gap. A generic inset border would sit below that gap at high roughness. The
adapter reserves top space for both the minimized label and the pen excursion;
side/bottom strokes remain inset. Cached geometry uses logical dp and the shared
three-frame Inklet clock. Material applies its animated cutout around the container.
The core pen renderer additionally clips the outline around the label's measured
bounds: a rough stroke can extend below Material's cutout during a transition.
The supplemental clip follows public layout coordinates, includes 4dp horizontal
clearance and 1dp vertical clearance, and affects only the pen, preserving the fill
behind a moving label. No background-colored rectangle masks the label. Bounds
updates invalidate drawing without rebuilding the cached pen geometry.

Use normal Material `TextFieldColors`: disabled colors take precedence over error,
then focused/unfocused colors. Indicator colors paint the pen. Container colors
paint a pen-shaped fill, and Material colors all content slots. Pen color changes
are immediate; label/content transitions remain native. A supplied `textStyle.color`
overrides state-dependent input text colors, matching native outlined fields.

The label is part of merged field semantics; interactive trailing content remains
its own action. `isError` sets error semantics using `errorMessage`, whose default
is English “Invalid input”. Hosts should supply a localized validation message and
visible supporting text. A caller's earlier `Modifier.semantics { error(...) }`
can override the default. Slots receive no additional Inklet glyph treatment.

`InkletTheme(reduceMotion = true)` freezes pen boil and focus re-sketching. Native
label transitions and cursor behavior continue under platform animation settings,
as with Inklet's tab recipes. Reserving space avoids clipping at the gallery's
maximum roughness (3) and boil (1); unusually thick pens or restrictive host
constraints still need visual validation. Do not clip the field to a tight shape.

## Supported API and remaining work

The existing String API and its positional argument order remain source compatible.
It now exposes label, leading/trailing icons, prefix/suffix, supporting text,
read-only state, typography, line limits, native colors, keyboard options/actions,
visual transformation, interaction source, corner radius and content padding.
`singleLine` continues to default to `true`; multiline callers must set it to false.

`VisualTransformation` is supplied to both editing and decoration so the displayed
value drives placeholder/label behavior. Password visual transformation retains
Foundation password semantics; it is not a state-based secure text-field adapter.

There are no `TextFieldValue` or `TextFieldState` overloads yet, so callers cannot
hoist selection/composition through this API. `InputTransformation`,
`OutputTransformation`, state-based line limits/scroll state, secure text-field
APIs, alternate label positions, filled fields and arbitrary shapes are not covered.
This change does not claim binary compatibility with previously compiled clients.

## Implementation checklist review

| Checklist item | Result for text fields | Evidence |
| --- | --- | --- |
| Prefer native components plus a modifier for outer-only changes | Not applicable: the floating gap is internal, and supporting text must be outside the border | Pinned API audit above; an outer modifier cannot implement either correctly |
| Add an adapter for repeated state, color and interaction wiring | Complete | `InkletTextField` shares value/transformation, enabled/single-line state and interaction source between Foundation editing and the Material decoration box |
| Use component-specific drawing and public slots for internal visuals | Complete after review fix | `DecorationBox.container` hosts the pen; moving-label bounds supplement Material's gap. Pixel tests cover intermediate animation frames, not just endpoints |
| Keep text, icons and layout as Compose content | Complete | Material lays out and styles caller slots; Foundation handles editing/selection. The label wrapper measures bounds without drawing or changing glyphs |
| Verify native border suppression against the pinned API | Complete | The native container is replaced entirely. Transparent-pen pixel tests verify no native border in unfocused/focused/error/disabled states; fill tests verify state-color precedence |

The review found and fixed a transient overlap between the rough outline and the
moving label at maximum gallery roughness. It also moved the pen renderer into
`dev.ggoggam.inklet` with explicit colors and no Material imports, preserving the
documented boundary between core drawing and Material adapters. Keyboard traversal
now explicitly verifies that disabled fields are skipped and read-only fields
remain focusable.

These results cover the current String-based outlined adapter. The overload gaps
above and the public-release checks remain open: Android/iOS device keyboard,
selection-handle and accessibility-service review; long-list cache/clock profiling;
and public API compatibility/toolchain/publication work. Compilation and desktop
scene tests do not replace those release checks.

## Validation

`TextFieldTest` exercises pointer focus, a hoisted interaction source, semantic
editing, keyboard deletion, IME actions, read-only selection, disabled editing,
password/error semantics, state-color precedence and multiline viewport limits.
Additional review tests check keyboard traversal, native border suppression,
container-fill state colors and intermediate label animation frames in LTR/RTL at
normal/double text size with active boil and reduced pen motion.
Pixel/layout checks verify floating gaps and supporting-text exclusion in LTR/RTL
at normal/double text size, with normal/disabled platform animations, plus zero
roughness, active boil and settled reduced motion.

`TextFieldExamplesTest` exercises the gallery's clear action and validation recovery
and writes light/dark, LTR/RTL, normal/double-size previews at maximum gallery
roughness to `sample/build/reports/text-fields/`. Desktop images are reviewed as
part of this change. Android/iOS device keyboard, selection handles, accessibility
services and visual review remain public-release checks.
