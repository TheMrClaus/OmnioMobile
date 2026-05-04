# Phase 2B Player Polish Design

## Context

- Repo: `/home/marcelloc/git/OmnioMobile/.worktrees/omnio-redesign`
- Branch: `omnio-redesign`
- Phase 2A detail redesign is already complete and verified.
- Phase 2B player redesign is partially implemented in the current worktree.
- The remaining work is limited to visual polish only.

## Goal

Finish the Omnio player redesign with a minimal consistency pass that improves visual cohesion across the player chrome, overlays, and modal panels while preserving all existing playback behavior.

## Non-Goals

- No playback engine changes.
- No source, episode, or scrobbling logic changes.
- No interaction-model redesign.
- No major dependency additions.
- No broad refactor of `PlayerScreen.kt`.

## Scope

Primary files in scope:

- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerControls.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerOverlays.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerSourcesPanel.kt`
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerEpisodesPanel.kt`

Secondary file in scope only if strictly needed:

- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerLayout.kt`

Out of scope unless a UI contract forces it:

- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreen.kt`

## Constraints

- Preserve playback state.
- Preserve scrobbling.
- Preserve gestures.
- Preserve source switching.
- Preserve progress persistence.
- Preserve control auto-hide.
- Preserve lock mode.
- Preserve source panel information architecture.
- Preserve episode panel information architecture.
- Prefer presentation-only edits.
- Keep changes as small and local as possible.

## Chosen Approach

Use a minimal consistency pass rather than a broad harmonization pass.

This means:

- Keep the existing Phase 2B sizing seam in `PlayerLayoutMetrics`.
- Unify the visual language already started in the player controls and panels.
- Avoid introducing new abstractions unless they reduce duplication immediately in the touched files.
- Add tests only if a new helper or layout metric is introduced through a red-green cycle first.

## Design

### 1. Player Chrome

The top and bottom player chrome should read as one Omnio treatment rather than separate styled fragments.

Implementation direction:

- Keep the cleaner Omnio top bar already introduced.
- Tighten header spacing and metadata hierarchy only where current text density feels uneven.
- Keep the larger center play/pause affordance and symmetric seek buttons.
- Keep the thinner red scrubber.
- Make the bottom action row feel like a single compact control tray with consistent pill spacing and hit areas.

### 2. Surface Language

All player controls and panels should use the same surface vocabulary.

That vocabulary is:

- glass-like dark surface fills
- subtle hairline borders
- rounded corners from the same radius family
- subdued secondary text
- restrained use of the red accent for active or selected states only

The purpose is consistency, not novelty. If an element already matches this vocabulary closely enough, leave it alone.

### 3. Overlays

`PlayerOverlays.kt` should be polished to match the same Omnio mood without changing flow.

Implementation direction:

- Keep the stronger red/black backdrop wash direction.
- Tighten visual consistency across opening, pause, gesture, and error overlays.
- Prefer color and spacing adjustments over structural rewrites.
- Do not change what is shown, when it is shown, or how dismiss/back actions work.

### 4. Source And Episode Panels

The source and episode panels should feel like siblings.

Implementation direction:

- Keep the current panel IA intact.
- Align header spacing, filter chip treatment, row background intensity, selected-state treatment, and metadata tone.
- Preserve existing sub-view flow in the episode panel.
- Keep the current row information content and selection behavior unchanged.

### 5. Layout Metrics

`PlayerLayoutMetrics` remains the sizing seam for responsive control density.

Rules:

- Reuse the existing metrics unless a remaining inconsistency cannot be solved locally.
- If a new metric is required, introduce the smallest possible one.
- If a new metric is introduced, add a failing test first in `PlayerLayoutTest.kt`, verify the failure, then implement the metric.

## Error Handling

No new error states are introduced.

Existing error and loading UI should keep their current behavior. Any changes in these states must be limited to presentation polish such as spacing, emphasis, or surface treatment.

## Testing Strategy

- Do not expand test surface for purely decorative edits.
- Keep the existing `PlayerLayoutTest.kt` coverage for responsive metric behavior.
- If a new helper or metric is added, follow test-first development for that seam only.
- After implementation, run the existing shared verification commands for branch confidence.

## Verification

Required verification after the polish pass:

- `./gradlew :composeApp:testFullDebugUnitTest`
- `./gradlew --no-build-cache :composeApp:assembleFullDebug`
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`

Manual smoke expectations:

- player opens successfully
- pause and resume still work
- seek back and seek forward still work
- scrubber still updates and seeks correctly
- control auto-hide still works
- source panel opens and selects correctly
- episode panel opens and selects correctly
- lock mode still works

## Risks

- Small visual-only edits can still accidentally affect touch targets or layout density on compact screens.
- Panel consistency edits can accidentally change emphasis or readability if secondary text is dimmed too far.
- Overlay polish can accidentally reduce contrast if gradients are pushed too aggressively.

## Mitigations

- Keep edits localized to touched composables.
- Avoid behavioral rewiring.
- Prefer parameter and styling adjustments before structural changes.
- Re-run broad verification after the pass.

## Implementation Notes

- Reuse existing Omnio primitives before adding new ones.
- Keep `PlayerScreen.kt` untouched unless a presentation contract requires a minimal hook-up change.
- Update `NEXT_AGENT_HANDOFF.md` after implementation with exact stop-state and verification commands run.

## Commit Note

This spec is written to the requested location, but it is not being committed automatically because the current workspace instructions only allow creating commits when explicitly requested by the user.
