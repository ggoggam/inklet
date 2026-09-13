# Contributing to Inklet

Inklet is a standalone Kotlin Multiplatform build. It has its own Gradle wrapper,
tool pins, multiplatform sample gallery, tests, and pre-commit configuration. No Daytwo source,
credentials, resources, or running services are needed.

Use JDK 17 or newer and Android SDK API 36. Set `ANDROID_HOME` or create an ignored
`local.properties` with `sdk.dir=/path/to/android/sdk`. For iOS compilation, use
macOS with Xcode compatible with the pinned Kotlin and Compose versions.

```sh
mise trust
MISE_ENV=ci mise install
mise exec -- prek install
mise run kt:fmt
mise run pre-commit
mise run test
mise run android:compile
mise run dev:android
mise run ios:compile
mise run dev:ios
mise run sample
```

Tests exercise deterministic geometry and actual Compose scenes, including input,
selection, accessibility semantics, and disabled controls. Add coverage for changes
to those behaviors. `dev:android` builds, installs, and launches the sample gallery,
preferring a connected phone over an emulator and starting an AVD if needed.
Override the choice with `ANDROID_SERIAL`. `dev:ios` builds and launches the same
gallery on an iOS simulator on an Apple Silicon Mac. Override the simulator with
`INKLET_SIM` (exact name or UDID). Both hosts live under `sample/`, alongside the
shared gallery and desktop entry point, following Basket's app-host pattern.
Use `android:compile` and `ios:compile` for library compilation without launching
an app. Run tests separately with `mise run test`. See [README.md](README.md#development)
for local tooling requirements.

## Moving to a public repository

The monorepo stores this project at `inklet/`. Its root CI catalog runs the same
tasks as the standalone workflow included here. To retain the project history,
split that directory from a committed workspace revision:

```sh
git subtree split --prefix=inklet -b inklet-public
```

Create the public repository separately and push that branch when ready. Its
included `.github/workflows/ci.yml` will run without workspace-specific secrets.
Generate standalone tool locks with `mise lock` and `MISE_ENV=ci mise lock` in the
new checkout; the monorepo currently supplies shared lockfiles at its root.

Daytwo can keep using a sibling checkout or a submodule with `includeBuild`, or use
`-Pinklet.path=/path/to/inklet` for another checkout location. Remove Inklet from
the workspace CI catalog and mise config roots if it becomes a submodule with its
own CI, following the other public projects in the workspace.

The build already applies `maven-publish`; `./gradlew publishToMavenLocal` can
produce local artifacts on a host that supports the requested targets. No remote
repository or release credentials are configured, and `dev.ggoggam.inklet:inklet`
has not been published. Before a remote release, configure the destination,
publication metadata, signing, and the release workflow for that destination.

Preserve the upstream Drawably MIT notice in `LICENSE` and attribution in the
README when redistributing the adapted renderer.
