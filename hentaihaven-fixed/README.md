# HentaiHaven for PipePipe

An 18+ PipePipe service plugin providing title search, series episode lists, and HLS playback.

## Current implementation

- Search uses the bridge's `/api/search` endpoint.
- Selecting a title loads `/api/anime/:slug` and its episode list.
- Playback resolves `/api/anime/:slug/episodes/:episode/stream` at play time.
- The returned HLS URL and its request headers are passed to PipePipe. Stream results are marked non-cacheable because the bridge documents playback references as temporary.
- The player extractor reports an age limit of 18.

## API dependency

The default API base is `https://haven.reyamika.eu.cc/api`. It is an independent third-party bridge, not an official HentaiHaven API or service. It can become unavailable, change its response schema, or stop resolving streams. Set another compatible API base under the plugin's **API base URL** setting. Do not enter credentials or private tokens into an untrusted bridge.

The bridge's documented playback contract returns `streams[].url`, `streams[].headers`, and optional `streams[].variants[]`. The plugin supports URL/header variants in that shape. It does not attempt to defeat logins, paywalls, age checks, geographic restrictions, or other access controls.

## Build requirements

- JDK 25
- Android SDK platform 36
- Android build-tools 37.0.0
- Network access to Maven Central, JitPack, Google Maven, and the Gradle distribution host

The included `gradlew` bootstrap downloads the official Gradle 9.5.1 distribution on first use. From this folder:

```sh
./gradlew :tools:genKey -PpluginSigningKey="$HOME/.pipepipe-hentaihaven-signing.key"
./gradlew :tools:index -PpluginSigningKey="$HOME/.pipepipe-hentaihaven-signing.key"
```

The `index` task builds the `.ppp` package, verifies it, and creates a signed `build/repo/index.json` with this plugin marked `nsfw: true`. Keep the private signing key backed up and out of Git. PipePipe pins the repository public key on first install, so every future update must use the same private key.

The root `Just-mods` workflow builds this project in GitHub Actions and uploads a short-lived CI artifact. **CI uses a disposable key only**. Its index is not a production repository index and must not be used for long-term plugin updates. Create and protect a permanent key before publishing.

## Repository integration

This project is intentionally isolated at `pipepipe-plugins/hentaihaven/`; it does not add a Gradle build to the `Just-mods` root. Its workflow lives at `.github/workflows/pipepipe-hentaihaven.yml`.

The generated index marks this plugin `nsfw: true`. NSFW is repository metadata, not a `plugin.json` field.

## Notes

This source package has not yet been compiled against a live PipePipe build in this environment. The included GitHub Actions workflow is intended to perform the official JDK/Android toolchain build and package validation. API availability and playback must also be tested on-device before calling this release-ready.
