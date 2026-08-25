# Split button (M3)

> Source: https://m3.material.io/components/split-button/{overview,guidelines,specs,accessibility}

## Variants/types

- Single variant; added to the catalog in M3 Expressive (May 2025) — no baseline M3 version.
- Composition: a **leading button** (common button with icon and/or label) plus a **trailing button** (menu icon button).
- Five sizes matching buttons and icon buttons: XS, S (default), M, L, XL.
- Four color configurations: elevated, filled, tonal, outlined.

## Anatomy

- Leading button: icon and/or label text
- Trailing button: menu icon that rotates when selected

## Key dimensions

- Minimum touch target per half: **48×48dp**. Extra small and small variants are shorter than 48dp, so surrounding target areas pad out to ≥48dp tall.
- Menu gap: **4dp** from the split button.
- Menu alignment: trailing-button edge preferred; otherwise one of the button's side edges, adapting to breakpoint/scroll position.

## States

Enabled · Disabled · Hovered · Focused · Pressed (+ Selected on the trailing button). Colors and state layers are shared with buttons and icon buttons — see design.md for the 8/10/10% layer model.

- Unlike toggle buttons, selection applies only a state layer — the color scheme does not change.
- Inner corners of both halves morph on hover/focus/press.
- Selected trailing icon becomes centered.

## Attributes/behavior

- Purpose: pair a main action with a menu of related actions, hiding extra options to reduce visual complexity; works alone or beside buttons/button groups.
- Leading label stays brief (1–2 words) with a matching icon.
- Trailing icon always keeps its expand/collapse glyph; it rotates inward **180°** on open/close with a shape morph, using the **standard motion scheme** (not expressive).
- Opens a menu by default but can be customized to open other components (e.g., cards); avoid unusual menu modifications.
- Size may differ from neighboring controls — prominent controls can be larger; scaling up in small breakpoints adds hero emphasis.
- Layout mirrors fully in right-to-left languages.

## Token group

`md.comp.split-button.*` token sets organized by size; colors/state layers resolve through the `md.comp.button.*` and `md.comp.icon-button.*` modules.

## Accessibility summary

- Assistive-tech users must reach and operate each half, reach anything the trailing button opens, and know the current selection state.
- Initial focus lands on the leading button, then moves to the trailing button (subject to OS settings).
- Keyboard: Tab navigates between the two buttons; Space or Enter activates the focused one.
- Leading label follows common-button labeling; trailing button needs an expanded/collapsed state label indicating more options exist (e.g., "Watch later" pairs with "More watch options").
- The opened menu follows the standard menu accessibility guidance.

## Captured spec tables

| Variant | M3 | M3 Expressive |
|---|---|---|
| Split button | -- | Available |

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Size | XS, S, M, L, XL | -- | Available |
| Color | Elevated, filled, tonal, outlined | -- | Available |

Keyboard navigation:

| Keys | Actions |
|---|---|
| Tab | Navigate between buttons |
| Space or enter | Activate focused button |
