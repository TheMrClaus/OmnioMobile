# Next Agent Handoff

## Baseline

- Local repo path: `~/git/OmnioMobile`
- Fork origin: `https://github.com/TheMrClaus/OmnioMobile.git`
- Upstream source: `https://github.com/NuvioMedia/NuvioMobile.git`
- Upstream baseline commit cloned from `cmp-rewrite`: `8a58fabfddf7dc8f39db7072bb5271ce9e4d8526`

## What Landed

- Android-facing product identity was switched toward Omnio:
  - Android application ID is now `com.omnio.mobile`
  - Android app name is now `Omnio`
  - updater target now points at `TheMrClaus/OmnioMobile`
  - repo/docs references now point at `OmnioMobile`
- `composeApp` build config now tolerates a missing root `local.properties` file.
- Anonymous sign-in was removed from the auth flow:
  - `AuthRepository` no longer restores or creates anonymous sessions
  - `AuthScreen` no longer offers continue-without-account
- Profile contract work started:
  - `usesPrimaryPlugins` is now editable in the profile editor
  - profile 1 is sanitized centrally so it cannot inherit primary addons or plugins
  - existing downstream plugin inheritance logic was verified to already exist in `PluginRepository`

## Validation

- Verified with: `./gradlew :composeApp:assembleFullDebug`
- Latest validation status before handoff: passing

## Important Scope Decisions

- iOS identity and bundle cleanup was intentionally deferred.
- Package namespaces are still `com.nuvio.app.*`; only product identity and selected behavior changed so far.
- This is still an Android-first migration slice.

## Recommended Next Slice

1. Add Omnio kids-profile fields to the shared profile model and repository:
   - `isKids`
   - `maxAgeRating`
2. Extend the profile editor and selection UI just enough to manage those fields on Android.
3. Add one shared content-filter service for Android/mobile browsing surfaces.
4. Thread that filter into the first high-value surfaces:
   - home
   - details
   - search

## Guardrails

- Keep validating with `./gradlew :composeApp:assembleFullDebug` after each slice.
- Avoid broad namespace/package renames until the migration seams stabilize.
- Keep iOS changes minimal unless they block shared-code compilation.
- Treat OmnioTV as the behavior source of truth for auth/profile/kids semantics, not the current Nuvio contract.

## Nearby Files

- `composeApp/build.gradle.kts`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/core/auth/AuthRepository.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/auth/AuthScreen.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileModels.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileRepository.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileEditScreen.kt`
- `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/PluginRepository.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/AppUpdater.kt`
