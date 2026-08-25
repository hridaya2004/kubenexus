# Buttons (M3)

> Source: https://m3.material.io/components/buttons/{overview,guidelines,specs,accessibility}

## Variants/types

- Two variants: **default** and **toggle** (selection). Toggle is an M3 Expressive (May 2025) addition; default exists in both M3 and M3 Expressive.
- Five color configurations: elevated, filled (default), tonal, outlined, text. Toggle buttons do not offer the text style.
- Five sizes: extra small, small (default), medium, large, extra large — XS/M/L/XL are Expressive additions.
- Two shapes: round (default) and square (Expressive). Shape morphs when pressed and when selected.

## Anatomy

- Container
- Label text
- Icon (optional, leading side; leading in LTR, trailing in RTL)

## Key dimensions

- Default height **40dp** with fully rounded corners (M2: 36dp, small radius).
- Leading/trailing icon standard size: **20dp**.
- Small-button horizontal padding: **24dp** legacy value, now not recommended; **16dp** recommended to match new sizes.
- Corner radius by size (dp): round = full at XS–XL; square = 12/12/16/28/28; pressed morph = 8/8/12/16/16.
- Target area: extra small and small buttons require ≥ **48×48dp** targets.
- Container width hugs the label dynamically and may stretch responsively; never narrower than the label text.

## States

Enabled · Disabled · Hovered · Focused · Pressed (+ Selected for toggles). State layers follow the shared model — hover 8%, focus 10%, pressed 10% content-color overlay (see design.md).

- Elevated style rests at elevation 1; drops to elevation 0 when disabled.
- Outlined style has no fill; container is a stroke.
- Text style has an invisible container at rest; state layers still apply on hover/focus/press/disable.
- Pressed shape: round and square buttons converge on the same squarer pressed radius (values above).
- Selected toggle swaps resting shape round ↔ square (square rest → round when selected).

## Attributes/behavior

- Labels: sentence case, brief (1–3 words), single line, never wrapped or truncated; keep toggle label length similar across states.
- Toggle iconography: outlined icon unselected → filled icon selected; if no filled variant exists, increase weight.
- Elevated = tonal + shadow; use sparingly for separation from busy backgrounds. Filled = highest emphasis after the FAB, ideally one per page. Tonal = secondary palette mid emphasis. Outlined = medium emphasis, pairs with filled; avoid visually busy backgrounds. Text = lowest emphasis.
- Custom color roles are allowed if container/text keep a **3:1** contrast ratio (e.g., tertiary + on-tertiary).
- Placement: dialogs, modal windows, forms, cards, toolbars; inside standard button groups. Avoid overuse — move low-priority actions to chips, links, overflow menus, or icon buttons.

## Token group

`md.comp.button.*` families, organized as common / color / size token sets per style (elevated, filled, tonal, outlined, text); baseline sets organized by color.

## Accessibility summary

- Enabled buttons need **3:1** contrast with the background — measured on the container for elevated/filled/tonal, on label text for outlined/text.
- Keep labels concise enough to fit within two lines at **200%** text size (Android); if truncation is unavoidable, provide single-tap access to full content.
- Web: use a modified motion curve to dampen resonant overlap during rapid clicks.
- Keyboard: Tab navigates to a button; Space or Enter activates it.
- Accessibility label matches the visible label text; extra context allowed.

## Captured spec tables

| Variant | M3 | M3 Expressive |
|---|---|---|
| Default | Available | Available |
| Toggle (selection) | -- | Available |

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Size | Small (default) | Available | Available |
| Size | XS, M, L, XL | -- | Available |
| Shape | Round (default) | Available | Available |
| Shape | Square | -- | Available |
| Color | Elevated, filled (default), tonal, outlined, text | Available | Available |
| Small button padding | 24dp | Available | Not recommended. Use 16dp |
| Small button padding | 16dp | -- | Available |

Color roles by style and selection state:

| | 1. Default | 2. Toggle unselected | 3. Toggle selected |
|---|---|---|---|
| Elevated container / elevated icon & label | Surface container low / Primary | Surface container low / Primary | Primary / On primary |
| Filled container / filled icon & label | Primary / On primary | Surface container / On surface variant | Primary / On primary |
| Tonal container / tonal icon & label | Secondary container / On secondary container | Secondary container / On secondary container | Secondary / On secondary |
| Outlined container / outlined icon & label | Outline variant (outline) / On surface variant | Outline variant (outline) / On surface variant | Inverse surface / Inverse on surface |
| Text icon & label | Primary | -- | -- |

Corner sizes (dp):

| | XS | S | M | L | XL |
|---|---|---|---|---|---|
| A. Round button | Full | Full | Full | Full | Full |
| B. Square button | 12dp | 12dp | 16dp | 28dp | 28dp |
| C. Pressed state | 8dp | 8dp | 12dp | 16dp | 16dp |

Keyboard navigation:

| Keys | Actions |
|---|---|
| Tab | Navigate to a button |
| Space or Enter | Activate a button |
