# Profile Card Sizing Design

## Summary

Make the profile selection cards use a fixed taller size so kids-profile subtitle text does not stretch some cards taller than others, and make the `Add Profile` card match that same rectangle.

## Problem

`ProfileSelectionScreen.kt` currently lets card height be driven by content. Regular profiles render only the profile name, while kids profiles render an additional `Kids up to ...` subtitle. That makes kids cards taller than non-kids cards. The `AddProfileCard` also ends up visually reading as a shorter square-like card instead of matching the profile cards.

## Goal

Create a uniform grid where every card has the same width and taller rectangular height, regardless of whether the profile shows a kids subtitle.

## Non-Goals

- Do not change card width.
- Do not change profile copy or kids-label wording.
- Do not redesign the avatar treatment, button placement, or overall screen layout.
- Do not introduce a broad card component refactor unless the existing code forces it.

## Scope

Primary file in scope:

- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSelectionScreen.kt`

No other files should need logic changes for this slice.

## Chosen Approach

Use a fixed card height with reserved bottom text space.

This keeps the current `150.dp` width, preserves the existing two-card row layout, and solves both issues with the smallest local edit:

- give `ProfileAvatarCard` a fixed taller minimum or exact height
- give `AddProfileCard` that same height
- reserve space for the kids subtitle even when a profile is not a kids profile

This is preferred over a shared component extraction because the current duplication is small and the request is limited to layout sizing.

## Design

### 1. Card Frame

Both `ProfileAvatarCard` and `AddProfileCard` will keep their current width of `150.dp` and use the same taller fixed height. The shape, surface fill, border, and press animation stay unchanged.

The fixed height should be large enough to fit:

- the current avatar block
- the profile name line
- the kids subtitle line
- the existing vertical spacing between those elements

### 2. Profile Text Area

`ProfileAvatarCard` should no longer let its total height depend on whether `profile.isKids` is true.

Implementation direction:

- keep the name as a single-line ellipsized title
- reserve a second-line area under the name for the kids subtitle
- when the profile is not a kids profile, keep that reserved area empty rather than collapsing it

This keeps the visual baseline of names and subtitles aligned across cards.

### 3. Add Profile Card

`AddProfileCard` should adopt the same fixed card height as the regular profile cards.

The avatar-plus icon circle can remain visually centered in the upper content area, with the `Add Profile` label below it. No copy or icon changes are needed.

### 4. Layout Impact

The card grid logic should remain unchanged:

- tablet layout still uses the horizontal scrolling row
- phone layout still uses the two-column grid

Only card sizing changes. Spacing between cards stays the same unless a tiny adjustment is required to avoid clipping after the height increase.

## Error Handling

No new state or error handling is needed. This is a layout-only change.

## Testing Strategy

This is a Compose UI sizing adjustment localized to a single screen. No new automated test seam is justified unless an existing profile-screen layout test already exists, which it does not appear to.

Verification should focus on successful compilation and visual sanity of the screen layout.

## Verification

Required verification after implementation:

- `./gradlew :composeApp:assembleFullDebug`

Additional verification if the final edit touches shared contracts in a way that affects iOS compilation expectations:

- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`

Manual smoke expectation:

- all profile cards render at the same taller height
- kids labels do not increase card height
- the `Add Profile` card matches the profile-card rectangle
- the two-column mobile layout still fits without overlap or clipping

## Risks

- Increasing card height could make the profile section feel more vertically dense on smaller phones.
- If the reserved subtitle space is too small, localized kids labels could still appear cramped or ellipsized earlier than expected.

## Mitigations

- Keep width unchanged and increase only height.
- Use the smallest height increase that comfortably fits the existing subtitle.
- Preserve current `maxLines = 1` ellipsis behavior for text to avoid overflow.

## Acceptance Criteria

- Every profile card uses the same height whether or not it shows a kids label.
- The `Add Profile` card uses that same height.
- Card width remains unchanged.
- The screen still builds successfully.
