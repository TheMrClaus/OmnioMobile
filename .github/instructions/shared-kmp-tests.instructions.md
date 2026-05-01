---
name: "Shared KMP Test Guidance"
description: "Use when changing shared Kotlin logic in composeApp/src/commonMain or adding tests in composeApp/src/commonTest. Covers test placement, package mirroring, and focused Gradle validation for shared KMP work."
applyTo:
  - "composeApp/src/commonMain/**/*.kt"
  - "composeApp/src/commonTest/**/*.kt"
---
# Shared KMP Test Guidance

- When a change modifies shared business logic under `composeApp/src/commonMain`, add or update a focused test in the matching package under `composeApp/src/commonTest`.
- Mirror the production package and keep test files behavior-scoped. Existing examples: [MetaDetailsParserTest](../../composeApp/src/commonTest/kotlin/com/nuvio/app/features/details/MetaDetailsParserTest.kt) and [TraktLibraryRepositoryTest](../../composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktLibraryRepositoryTest.kt).
- Prefer `kotlin.test` assertions and the existing lightweight test style over introducing heavier test frameworks or platform-specific helpers for shared logic.
- Cover the direct rule, parser branch, or repository decision that changed instead of relying on broad end-to-end tests.
- Validate the narrowest slice first when you know the touched test class: `./gradlew :composeApp:testFullDebugUnitTest --tests 'com.nuvio.app.features.<package>.<TestClass>'`
- Keep the repo baseline build after shared changes: `./gradlew :composeApp:assembleFullDebug`
- If the change can affect shared compilation contracts for iOS, also run `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
