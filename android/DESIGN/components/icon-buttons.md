# Icon buttons (M3)

> Source: https://m3.material.io/components/icon-buttons/{overview,guidelines,specs,accessibility}

## Variants/types

- Two variants: **default** and **toggle** (selection); both available in M3 and M3 Expressive.
- Four color styles by emphasis: filled (default) > tonal > outlined > standard.
- Five sizes: extra small, small (default), medium, large, extra large (XS/M/L/XL added in M3 Expressive, May 2025).
- Two shapes: round (default), square; shape morphs when pressed and when selected.
- Three widths: narrow, default, wide (narrow/wide are Expressive additions).
- Formerly named "toggle buttons"; renamed with the default/toggle split.

## Anatomy

- Icon
- Container

## Key dimensions

- Sizes: XS **32dp** · S **40dp** (default) · M **56dp** · L **96dp** · XL **136dp**.
- Target size ≥ **48×48dp**, required for XS and S and whenever icon buttons are nested in other components; density must not shrink targets below 48×48 CSS px.
- Corner radius by size (dp): round = full at all sizes; square = 12/12/16/28/28 (XS→XL); pressed morph = 8/8/12/16/16.

## States

Enabled · Disabled · Hovered · Focused · Pressed (+ Selected for toggles). State layers per the shared model — hovered 8%, focused 10%, pressed 10%; disabled applies a 10% state layer over different base colors (see design.md).

- Standard style has an invisible container at rest; it becomes visible only when a state layer is applied.
- Pressed shape converges for round and square variants (values above).
- Selected toggle swaps resting shape round ↔ square by default (square rest → round selected).

## Attributes/behavior

- Must use system icons with clear meaning; ambiguous icons need a tooltip.
- Web hover shows a tooltip describing the action.
- Toggle pattern: outlined icon when unselected, filled icon when selected.
- In standard button groups, adjacent icon buttons respond to each other when pressed.
- Filled style = highest emphasis, use sparingly; tonal = secondary actions next to a high-emphasis action; outlined = medium emphasis; standard = low emphasis or colorful surfaces.
- Use size and width to build hierarchy; equal-importance actions should share one size.
- Do not apply density by default — it drops targets below the 48dp minimum.

## Token group

`md.comp.icon-button.*` families organized as common / color / size sets. The legacy filled/tonal/outlined icon-button token sets are deprecated in favor of the new sets; other tokens remain in the module.

## Accessibility summary

- Icon needs at least **3:1** contrast against surface/background.
- Keyboard: Tab focuses a non-disabled icon button; Space or Enter activates it.
- Accessibility label describes the executed action ("Add to favorites", "Send message"); tooltip on web mirrors this label.
- Minimum target **48dp** per button even when nested or densified; density controls must preserve 48×48 CSS px targets and stay opt-in.

## Captured spec tables

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Available |
| Implementation | Jetpack Compose | Available |
| Implementation | Jetpack Compose: Expressive | Available |
| Implementation | Android Views (MDC-Android) | Available |
| Implementation | Android Views (MDC-Android): Expressive | Available |
| Implementation | Web | Available |
| Implementation | Web: Expressive | Unavailable |

| Variant | M3 | M3 Expressive |
|---|---|---|
| Default | Available | Available |
| Toggle (selection) | Available | Available |

| Category | Options | M3 | M3 Expressive |
|---|---|---|---|
| Size | Small (default) | Available | Available |
| Size | XS, M, L, XL | -- | Available |
| Shape | Round (default) | Available | Available |
| Shape | Square | -- | Available |
| Color | Filled (default), tonal, outlined, standard | Available | Available |
| Width | Default | Available | Available |
| Width | Narrow, wide | -- | Available |

Color roles by style and selection state:

| | 1. Default | 2. Toggle unselected | 3. Toggle selected |
|---|---|---|---|
| Filled container / filled icon | Primary / On primary | Surface container / On surface variant | Primary / On primary |
| Tonal container / tonal icon | Secondary container / On secondary container | Secondary container / On secondary container | Secondary / On secondary |
| Outlined container / outlined icon | Outline variant (outline) / On surface variant | Outline variant (outline) / On surface variant | Inverse surface / Inverse on surface |
| Standard icon | On surface variant | On surface variant | Primary |

Corner sizes (dp):

| | XS | S | M | L | XL |
|---|---|---|---|---|---|
| A. Round button | Full | Full | Full | Full | Full |
| B. Square button | 12dp | 12dp | 16dp | 28dp | 28dp |
| C. Pressed state | 8dp | 8dp | 12dp | 16dp | 16dp |

Keyboard navigation:

| Keys | Actions |
|---|---|
| Tab | Focus lands on (non-disabled) icon button |
| Space or Enter | Activates the (non-disabled) icon button |
