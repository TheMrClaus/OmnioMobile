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
- Omnio kids-profile fields are now present in the shared profile contract:
  - `NuvioProfile` and `ProfilePushPayload` now carry `isKids` and `maxAgeRating`
  - kids max-age normalization is centralized in the profile model/repository layer
- Android profile UI now supports kids-mode management:
  - `ProfileEditScreen` can toggle kids mode and choose a max age rating
  - `ProfileSelectionScreen` now shows a kids badge/summary for those profiles
- Shared content filtering is now in place for profile-based kids browsing:
  - `ProfileContentFilter` was added as the central filter service
  - shared catalog fetches now filter by the active profile
  - details screens now block over-limit titles and trim related rails
  - TMDB collection/person/entity browse flows now fetch/cache preview age ratings and filter against the active profile
  - local library shelves and internal library view-all rows now filter against the active profile
  - Trakt library lists now hydrate/store age ratings and filter against the active profile
  - home continue-watching / next-up rows now resolve parent age ratings when metadata is available and filter against the active profile
  - profile switches now reset search state so prior results do not leak across profiles
- Metadata parsing is a bit more tolerant now:
  - shared preview/detail parsing accepts `ageRating`, `age_rating`, or `certification`
- Focused common tests were added for the current filtering slice:
  - `ProfileContentFilter` now has direct coverage for library-item threshold behavior and unknown-rating pass-through
  - `HomeScreenTest` now covers kids filtering for continue-watching rows
  - `TraktLibraryRepositoryTest` now covers age-rating hydration requirements
- Local/CI runtime config bootstrap is now wired for real backend auth:
  - local Android builds use gitignored `local.properties` with populated Supabase and Trakt credentials
  - `.github/workflows/android-assemble-full-debug.yml` writes `local.properties` from repo secrets and uploads the generated APK
  - the fork repo now has `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `TRAKT_CLIENT_ID`, `TRAKT_CLIENT_SECRET`, and the currently available signing password/alias secrets configured
- A first external test artifact now exists:
  - commit `2e26d8d` was pushed to `origin/main`
  - GitHub prerelease `v0.1.0-beta.20260501-1` was published with the debug APK attached

## Validation

- Verified with: `./gradlew :composeApp:testFullDebugUnitTest --tests 'com.nuvio.app.features.profiles.ProfileContentFilterTest' --tests 'com.nuvio.app.features.trakt.TraktLibraryRepositoryTest' --tests 'com.nuvio.app.features.library.LibraryRepositoryTest' --tests 'com.nuvio.app.features.home.HomeScreenTest'`
- Verified with: `./gradlew :composeApp:assembleFullDebug`
- Verified with: `if grep -q 'const val CLIENT_ID = ""' composeApp/build/generated/runtime-config/kotlin/com/nuvio/app/features/trakt/TraktConfig.kt; then echo 'Trakt client ID is still blank' >&2; exit 1; fi && if grep -q 'const val CLIENT_SECRET = ""' composeApp/build/generated/runtime-config/kotlin/com/nuvio/app/features/trakt/TraktConfig.kt; then echo 'Trakt client secret is still blank' >&2; exit 1; fi && echo 'Generated Trakt config is non-empty'`
- Latest validation status before handoff: passing

## Important Scope Decisions

- iOS identity and bundle cleanup was intentionally deferred.
- Package namespaces are still `com.nuvio.app.*`; only product identity and selected behavior changed so far.
- This is still an Android-first migration slice.
- Kids filtering currently allows unrated/unknown titles through; no conservative block policy has been applied yet.
- Continue-watching filtering depends on resolving parent metadata at runtime; if metadata is unavailable, those items currently still fall through under the existing allow-unknown policy.
- `TRAKT_REDIRECT_URI` from OmnioTV was intentionally not copied into OmnioMobile; mobile still relies on the default `nuvio://auth/trakt` deep-link callback flow.
- The published beta asset is a debug sideload APK, not a signed release/store build.
- OmnioTV repo-secret parity is still incomplete; high-impact gaps include both keystore base64 secrets plus the various aiometadata/internal API URLs.

## Recommended Next Slice

1. Add a signed beta/release path instead of relying on debug prereleases:
  - port `OMNIO_PHONE_RELEASE_KEYSTORE_BASE64` into the fork secrets
  - decide whether beta delivery stays on GitHub prereleases or moves to a dedicated distribution channel
  - add a signed workflow once the keystore material exists
2. Triage and port only the remaining OmnioTV secrets that mobile actually needs:
  - keep `TRAKT_REDIRECT_URI` out unless the app callback flow changes away from `nuvio://auth/trakt`
  - bring over aiometadata/internal API URL secrets only for features that are confirmed wired in OmnioMobile
  - decide whether CI should keep writing plain `local.properties` or switch to a base64 bootstrap secret model
3. Finish first-run functionality on a clean install:
  - seed one or more default addons, or add a guided first-run addon import flow
  - verify profiles, addons, playback, and settings end-to-end without manual repository setup
4. Resume kids-filtering hardening after the bootstrap/distribution work:
  - decide the unknown/unrated policy
  - finish remaining saved/history/progress seams
  - add the remaining focused parser/filtering tests

## Guardrails

- Keep validating with `./gradlew :composeApp:assembleFullDebug` after each slice.
- Avoid broad namespace/package renames until the migration seams stabilize.
- Keep iOS changes minimal unless they block shared-code compilation.
- Treat OmnioTV as the behavior source of truth for auth/profile/kids semantics, not the current Nuvio contract.
- Keep `local.properties` out of git; use it only as the local secret source for runtime-config generation.
- Do not replace the mobile Trakt redirect with OmnioTV's OOB/web redirects unless callback handling is updated in-app first.
- Treat prerelease `v0.1.0-beta.20260501-1` as a debug-only tester artifact.

## Nearby Files

- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileModels.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileContentFilter.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileRepository.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileEditScreen.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSelectionScreen.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/catalog/CatalogData.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeScreen.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressModels.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryScreen.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryRepository.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktLibraryRepository.kt`
- `.github/workflows/android-assemble-full-debug.yml`
- `composeApp/build.gradle.kts`
- `iosApp/Configuration/Version.xcconfig`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/TmdbCollectionSourceResolver.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbMetadataService.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsScreen.kt`
