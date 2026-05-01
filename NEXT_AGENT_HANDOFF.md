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
  - profile switches now reset search state so prior results do not leak across profiles
- Metadata parsing is a bit more tolerant now:
  - shared preview/detail parsing accepts `ageRating`, `age_rating`, or `certification`

## Validation

- Verified with: `./gradlew :composeApp:assembleFullDebug`
- Latest validation status before handoff: passing

## Important Scope Decisions

- iOS identity and bundle cleanup was intentionally deferred.
- Package namespaces are still `com.nuvio.app.*`; only product identity and selected behavior changed so far.
- This is still an Android-first migration slice.
- Kids filtering currently allows unrated/unknown titles through; no conservative block policy has been applied yet.
- The current slice focused on browsing surfaces fed by shared catalog/TMDB metadata paths, not saved/history/progress surfaces.

## Recommended Next Slice

1. Finish the remaining personalized/saved browsing seams that still likely bypass kids filtering:
  - local library shelves and section previews
  - Trakt library surfaces
  - home continue-watching / next-up cards
2. Decide and implement the product policy for unknown or unrated titles:
  - keep current allow-through behavior, or
  - switch to conservative blocking for kids profiles, or
  - make the policy explicit/configurable in one shared place
3. Add focused common tests before widening scope again:
  - `ProfileContentFilter` age-rating normalization and threshold behavior
  - parser coverage for `ageRating`, `age_rating`, and `certification`
  - TMDB preview age-rating plumbing for collection/person/entity paths
4. After the above is stable, reassess whether the next migration slice should be:
  - parental-control hardening, or
  - iOS/profile parity work only where shared-code compilation requires it

## Guardrails

- Keep validating with `./gradlew :composeApp:assembleFullDebug` after each slice.
- Avoid broad namespace/package renames until the migration seams stabilize.
- Keep iOS changes minimal unless they block shared-code compilation.
- Treat OmnioTV as the behavior source of truth for auth/profile/kids semantics, not the current Nuvio contract.

## Nearby Files

- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileModels.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileContentFilter.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileRepository.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileEditScreen.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSelectionScreen.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/catalog/CatalogData.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeScreen.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryScreen.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktLibraryRepository.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/TmdbCollectionSourceResolver.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbMetadataService.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsScreen.kt`
