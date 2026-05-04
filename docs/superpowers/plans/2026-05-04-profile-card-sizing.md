# Profile Card Sizing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make all profile cards and the add-profile card use the same taller fixed height so kids subtitles do not stretch individual cards.

**Architecture:** Keep the change local to `ProfileSelectionScreen.kt` by introducing a shared card-height constant and reserving subtitle space inside `ProfileAvatarCard`. Reuse that same height in `AddProfileCard` so layout behavior stays consistent without restructuring the screen.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material 3

---

## File Map

- Modify: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSelectionScreen.kt`
  - Keep the existing screen layout intact.
  - Add a shared height constant for the profile cards.
  - Update `ProfileAvatarCard` to use a fixed height and reserved subtitle area.
  - Update `AddProfileCard` to use the same fixed height.
- Verify: Gradle build from repo root

### Task 1: Fix Profile Card Height Behavior

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSelectionScreen.kt`
- Verify: repo root Gradle build

- [ ] **Step 1: Read the existing card implementation and identify the sizing seam**

Inspect these sections in `ProfileSelectionScreen.kt`:

```kotlin
Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
        .width(150.dp)
        .graphicsLayer {
            alpha = animAlpha.value
            scaleX = animScale.value * pressScale
            scaleY = animScale.value * pressScale
            translationY = animOffset.value
        }
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
        .border(1.dp, omnioHairlineColor(), RoundedCornerShape(20.dp))
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
        .padding(horizontal = 8.dp, vertical = 10.dp),
)
```

And the kids subtitle block:

```kotlin
if (profile.isKids) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(
            Res.string.profile_kids_up_to,
            profile.effectiveMaxAgeRating() ?: DEFAULT_KIDS_MAX_AGE_RATING,
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
```

Expected finding: the card height is content-driven, so kids profiles become taller.

- [ ] **Step 2: Introduce shared card size constants**

Add small local constants near the other private composables in `ProfileSelectionScreen.kt`:

```kotlin
private val ProfileCardWidth = 150.dp
private val ProfileCardHeight = 196.dp
private val ProfileCardShape = RoundedCornerShape(20.dp)
```

Use the width constant anywhere the card width is currently hardcoded. Keep the height local to this file.

- [ ] **Step 3: Update `ProfileAvatarCard` to use fixed height**

Replace the card container modifier with the shared sizing constants:

```kotlin
modifier = Modifier
    .width(ProfileCardWidth)
    .height(ProfileCardHeight)
    .graphicsLayer {
        alpha = animAlpha.value
        scaleX = animScale.value * pressScale
        scaleY = animScale.value * pressScale
        translationY = animOffset.value
    }
    .clip(ProfileCardShape)
    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
    .border(1.dp, omnioHairlineColor(), ProfileCardShape)
    .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
    .padding(horizontal = 8.dp, vertical = 10.dp)
```

This ensures the card is always the same taller rectangle.

- [ ] **Step 4: Reserve subtitle space inside `ProfileAvatarCard`**

Replace the conditional kids subtitle block with a fixed-height text area:

```kotlin
Spacer(modifier = Modifier.height(4.dp))

Box(
    modifier = Modifier.height(24.dp),
    contentAlignment = Alignment.TopCenter,
) {
    if (profile.isKids) {
        Text(
            text = stringResource(
                Res.string.profile_kids_up_to,
                profile.effectiveMaxAgeRating() ?: DEFAULT_KIDS_MAX_AGE_RATING,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

This keeps a subtitle row present even when empty so card height no longer depends on the kids label.

- [ ] **Step 5: Update `AddProfileCard` to match the same rectangle**

Apply the same width, height, and shape constants to the add card:

```kotlin
modifier = Modifier
    .width(ProfileCardWidth)
    .height(ProfileCardHeight)
    .graphicsLayer {
        alpha = animAlpha.value
        scaleX = animScale.value * pressScale
        scaleY = animScale.value * pressScale
        translationY = animOffset.value
    }
    .clip(ProfileCardShape)
    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
    .border(1.dp, omnioHairlineColor(), ProfileCardShape)
    .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
    .padding(horizontal = 8.dp, vertical = 10.dp)
```

Keep the icon and label styling unchanged unless the fixed height makes a tiny alignment tweak necessary.

- [ ] **Step 6: Run the Android build verification**

Run:

```bash
./gradlew :composeApp:assembleFullDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Review the final diff for scope control**

Confirm the final changes are limited to:

```text
composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSelectionScreen.kt
docs/superpowers/specs/2026-05-04-profile-card-sizing-design.md
docs/superpowers/plans/2026-05-04-profile-card-sizing.md
```

Expected result: no unrelated behavior changes, no copy changes, and no broad refactor.
