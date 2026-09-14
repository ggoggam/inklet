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
arm64 and Wasm/JS) from macOS, then creates `v<version>` and a GitHub release. Prerelease
versions produce prereleases on GitHub. Pushing a tag does not publish anything.
Only the library is published; the gallery and platform hosts are samples.

`0.1.0-LOCAL` means the initial release is still being prepared. The workflow skips
unchanged versions, `-LOCAL` and `-SNAPSHOT` versions, and rejects malformed versions
or versions with an existing tag. Once setup is complete, change it to `0.1.0`
(or a prerelease such as `0.1.0-rc.1`) through a pull request.

Before the first release, configure these secrets in the repository's
[`maven-central` environment](https://github.com/ggoggam/inklet/settings/environments).
The Central Portal account must have a verified namespace covering `dev.ggoggam.inklet`.
Vitre's token and signing key can be reused if that account has namespace access;
GitHub does not allow reading secret values back from another repository.

| Secret | Value |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Central Portal user-token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user-token password |
| `SIGNING_IN_MEMORY_KEY` | Complete ASCII-armored private GPG key, including newlines |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | Key passphrase; leave unset for an unprotected key |

The public signing key must be available to Central through a supported keyserver.
See the [publishing plugin's Central setup guide](https://vanniktech.github.io/gradle-maven-publish-plugin/central/)
for token and signing-key setup. Do not set an empty `signingInMemoryKeyId`; the
workflow uses the first key in the exported keyring, as Vitre does.

Central versions cannot be overwritten. If a workflow fails after uploading, check
the Central Portal deployment before attempting another publish. If Central already
published it, finish the missing tag/GitHub release against the original commit.

## Local publication

On macOS with Xcode and Android SDK 36:

```sh
mise run publish:local
```

This writes unsigned artifacts to `~/.m2/repository` using `VERSION_NAME`.
Consumers can add `mavenLocal()` and depend on
`dev.ggoggam.inklet:inklet:0.1.0-LOCAL`, or use `includeBuild("../inklet")`.
Signing is enabled by default for Central and explicitly disabled by this task.
`mise run publish` uploads and releases to Central and requires the same credentials
as CI; it is not a local validation command.

Tool locks belong to this repository. When updating tool pins, refresh both profiles:

```sh
mise lock -p linux-x64,macos-arm64
MISE_ENV=ci mise lock -p linux-x64,macos-arm64
```

Preserve the upstream Drawably MIT notice in `LICENSE` and attribution in the
README when redistributing the adapted renderer.
