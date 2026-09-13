# Inklet

A native Kotlin / Compose Multiplatform port of the hand-drawn renderer from
[Drawably](https://github.com/Danilaa1/drawably), for native apps. Targets Android,
iOS arm64, the iOS arm64 simulator, and desktop JVM. No React, WebView, SVG parser,
or downloaded font is needed at runtime.

![Native Compose gallery](docs/preview-light.png)

## Run

This is an independent Gradle project with its own wrapper and a desktop gallery.
Use JDK 17 or newer and an installed Android SDK (API 36). Set `ANDROID_HOME` or create a
local `local.properties` containing `sdk.dir=/path/to/android/sdk`.

```sh
./gradlew desktopTest
./gradlew :demo:run
./gradlew compileKotlinIosSimulatorArm64  # macOS with Xcode
```

The gallery supports adding wishes, checking them off, selecting a radio option,
saving a moment, changing between light and dark colors, and disabling motion.
Sample data lives only in memory. To export a deterministic native rendering:

```sh
./gradlew :demo:run --args='--snapshot /tmp/inklet.png 1120 1040'
./gradlew :demo:run --args='--snapshot /tmp/inklet-dark.png 1120 1040 --dark'
```

Versions match Daytwo: Kotlin 2.4.0-RC, Compose 1.11.0, Material 3 1.9.0, AGP 9.2.1,
Gradle 9.4.1. This project does not depend on Daytwo's source, resources, or backend.

## Use from another app

Add the standalone build in the consumer's `settings.gradle.kts`:

```kotlin
includeBuild("../inklet")
```

Add this to `commonMain.dependencies`:

```kotlin
implementation("dev.ggoggam.inklet:inklet:0.1.0")
```

Wrap the existing Material theme once:

```kotlin
MaterialTheme {
    InkletTheme(reduceMotion = false) {
        InkletButton(onClick = ::save) { Text("Save our plan") }
    }
}
```

The port inherits Material colors, content colors, and text styles. Give list items
stable seeds (`seed = item.id.hashCode()`) so recycling a row reproduces its drawing.
Without an explicit seed, a control keeps a random seed for its composition lifetime.

## Native API

| API | Purpose |
| --- | --- |
| `InkletButton` | Solid, outline, and scribble variants; native button behavior |
| `InkletCard`, `InkletBadge`, `InkletDivider` | Sketched surfaces and labels |
| `InkletCheckbox`, `InkletRadioButton`, `InkletToggle` | Native selection semantics with at least 48dp touch targets |
| `InkletTextField` | Native editable input; set `singleLine = false` for a textarea |
| `Modifier.inkletBorder()` | Add a pen outline to existing controls, including custom editors |
| `Modifier.inkletDecoration()` | Underline, highlight, or circle around a label/block |
| `InkletTheme`, `InkletStyle` | Shared animation clock, roughness, stroke width, and motion override |
| `Rough` | Lines, rounded rectangles, circles, ellipses, checkmarks, arrows, scribble fills, and boil variants |

Label standalone selection controls with a content description or a labelled parent.
Radio groups should use Compose's `selectableGroup()` on their parent. A card is a
non-interactive container; add native `clickable` behavior when appropriate.

This first native version covers the renderer and the controls used by Daytwo.
It does not reproduce the DOM attach/destroy API, browser select styling, every
upstream composite (tooltip, pager, tabs, etc.), or the optional Drawably Pen font.
Decorations surround one layout block; they do not detect individual lines in
wrapped text. Use native menus and compose additional patterns from these primitives.

## Rendering and motion

`Rough.kt` adapts upstream `src/rough.ts` and `src/prng.ts`: Mulberry32, 8-unit
sampling, two jittered passes, midpoint quadratic curves, and three subtle boil
frames. Geometry uses dp and converts to pixels once when the draw cache is built.
Only the selected frame changes during drawing; geometry is not regenerated on
each animation tick. The whole theme shares one clock, with three visible updates
per 1200ms. Interactive controls re-sketch on press, focus, and pointer entry.

Compose's animation duration scale controls the clock. `reduceMotion = true`
explicitly removes animation and interaction re-sketching. A control outside a
`InkletTheme` renders statically. Keep the host's existing platform accessibility
and lifecycle handling. Daytwo uses a gentler style (roughness 0.7, boil 0.2, 1dp ink)
and keeps its existing Korean typography and light/dark palettes.

## Development

Install [mise](https://mise.jdx.dev), then run from this directory:

```sh
mise trust
MISE_ENV=ci mise install  # includes the pinned JDK; local installs can use an existing JDK
mise run pre-commit
mise run test            # geometry + rendered-control tests and desktop gallery compilation
mise run demo
mise run ios:check       # macOS with Xcode; compiles both supported iOS targets
```

`mise run kt:fmt` formats Kotlin; `mise run lint` checks without changing files.
Install Git hooks with `mise exec -- prek install`. Each project owns its hook
configuration; the workspace uses prek's monorepo discovery.

The monorepo registers Inklet in its project catalog and mise config roots. Its
Linux CI runs pre-commit and desktop tests independently of Daytwo. The workflow
in `.github/workflows/ci.yml` becomes active when this directory is the root of a
separate repository; it needs no Daytwo files or repository secrets.

Daytwo consumes the sibling with `includeBuild("../inklet")`, with an optional
`-Pinklet.path=/path/to/inklet` override. Future standalone checkout and publication
instructions are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Validation

Tests cover JavaScript PRNG golden vectors (including unsigned overflow), seeded
reproduction, bounded boil frames, static motion, degenerate shapes, button and
selection accessibility actions, disabled actions, native text editing, and 48dp
checkbox bounds. Desktop scene tests render real Compose components through Skia.

## Attribution

Original Drawably by Daniel Belyi, [MIT licensed](LICENSE). The upstream copyright
and permission notice are preserved, including a copy in Daytwo's bundled Compose
resources (`files/drawably-LICENSE.txt`). This is an independent native port, not an
official upstream package. Upstream sources reviewed on 2026-09-13:
[rough.ts](https://github.com/Danilaa1/drawably/blob/main/src/rough.ts) and
[prng.ts](https://github.com/Danilaa1/drawably/blob/main/src/prng.ts).
