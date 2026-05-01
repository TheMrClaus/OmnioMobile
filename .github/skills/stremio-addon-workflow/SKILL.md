---
name: stremio-addon-workflow
description: 'Use when working on Stremio addon protocol integration, manifest parsing, deep links, addon testing, or addon-powered catalog and metadata flows in OmnioMobile.'
argument-hint: 'Describe the addon surface or protocol change'
---

# Stremio Addon Workflow

## When to Use

- Tasks touching addon manifests, protocol resources, deep links, parser behavior, or addon-driven catalog and stream flows.
- Validating OmnioMobile behavior against the Stremio addon contract before changing shared parsing or integration code.

## Procedure

1. Start with the source-of-truth docs instead of inferring the contract from current app code.
   - Overview and SDK shape: [Docs/Stremio addons refer/README.md](../../../Docs/Stremio%20addons%20refer/README.md)
   - Protocol details: [Docs/Stremio addons refer/protocol.md](../../../Docs/Stremio%20addons%20refer/protocol.md)
   - Advanced patterns: [Docs/Stremio addons refer/advanced.md](../../../Docs/Stremio%20addons%20refer/advanced.md)
   - Example flows: [Docs/Stremio addons refer/examples.md](../../../Docs/Stremio%20addons%20refer/examples.md)
   - Deep links: [Docs/Stremio addons refer/deep-links.md](../../../Docs/Stremio%20addons%20refer/deep-links.md)
   - Manual testing and installation: [Docs/Stremio addons refer/testing.md](../../../Docs/Stremio%20addons%20refer/testing.md)
2. Map the OmnioMobile integration surface before editing.
   - Shared app integration usually lives under `composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons`.
   - QuickJS or KSoup dependent addon execution belongs in `fullCommonMain`, `androidFull`, or `iosFull`; keep those dependencies out of `androidPlaystore` and `iosAppStore` source sets.
3. Keep protocol changes contract-first.
   - Resolve manifest, catalog, meta, stream, and subtitle field semantics against the Stremio docs before changing parsers.
   - If app behavior diverges from the docs, document the mismatch in the task output instead of silently normalizing it away.
4. Validate at the smallest useful scope.
   - Add or update `composeApp/src/commonTest` coverage for parser and repository changes.
   - Run a focused test command when possible, then the repo baseline build: `./gradlew :composeApp:assembleFullDebug`
5. Close the loop on migration-sensitive surfaces.
   - If addon work touches auth, profiles, identity, or kids filtering, refresh [NEXT_AGENT_HANDOFF.md](../../../NEXT_AGENT_HANDOFF.md) before considering the slice finished.
