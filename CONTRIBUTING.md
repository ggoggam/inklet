# Contributing to Inklet

Inklet is a standalone Kotlin Multiplatform build. It has its own Gradle wrapper,
tool pins, multiplatform sample gallery, tests, and pre-commit configuration.
No external application or running services are needed.

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
mise run dev:web
mise run web:test
mise run web:build
```

Tests exercise deterministic geometry and actual Compose scenes, including input,
selection, accessibility semantics, disabled controls, and the sample container
recipes (including popup actions and dismissal). Add coverage for changes
to those behaviors. `dev:android` builds, installs, and launches the sample gallery,
preferring a connected phone over an emulator and starting an AVD if needed.
Override the choice with `ANDROID_SERIAL`. `dev:ios` builds and launches the same
gallery on an iOS simulator on an Apple Silicon Mac. Override the simulator with
`INKLET_SIM` (exact name or UDID). Both hosts live under `sample/`, alongside the
shared gallery and desktop entry point.
`dev:web` serves the shared gallery in a browser. `web:test` runs shared geometry
tests in headless Chrome (install Chrome or set `CHROME_BIN`); `web:build` generates
the static site in `sample/build/dist/wasmJs/productionExecutable/`.
Gradle manages Node.js and Yarn. Commit `kotlin-js-store/wasm/yarn.lock` when web
dependencies change. CI tests the Wasm library and uploads the production site as
the `web-gallery` artifact for preview or hosting.
The [GitHub Pages workflow](.github/workflows/pages.yml) deploys the
[public gallery](https://blog.ggoggam.dev/inklet/) on pushes to `main` or a manual
workflow run. In repository Settings → Pages, the build source is **GitHub Actions**.
Use `android:compile` and `ios:compile` for library compilation without launching
an app. Run tests separately with `mise run test`. See [README.md](README.md#development)
for local tooling requirements.

## Releasing

Publishing follows [Vitre](https://github.com/ggoggam/vitre): changing `VERSION_NAME`
in [gradle.properties](gradle.properties) on `main` triggers
[release.yml](.github/workflows/release.yml). It runs the full CI workflow, publishes
all six publications (root metadata, Android, desktop, iOS arm64, iOS simulator
arm64 and Wasm/JS) from macOS with `mise run publish` to this repository's
[GitHub Packages](https://github.com/ggoggam/inklet/packages) registry, then creates
`v<version>` and a GitHub release. Stable versions and prereleases (`0.4.0-rc.1`) take the same
path; prereleases are marked as prereleases on GitHub. Versions up to 0.3.0 were published to
Maven Central and remain there; nothing is published to Central any more.
Pushing a tag does not publish anything.
Only the library is published; the gallery and platform hosts are samples.

The workflow skips unchanged versions, `-LOCAL` and `-SNAPSHOT` versions, and rejects
malformed versions or versions with an existing tag. Bump the version, stable or a prerelease
such as `0.4.0-rc.1`, through a pull request.

The workflow uploads with its own `GITHUB_TOKEN` (`packages: write`), so GitHub Packages needs no
secret. Artifacts are still signed, with these secrets in the repository's
[`maven-central` environment](https://github.com/ggoggam/inklet/settings/environments) (the name
dates from Maven Central):

| Secret | Value |
| --- | --- |
| `SIGNING_IN_MEMORY_KEY` | Complete ASCII-armored private GPG key, including newlines |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | Key passphrase; leave unset for an unprotected key |

Do not set an empty `signingInMemoryKeyId`; the workflow uses the first key in the exported
keyring, as Vitre does.

GitHub Packages uploads are not atomic, and a version cannot be overwritten: uploading a file
that already exists fails with HTTP 409. If the publish step fails partway, re-running it fails on
the files that did arrive. Either delete that version from every `dev.ggoggam.inklet` package under
[Packages](https://github.com/ggoggam/inklet/packages) (each publication is its own package) and
re-run the job, or bump `VERSION_NAME` to a new version. If the publish succeeded and only the tag
or GitHub release failed, create those by hand against the original commit instead of publishing
again.

## Local publication

On macOS with Xcode and Android SDK 36:

```sh
mise run publish:local
```

This writes unsigned artifacts to `~/.m2/repository` using `VERSION_NAME`.
Consumers can add `mavenLocal()` and depend on
`dev.ggoggam.inklet:inklet:0.1.0-LOCAL`, or use `includeBuild("../inklet")`.
Signing is enabled by default for releases and explicitly disabled by this task.
`mise run publish` uploads to GitHub Packages and requires the same credentials as CI;
it is not a local validation command.

Tool locks belong to this repository. When updating tool pins, refresh both profiles:

```sh
mise lock -p linux-x64,macos-arm64
MISE_ENV=ci mise lock -p linux-x64,macos-arm64
```

Preserve the upstream Drawably MIT notice in `LICENSE` and attribution in the
README when redistributing the adapted renderer.
