<div align="center">

  <h1>Omnio Mobile</h1>
  <br />
  <br />

  [![Contributors][contributors-shield]][contributors-url]
  [![Forks][forks-shield]][forks-url]
  [![Stargazers][stars-shield]][stars-url]
  [![Issues][issues-shield]][issues-url]
  [![License][license-shield]][license-url]

  <p>
    Omnio's multiplatform mobile client for Android and iOS.
    <br />
    Stremio addon ecosystem • Cross-platform
  </p>

</div>

## About

Omnio Mobile is a Kotlin Multiplatform mobile client for Android and iOS. It delivers a shared Compose UI while keeping the playback-focused experience, collection tools, watch progress flows, downloads, and Stremio addon ecosystem integration from the fork baseline.

The mobile app is built from a single shared codebase in [composeApp](./composeApp), with native platform entry points for Android and iOS.

## Installation

### Android

Download the latest Android build from [GitHub Releases](https://github.com/TheMrClaus/OmnioMobile/releases/latest).

### iOS

- [TestFlight](https://testflight.apple.com/join/u4y7MHK9)

## Development

```bash
git clone https://github.com/TheMrClaus/OmnioMobile.git
cd OmnioMobile
./scripts/run-mobile.sh android
# or
./scripts/run-mobile.sh ios
```

### Project Structure

- `composeApp/` contains the shared Kotlin Multiplatform and Compose Multiplatform app code.
- `composeApp/src/commonMain/` contains shared UI, features, repositories, and platform-agnostic logic.
- `composeApp/src/androidMain/` contains Android-specific integrations.
- `composeApp/src/iosMain/` contains iOS-specific integrations.
- `iosApp/` contains the native Xcode project and iOS entry point.

Useful commands:

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
./scripts/build-distribution.sh
```

Versioning is driven from `iosApp/Configuration/Version.xcconfig`, which is used as the shared source of truth for both iOS and Android builds.

## Legal & DMCA

Omnio Mobile functions solely as a client-side interface for browsing metadata and playing media provided by user-installed extensions and/or user-provided sources. It is intended for content the user owns or is otherwise authorized to access.

Omnio Mobile is not affiliated with any third-party extensions, catalogs, sources, or content providers. It does not host, store, or distribute any media content.

## Built With

- Kotlin Multiplatform
- Compose Multiplatform
- Kotlin
- AndroidX Media3
- AVFoundation and native iOS integrations

## Star History

<a href="https://www.star-history.com/#TheMrClaus/OmnioMobile&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=TheMrClaus/OmnioMobile&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=TheMrClaus/OmnioMobile&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=TheMrClaus/OmnioMobile&type=date&legend=top-left" />
 </picture>
</a>

<!-- MARKDOWN LINKS & IMAGES -->
[contributors-shield]: https://img.shields.io/github/contributors/TheMrClaus/OmnioMobile.svg?style=for-the-badge
[contributors-url]: https://github.com/TheMrClaus/OmnioMobile/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/TheMrClaus/OmnioMobile.svg?style=for-the-badge
[forks-url]: https://github.com/TheMrClaus/OmnioMobile/network/members
[stars-shield]: https://img.shields.io/github/stars/TheMrClaus/OmnioMobile.svg?style=for-the-badge
[stars-url]: https://github.com/TheMrClaus/OmnioMobile/stargazers
[issues-shield]: https://img.shields.io/github/issues/TheMrClaus/OmnioMobile.svg?style=for-the-badge
[issues-url]: https://github.com/TheMrClaus/OmnioMobile/issues
[license-shield]: https://img.shields.io/github/license/TheMrClaus/OmnioMobile.svg?style=for-the-badge
[license-url]: https://github.com/TheMrClaus/OmnioMobile/blob/main/LICENSE