# Home Hero Brand Safe Area Design

## Goal

Prevent the `Omnio` brand label in the home hero from overlapping the phone status bar on mobile devices.

## Scope

- Adjust only the home hero top branding placement.
- Keep the existing Omnio redesign language and current hero hierarchy.
- Do not change hero content structure, chips, CTA layout, or tablet-specific composition unless required by the same safe-area behavior.

## Chosen Approach

Keep the `Omnio` label in the current top-left hero position, but add status-bar-safe top inset handling before rendering the `HeroTopBar` on mobile.

## Design

- The `HeroTopBar` remains the first element in the hero content stack.
- On mobile, the hero top area should reserve enough top padding to clear the device status bar / cutout area before drawing the brand label.
- The filter chips remain directly below the `Omnio` label with the same spacing and ordering.
- Tablet layout should remain visually unchanged unless the same unsafe overlap exists there.

## Implementation Notes

- Prefer a minimal Compose inset-based solution in `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomeHeroSection.kt`.
- Apply the inset at the container that positions `HeroTopBar`, rather than moving the brand text into a different hero region.
- Preserve the rest of the hero spacing as much as possible.

## Testing

- Verify on a mobile-sized layout that the `Omnio` label clears the status bar.
- Confirm the chips still sit directly beneath the label.
- Confirm tablet layout remains visually stable.

## Out Of Scope

- Reworking hero information architecture.
- Moving branding into the lower hero content block.
- Removing hero branding entirely.
